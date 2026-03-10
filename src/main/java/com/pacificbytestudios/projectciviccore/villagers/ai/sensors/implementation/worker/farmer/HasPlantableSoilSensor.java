package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.farmer;

import java.util.ArrayDeque;
import java.util.Map;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;

@Deprecated(forRemoval = true)
public class HasPlantableSoilSensor extends SensorBase {
  private static final String PLANTABLE_SOIL = "Soil_Dirt_Tilled";

  protected final PositionProvider positionProvider = new PositionProvider();

  public HasPlantableSoilSensor(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double dt,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, dt, store)) {
      this.positionProvider.clear();
      return false;
    }

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HasTillableSoilSensor] Could not fetch the villager component");
      this.positionProvider.clear();
      return false;
    }

    VillagerWorkerComponent workerData = villager.getWorkerData();
    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HasTillableSoilSensor] Could not fetch workerData");
      this.positionProvider.clear();
      return false;
    }

    Map<String, ArrayDeque<Vector3i>> blockPositions = workerData
        .getWorkareaSnapshot()
        .getBlockTypePosition();

    ArrayDeque<Vector3i> plantableSoil = blockPositions.get(PLANTABLE_SOIL);

    boolean hasPlantableSoil = plantableSoil != null && !plantableSoil.isEmpty();

    if (!hasPlantableSoil) {
      this.positionProvider.clear();
    }

    return hasPlantableSoil;
  }

}
