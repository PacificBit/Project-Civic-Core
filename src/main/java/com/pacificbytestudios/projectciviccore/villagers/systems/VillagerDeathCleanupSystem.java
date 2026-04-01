package com.pacificbytestudios.projectciviccore.villagers.systems;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.AppartmentComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseAppartmentRegistry;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.ForceFillWorkplaceVacancyTask;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerHousingComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

public final class VillagerDeathCleanupSystem extends DeathSystems.OnDeathSystem {

  @Override
  public void onComponentAdded(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull DeathComponent component,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    VillagerComponent villagerComponent = commandBuffer.getComponent(ref, VillagerComponent.getComponentType());
    UUIDComponent uuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
    TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());

    if (villagerComponent == null || uuidComponent == null) {
      return;
    }

    UUID villagerId = uuidComponent.getUuid();
    ProjectCivicCore.LOGGER.atWarning().log(
        "[VillagerDeathCleanupSystem] Villager '" + villagerId + "' died!\n Death Reason: " +
            component.getDeathCause() + "\n Death Position: " +
            (transform == null ? "<TransformComponent not found>" : transform.getPosition()));

    this.cleanupPersistedWorkplace(villagerComponent, villagerId);
    this.cleanupPersistedHousing(villagerComponent, villagerId);

    Ref<ChunkStore> heartRef = this.resolveVillageHeartRef(villagerComponent);
    if (heartRef == null || !heartRef.isValid()) {
      ProjectCivicCore.LOGGER.atWarning().log(
          "[VillagerDeathCleanupSystem] Could not resolve village heart for dead villager " + villagerId);
      this.clearEntityLinks(villagerComponent);
      return;
    }

    VillageHeartComponent villageHeart = heartRef.getStore().getComponent(
        heartRef,
        VillageHeartComponent.getComponentType());
    if (villageHeart == null) {
      ProjectCivicCore.LOGGER.atWarning().log(
          "[VillagerDeathCleanupSystem] Could not find village heart component for dead villager " + villagerId);
      this.clearEntityLinks(villagerComponent);
      return;
    }

    villageHeart.removeVillagerLinks(villagerId);
    heartRef.getStore().putComponent(heartRef, VillageHeartComponent.getComponentType(), villageHeart);

    this.clearEntityLinks(villagerComponent);
  }

  private void cleanupPersistedWorkplace(VillagerComponent villagerComponent, UUID villagerId) {
    VillagerWorkerComponent workerData = villagerComponent.getWorkerData();
    if (workerData == null) {
      return;
    }

    WorkplaceComponent workplace = workerData.getWorkplace();
    if (workplace == null || workplace.getWorkplaceRef() == null) {
      return;
    }

    Ref<ChunkStore> workplaceRef = workplace.getWorkplaceRef().getRef();
    if (workplaceRef == null || !workplaceRef.isValid()) {
      return;
    }

    WorkplaceComponent persistedWorkplace = workplaceRef.getStore().getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());
    if (persistedWorkplace == null) {
      return;
    }

    persistedWorkplace.unregisterWorker(villagerId);
    if (villagerId.equals(persistedWorkplace.getBossId())) {
      persistedWorkplace.registerAsBoss(null);
    }

    if (workerData.getTypeOfWorker() == WorkType.MINER) {
      MineComponent mineComponent = workplaceRef.getStore().getComponent(
          workplaceRef,
          MineComponent.getComponentType());
      mineComponent.unregisterFromGallery(villagerId);
    } else if (workerData.getTypeOfWorker() == WorkType.MAYOR) {
      VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
      taskComponent.registerTask(new ForceFillWorkplaceVacancyTask(persistedWorkplace.getUuid()));
      Utils.safeVillageTaskEnqueue(persistedWorkplace.getVillageHeartRef(), taskComponent);
    }

    workplaceRef.getStore().putComponent(workplaceRef, WorkplaceComponent.getComponentType(), persistedWorkplace);
  }

  private void cleanupPersistedHousing(VillagerComponent villagerComponent, UUID villagerId) {
    VillagerHousingComponent housingData = villagerComponent.getHousingData();
    if (housingData == null) {
      return;
    }

    HouseComponent house = housingData.getHouse();
    if (house == null || house.getHouseRef() == null) {
      return;
    }

    Ref<ChunkStore> houseRef = house.getHouseRef().getRef();
    if (houseRef == null || !houseRef.isValid()) {
      return;
    }

    HouseComponent persistedHouse = houseRef.getStore().getComponent(houseRef, HouseComponent.getComponentType());
    if (persistedHouse == null) {
      return;
    }

    this.clearBedAssignment(persistedHouse, villagerId);
    houseRef.getStore().putComponent(houseRef, HouseComponent.getComponentType(), persistedHouse);
  }

  private void clearBedAssignment(HouseComponent houseComponent, UUID villagerId) {
    HouseAppartmentRegistry appartmentRegistry = houseComponent.getAppartmentRegistry();
    if (appartmentRegistry == null) {
      return;
    }

    for (AppartmentComponent appartment : appartmentRegistry.getAppartments().values()) {
      if (appartment == null) {
        continue;
      }

      for (VillageBedComponent bed : appartment.getBeds().values()) {
        if (bed == null || !villagerId.equals(bed.getOwner())) {
          continue;
        }

        bed.setOwner(null);
        bed.setTaken(false);
      }
    }
  }

  private void clearEntityLinks(VillagerComponent villagerComponent) {
    villagerComponent.setWorkerData(null);
    villagerComponent.setHousingData(null);
  }

  private Ref<ChunkStore> resolveVillageHeartRef(VillagerComponent villagerComponent) {
    try {
      return villagerComponent.getVillageHeartRef();
    } catch (NullPointerException ignored) {
      return null;
    }
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.and(
        VillagerComponent.getComponentType(),
        UUIDComponent.getComponentType(),
        NPCEntity.getComponentType());
  }
}
