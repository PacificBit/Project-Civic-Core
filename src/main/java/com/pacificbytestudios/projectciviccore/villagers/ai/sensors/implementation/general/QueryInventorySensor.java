package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderQueryInventorySensor;

public class QueryInventorySensor extends SensorBase {

  public QueryInventorySensor(BuilderQueryInventorySensor builderSensorBase) {
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
      double dt,
      Store<EntityStore> store) {
    NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());

    if (npcEntity == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[QueryInventorySensor] Could not fetch the npcEntity");
      return false;
    }

    ItemContainer container = npcEntity.getInventory().getStorage();
    int freeSlots = InventoryHelper.countFreeSlots(container);

    return freeSlots == container.getCapacity();
  }

}
