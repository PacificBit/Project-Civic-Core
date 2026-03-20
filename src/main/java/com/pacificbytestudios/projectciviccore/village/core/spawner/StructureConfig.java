package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.components.house.AppartmentComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseAppartmentRegistry;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;

public final class StructureConfig {
  private StructureConfig() {
  }

  public static final class TownHeart {
    public static final String PREFAB_JSON = "Structures/Temporary/City_Center.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Village_Core");
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private TownHeart() {
    }
  }

  public static final class Farm {
    public static final String PREFAB_JSON = "Structures/Temporary/Farm.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Farm_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final ClaimedAreaComponent WORKING_AREA_OFFSET = new ClaimedAreaComponent(
        new Vector2i(-3, -3),
        new Vector2i(3, 3));
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(0, 1, -4);
    public static final Vector3i CONTAINER_OFFSET = new Vector3i(2, 0, 0);
    public static final Vector3i BENCH_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector3i CONTAINER_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private Farm() {
    }
  }

  public static final class Storage {
    public static final String PREFAB_JSON = "Structures/Temporary/Storage.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Storage_Core");
    public static final BlockType DOUBLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Large");
    public static final Rotation LEFT_CONTAINER_YAW_OFFSET = Rotation.Ninety;
    public static final Rotation RIGHT_CONTAINER_YAW_OFFSET = Rotation.TwoSeventy;
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(0, 1, -3);
    public static final Vector3i BENCH_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector3i LEFT_CONTAINER_ACCESS_OFFSET = new Vector3i(1, 0, 0);
    public static final Vector3i RIGHT_CONTAINER_ACCESS_OFFSET = new Vector3i(-1, 0, 0);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private static final Map<Integer, List<Vector3i>> STORAGE_SPAWNING_OFFSETS = Map.of(
        1,
        List.of(
            new Vector3i(-3, 1, 2),
            new Vector3i(-3, 1, -1),
            new Vector3i(3, 1, 3),
            new Vector3i(3, 1, 0)));

    private Storage() {
    }

    public static List<Vector3i> getStorageOffsetForLevel(int level) {
      return STORAGE_SPAWNING_OFFSETS.get(level);
    }
  }

  public static final class Road {
    private static final Map<Short, Map<RoadLength, String>> VILLAGE_LVL_MAIN_ROADS = Map.of(
        (short) 1,
        Map.of(
            RoadLength.SMALL, "Structures/Temporary/Main_Road_5x1.prefab.json",
            RoadLength.MEDIUM, "Structures/Temporary/Main_Road_5x2.prefab.json",
            RoadLength.LARGE, "Structures/Temporary/Main_Road_5x4.prefab.json"));

    private static final Map<Short, Map<RoadLength, String>> VILLAGE_LVL_SIDE_ROADS = Map.of(
        (short) 1,
        Map.of(
            RoadLength.SMALL, "Structures/Temporary/Side_Road_4x1.prefab.json",
            RoadLength.MEDIUM, "Structures/Temporary/Side_Road_4x2.prefab.json",
            RoadLength.LARGE, "Structures/Temporary/Side_Road_4x4.prefab.json"));

    public enum RoadType {
      MAIN_ROAD, SIDE_ROAD;
    }

    public enum RoadLength {
      SMALL, MEDIUM, LARGE;
    }

    public static String getRoadPrefab(short villageLvl, RoadType type, RoadLength length) {
      Map<Short, Map<RoadLength, String>> prefabsByLevel = type.equals(RoadType.MAIN_ROAD)
          ? VILLAGE_LVL_MAIN_ROADS
          : VILLAGE_LVL_SIDE_ROADS;
      Map<RoadLength, String> prefabs = prefabsByLevel.getOrDefault(
          villageLvl,
          prefabsByLevel.get((short) 1));
      return prefabs == null ? null : prefabs.get(length);
    }

  }

  public static final class Tavern {
    public static final String PREFAB_JSON = "Structures/Temporary/Tavern.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Tavern_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final Rotation WORKPLACE_BLOCK_YAW_OFFSET = Rotation.Ninety;
    public static final Rotation CONTAINER_BLOCK_YAW_OFFSET = Rotation.Ninety;
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(-4, 1, -4);
    public static final Vector3i BENCH_ACCESS_OFFSET = new Vector3i(1, 0, 0);
    public static final Vector3i CONTAINER_ACCESS_OFFSET = new Vector3i(1, 0, 0);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private static final Map<Integer, List<Vector3i>> STORAGE_SPAWNING_OFFSETS = Map.of(
        1,
        List.of(new Vector3i(-4, 1, -2)));

    private static final Map<Integer, List<Interactable>> INTERACTABLES = Map.of(
        1,
        List.of(
            new Interactable(new Vector3i(-2, 1, 1), new Vector3i(-2, 2, 2), true),
            new Interactable(new Vector3i(-3, 1, 1), new Vector3i(-3, 2, 2), true),
            new Interactable(new Vector3i(-2, 1, 4), new Vector3i(-2, 2, 3), true),
            new Interactable(new Vector3i(-3, 1, 4), new Vector3i(-3, 2, 3), true),
            new Interactable(new Vector3i(2, 1, 1), new Vector3i(2, 2, 2), true),
            new Interactable(new Vector3i(3, 1, 1), new Vector3i(3, 2, 2), true),
            new Interactable(new Vector3i(2, 1, 4), new Vector3i(2, 2, 3), true),
            new Interactable(new Vector3i(3, 1, 4), new Vector3i(3, 2, 3), true),
            new Interactable(new Vector3i(2, 1, -1), new Vector3i(2, 2, -2), true),
            new Interactable(new Vector3i(3, 1, -1), new Vector3i(3, 2, -2), true),
            new Interactable(new Vector3i(2, 1, -4), new Vector3i(2, 2, -3), true),
            new Interactable(new Vector3i(3, 1, -4), new Vector3i(3, 2, -3), true)));

    private Tavern() {
    }

    public static List<Vector3i> getStorageOffsetForLevel(int level) {
      return STORAGE_SPAWNING_OFFSETS.get(level);
    }

    public static List<Interactable> getInteractables(Vector3i prefabAnchor, Rotation rotation, int level) {
      return toAbsoluteInteractables(INTERACTABLES.get(level), prefabAnchor, rotation);
    }
  }

  public static final class TownHall {
    public static final String PREFAB_JSON = "Structures/Temporary/Town_Hall.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Town_Hall_Core");
    public static final Rotation WORKPLACE_BLOCK_YAW_OFFSET = Rotation.OneEighty;
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(-4, 1, 4);
    public static final Vector3i BENCH_ACCESS_OFFSET = new Vector3i(0, 0, -1);
    public static final Vector3i CONTAINER_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private TownHall() {
    }
  }

  public static final class Mine {
    public static final String PREFAB_JSON = "Structures/Temporary/Mine.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Mine_Core");
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(0, 1, 0);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private Mine() {
    }
  }

  public static final class Sawmill {
    public static final String PREFAB_JSON = "Structures/Temporary/Sawmill.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Sawmill_Core");
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(0, 1, 0);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private Sawmill() {
    }
  }

  public static final class House {
    public static final String PREFAB_JSON = "Structures/Temporary/House.prefab.json";
    public static final BlockType CORE_BLOCK = BlockType.getAssetMap().getAsset("Housing_Core");
    public static final BlockType CRUDE_BED_BLOCK = BlockType.getAssetMap().getAsset("Village_Crude_Bed");
    public static final Vector3i CORE_BLOCK_OFFSET = new Vector3i(0, 1, -3);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private static final Map<Integer, List<List<Vector3i>>> APPARTMENT_BED_OFFSETS = Map.of(
        1,
        List.of(
            List.of(new Vector3i(-2, 1, 0)),
            List.of(new Vector3i(2, 1, 0))));

    private House() {
    }

    public static HouseAppartmentRegistry getAppartments(Vector3i prefabAnchor, Rotation rotation, int level) {
      HouseAppartmentRegistry registry = new HouseAppartmentRegistry();
      List<List<Vector3i>> appartments = APPARTMENT_BED_OFFSETS.getOrDefault(
          level,
          APPARTMENT_BED_OFFSETS.getOrDefault(1, List.of()));

      for (List<Vector3i> bedOffsets : appartments) {
        AppartmentComponent appartment = new AppartmentComponent();
        Map<UUID, VillageBedComponent> beds = new HashMap<>();

        for (Vector3i bedOffset : bedOffsets) {
          VillageBedComponent bed = new VillageBedComponent();
          bed.setTaken(false);
          bed.setPosition(toAbsolutePosition(prefabAnchor, bedOffset, rotation));
          beds.put(UUID.randomUUID(), bed);
        }

        appartment.setBeds(beds);
        registry.registerAppartment(UUID.randomUUID(), appartment);
      }

      return registry;
    }
  }

  private static List<Interactable> toAbsoluteInteractables(
      List<Interactable> template,
      Vector3i prefabAnchor,
      Rotation rotation) {
    if (template == null || template.isEmpty()) {
      return List.of();
    }

    List<Interactable> result = new ArrayList<>(template.size());
    for (Interactable interactable : template) {
      if (interactable == null) {
        continue;
      }

      Interactable copy = interactable.clone();
      copy.setInteractablePosition(toAbsolutePosition(
          prefabAnchor,
          interactable.getInteractablePosition(),
          rotation));

      if (interactable.getDependsOn() != null) {
        copy.setDependsOn(toAbsolutePosition(
            prefabAnchor,
            interactable.getDependsOn(),
            rotation));
      }

      result.add(copy);
    }

    return result;
  }

  private static Vector3i toAbsolutePosition(Vector3i prefabAnchor, Vector3i offset, Rotation rotation) {
    return StructurePlacementUtils.withOffset(prefabAnchor, offset, rotation);
  }
}
