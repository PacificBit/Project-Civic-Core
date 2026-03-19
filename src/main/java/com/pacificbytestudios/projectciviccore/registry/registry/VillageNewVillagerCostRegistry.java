package com.pacificbytestudios.projectciviccore.registry.registry;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.server.core.inventory.MaterialQuantity;

public final class VillageNewVillagerCostRegistry {
  private static final Map<Short, Set<MaterialQuantity>> requirementsByCurrentLevel = new ConcurrentHashMap<>();

  private static boolean initialized = false;

  private VillageNewVillagerCostRegistry() {
  }

  public static void initialize() {
    if (initialized) {
      return;
    }
    initialized = true;

    registerRequirements(
        1,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 50, null));

    registerRequirements(
        2,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 150, null));

    registerRequirements(
        3,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 300, null));
  }

  public static Set<MaterialQuantity> getRequirementsForLevel(short level) {
    Set<MaterialQuantity> requirement = requirementsByCurrentLevel.get(level);
    if (requirement == null || requirement.isEmpty()) {
      return new LinkedHashSet<>();
    }

    Set<MaterialQuantity> cloned = new LinkedHashSet<>();
    for (MaterialQuantity materialQuantity : requirement) {
      cloned.add(materialQuantity.clone(materialQuantity.getQuantity()));
    }

    return cloned;
  }

  private static void registerRequirements(int level, MaterialQuantity... materialQuantity) {
    Set<MaterialQuantity> materials = new LinkedHashSet<>();
    for (MaterialQuantity quantity : materialQuantity) {
      materials.add(quantity);
    }
    requirementsByCurrentLevel.put((short) level, materials);
  }
}
