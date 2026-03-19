package com.pacificbytestudios.projectciviccore.registry.registry;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public final class VillageBuildingCostRegistry {
  private static final Map<StructureType, Set<MaterialQuantity>> requirementsByStructureType = new ConcurrentHashMap<>();

  private static boolean initialized = false;

  private VillageBuildingCostRegistry() {
  }

  public static void initialize() {
    if (initialized) {
      return;
    }
    initialized = true;

    registerRequirements(
        StructureType.HOUSE,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.TOWN_HALL,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.MINE,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.SAWMILL,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.FARM,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.STORAGE,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.TAVERN,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
  }

  public static Set<MaterialQuantity> getRequirementsForBuilding(StructureType structureType) {
    Set<MaterialQuantity> requirement = requirementsByStructureType.get(structureType);
    if (requirement == null || requirement.isEmpty()) {
      return new LinkedHashSet<>();
    }

    Set<MaterialQuantity> cloned = new LinkedHashSet<>();
    for (MaterialQuantity materialQuantity : requirement) {
      cloned.add(materialQuantity.clone(materialQuantity.getQuantity()));
    }

    return cloned;
  }

  private static void registerRequirements(
      StructureType structureType,
      MaterialQuantity... materialQuantity) {
    Set<MaterialQuantity> materials = new LinkedHashSet<>();
    for (MaterialQuantity quantity : materialQuantity) {
      materials.add(quantity);
    }
    requirementsByStructureType.put(structureType, materials);
  }
}
