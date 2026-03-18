package com.pacificbytestudios.projectciviccore.registry.registry;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;

public final class VillageLevelUpRegistry {
  private static final Map<Integer, Set<ItemRequestCriterion>> requirementsByCurrentLevel = new ConcurrentHashMap<>();

  private static boolean initialized = false;

  private VillageLevelUpRegistry() {
  }

  public static void initialize() {
    if (initialized) {
      return;
    }
    initialized = true;

    registerRequirements(
        2,
        new ItemRequestCriterion("Village_Food*", true, 30),
        new ItemRequestCriterion("Ingredient_Life_Essence*", true, 200));

    registerRequirements(
        3,
        new ItemRequestCriterion("Village_Food*", true, 30),
        new ItemRequestCriterion("Ingredient_Life_Essence*", true, 200));
  }

  public static Set<ItemRequestCriterion> getRequirementsForNextLevel(int currentLevel) {
    Set<ItemRequestCriterion> criteria = requirementsByCurrentLevel.get(currentLevel);
    if (criteria == null || criteria.isEmpty()) {
      return new LinkedHashSet<>();
    }

    Set<ItemRequestCriterion> cloned = new LinkedHashSet<>();
    for (ItemRequestCriterion criterion : criteria) {
      cloned.add(criterion.clone());
    }

    return cloned;
  }

  private static void registerRequirements(int currentLevel, ItemRequestCriterion... criteria) {
    Set<ItemRequestCriterion> levelCriteria = new LinkedHashSet<>();
    for (ItemRequestCriterion criterion : criteria) {
      levelCriteria.add(criterion);
    }
    requirementsByCurrentLevel.put(currentLevel, levelCriteria);
  }
}
