package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class HasReservedTavernTable extends VillagerSensorBase {
  Interactable reservedTable;

  public HasReservedTavernTable(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, deltaTime, store)) {
      return false;
    }

    UUID villagerId = this.getVillagerId(ref, store);
    if (villagerId == null) {
      return false;
    }

    if (!this.triggered) {
      this.triggered = true;

      VillageHeartComponent heart = this.getVillageHeartComponent(ref, store);
      VillagerComponent villagerComponent = this.getVillagerComponent(ref, store);
      if (heart == null || villagerComponent == null) {
        return false;
      }

      Store<ChunkStore> chunkStore = heart.getVillageRef().getStore();
      for (UUID tavernId : heart.getVillageTavernWorkplaces()) {
        WorkplaceComponent workplace = heart.getWorkplaceFromId(tavernId);
        Ref<ChunkStore> workplaceRef = workplace.getWorkplaceRef().getRef();

        WorkplaceInteractablesMapComponent interactables = chunkStore.getComponent(
            workplaceRef,
            WorkplaceInteractablesMapComponent.getComponentType());
        this.reservedTable = interactables.getFirstAvailableInteractable(
            WorkplaceInteractibles.EATING_SPOT);

        if (this.reservedTable == null) {
          continue;
        }

        this.reservedTable.occupy(villagerId);
        villagerComponent.setReservedInteractable(this.reservedTable.getInteractablePosition());
        villagerComponent.setReservedTavernLocation(workplace.getWorkplaceRef().clone());
        store.putComponent(ref, VillagerComponent.getComponentType(), villagerComponent);
        break;
      }
    }

    return this.reservedTable != null;
  }

}
