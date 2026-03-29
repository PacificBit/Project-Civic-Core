package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderQueryDirtyInventorySensor;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryTransferValidators;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class QueryDirtyInventorySensor extends VillagerSensorBase {

  private final String[] excludeItemTypes;
  private final double minimumFullnessPercent;

  public QueryDirtyInventorySensor(
      BuilderQueryDirtyInventorySensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.excludeItemTypes = builderSensorBase.getExcludeItemTypes(support);
    this.minimumFullnessPercent = clampMinimumFullnessPercent(
        builderSensorBase.getMinimumFullnessPercent(support));
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
    ItemContainer container = this.getVillagerStorage(ref, store);
    if (container == null) {
      return false;
    }

    if (!meetsFullnessThreshold(container)) {
      return false;
    }

    if (this.excludeItemTypes == null || this.excludeItemTypes.length == 0) {
      int freeSlots = InventoryHelper.countFreeSlots(container);
      return freeSlots != container.getCapacity();
    }

    ToIntFunction<ItemStack> validator = buildDirtyInventoryValidator();
    return hasDirtyInventory(container, validator);
  }

  private double clampMinimumFullnessPercent(double configuredValue) {
    if (configuredValue < 0) {
      return -1;
    }

    return Math.min(configuredValue, 100.0);
  }

  private boolean meetsFullnessThreshold(ItemContainer container) {
    if (this.minimumFullnessPercent < 0) {
      return true;
    }

    int capacity = container.getCapacity();
    if (capacity <= 0) {
      return false;
    }

    int usedSlots = capacity - InventoryHelper.countFreeSlots(container);
    double fullnessPercent = (usedSlots * 100.0) / capacity;
    return fullnessPercent >= this.minimumFullnessPercent;
  }

  private ToIntFunction<ItemStack> buildDirtyInventoryValidator() {
    List<InventoryItemCriteria> criteria = Arrays.stream(this.excludeItemTypes)
        .filter(itemId -> itemId != null && !itemId.isBlank())
        .map(itemId -> new InventoryItemCriteria(itemId, false, 0))
        .toList();

    if (criteria.isEmpty()) {
      return InventoryTransferValidators.buildTransferValidator(null, ignored -> 0);
    }

    return InventoryTransferValidators.buildTransferValidator(criteria, ignored -> 0);
  }

  private boolean hasDirtyInventory(
      ItemContainer container,
      ToIntFunction<ItemStack> validator) {

    for (short slot = 0; slot < container.getCapacity(); slot++) {
      ItemStack item = container.getItemStack(slot);

      if (!InventoryTransferValidators.isTransferableItem(item)) {
        continue;
      }

      if (validator.applyAsInt(item) > 0) {
        return true;
      }
    }

    return false;
  }
}
