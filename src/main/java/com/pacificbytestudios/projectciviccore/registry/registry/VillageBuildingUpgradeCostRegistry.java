package com.pacificbytestudios.projectciviccore.registry.registry;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public final class VillageBuildingUpgradeCostRegistry {
  private static final Map<StructureType, Map<Short, Set<MaterialQuantity>>> requirementsByStructureType = new ConcurrentHashMap<>();

  private static boolean initialized = false;

  private VillageBuildingUpgradeCostRegistry() {
  }

  public static void initialize() {
    if (initialized) {
      return;
    }
    initialized = true;

    registerRequirements(
        StructureType.HOUSE,
        1,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
    registerRequirements(
        StructureType.HOUSE,
        2,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.TOWN_HALL,
        1,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
    registerRequirements(
        StructureType.TOWN_HALL,
        2,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.MINE,
        1,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
    registerRequirements(
        StructureType.MINE,
        2,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.SAWMILL,
        1,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
    registerRequirements(
        StructureType.SAWMILL,
        2,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.STORAGE,
        1,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
    registerRequirements(
        StructureType.STORAGE,
        2,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.TAVERN,
        1,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
    registerRequirements(
        StructureType.TAVERN,
        2,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));

    registerRequirements(
        StructureType.FARM,
        1,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
    registerRequirements(
        StructureType.FARM,
        2,
        new MaterialQuantity("Ingredient_Life_Essence*", null, null, 100, null));
  }

  public static Set<MaterialQuantity> getRequirementsForNextLevel(
      StructureType structureType,
      short currentLevel) {
    Map<Short, Set<MaterialQuantity>> requirementsByCurrentLevel = requirementsByStructureType.get(structureType);
    if (requirementsByCurrentLevel == null || requirementsByCurrentLevel.isEmpty()) {
      return new LinkedHashSet<>();
    }

    Set<MaterialQuantity> requirement = requirementsByCurrentLevel.get(currentLevel);
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
      int currentLevel,
      MaterialQuantity... materialQuantity) {
    Set<MaterialQuantity> materials = new LinkedHashSet<>();
    for (MaterialQuantity quantity : materialQuantity) {
      materials.add(quantity);
    }
    requirementsByStructureType
        .computeIfAbsent(structureType, ignored -> new ConcurrentHashMap<>())
        .put((short) currentLevel, materials);
  }
}
