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
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.components.house.AppartmentComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseAppartmentRegistry;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;

public final class StructureConfig {
  public static final BlockType CONSTRUCTION_SIGN = BlockType.getAssetMap()
      .getAsset("Village_Structure_In_Construction");
  private static final int LONGEST_CONSTRUCTION_SIDE;
  private static final int SMALLEST_CONSTRUCTION_SIDE;

  static {
    int longest = Math.max(getLongestSide(Mine.SIZE_2I),
        getLongestSide(Farm.SIZE_2I));
    int longest1 = Math.max(getLongestSide(Storage.SIZE_2I),
        getLongestSide(Tavern.SIZE_2I));
    int longest2 = Math.max(getLongestSide(House.SIZE_2I),
        getLongestSide(Sawmill.SIZE_2I));
    int longest3 = Math.max(longest, longest1);
    LONGEST_CONSTRUCTION_SIDE = Math.max(longest3, longest2);

    int smallest = Math.min(getSmallestSide(Mine.SIZE_2I),
        getSmallestSide(Farm.SIZE_2I));
    int smallest1 = Math.min(getSmallestSide(Storage.SIZE_2I),
        getSmallestSide(Tavern.SIZE_2I));
    int smallest2 = Math.min(getSmallestSide(House.SIZE_2I),
        getSmallestSide(Sawmill.SIZE_2I));
    int smallest3 = Math.min(smallest, smallest1);
    SMALLEST_CONSTRUCTION_SIDE = Math.min(smallest3, smallest2);
  }

  public static int getLongestSide() {
    return LONGEST_CONSTRUCTION_SIDE;
  }

  public static int getSmallestSide() {
    return SMALLEST_CONSTRUCTION_SIDE;
  }

  private static int getLongestSide(Vector2i size) {
    return Math.max(size.x, size.y);
  }

  private static int getSmallestSide(Vector2i size) {
    return Math.min(size.x, size.y);
  }

  public record BlockTransform(Vector3i position, Rotation rotation) {
  }

  private StructureConfig() {
  }

  public static final class TownHeart {
    public static final String PREFAB_JSON = "Structures/General/CityCenter/Level1/City_Center.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Village_Core");
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private TownHeart() {
    }
  }

  public static final class Farm {
    public static final String PREFAB_JSON = "Structures/Temporary/Farm.prefab.json";
    // public static final String PREFAB_JSON =
    // "Structures/Workplace/Workplace_Farm.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Farm_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final AreaFootprint WORKING_AREA_OFFSET = new AreaFootprint(
        new Vector2i(-3, -3),
        new Vector2i(3, 3));
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(0, 1, -4);
    public static final Vector3i CONTAINER_OFFSET = new Vector3i(2, 0, 0);
    public static final Vector3i BENCH_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector3i CONTAINER_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vector3i CONSTRUCTION_SIGN_OFFSET = new Vector3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        2,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        3,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON));

    private Farm() {
    }

    public static String getPrefab(int farmLevel, short stage) {
      return PREFAB_MAP.get(farmLevel).get(stage);
    }
  }

  public static final class Storage {
    public static final String PREFAB_JSON = "Structures/Workplace/Storage/Level1/Stage3/Storage.prefab.json";
    // public static final String PREFAB_JSON =
    // "Structures/Workplace/Workplace_Storage.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Storage_Core");
    public static final BlockType DOUBLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Large");
    public static final Rotation LEFT_CONTAINER_YAW_OFFSET = Rotation.Ninety;
    public static final Rotation RIGHT_CONTAINER_YAW_OFFSET = Rotation.TwoSeventy;
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(0, 1, -3);
    public static final Vector3i BENCH_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector3i LEFT_CONTAINER_ACCESS_OFFSET = new Vector3i(1,
        0, 0);
    public static final Vector3i RIGHT_CONTAINER_ACCESS_OFFSET = new Vector3i(-1,
        0, 0);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vector3i CONSTRUCTION_SIGN_OFFSET = new Vector3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/Storage/Level1/Stage0/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage1/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage2/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage3/Storage.prefab.json"),

        2,
        createStagePrefabMap(
            "Structures/Workplace/Storage/Level1/Stage0/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage1/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage2/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage3/Storage.prefab.json"),

        3,
        createStagePrefabMap(
            "Structures/Workplace/Storage/Level1/Stage0/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage1/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage2/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage3/Storage.prefab.json"));

    private static final Map<Integer, List<Vector3i>> STORAGE_SPAWNING_OFFSETS = Map.of(
        1,
        List.of(
            new Vector3i(-3, 1, 1),
            new Vector3i(-3, 1, -2),
            new Vector3i(3, 1, 2),
            new Vector3i(3, 1, -1)));

    private Storage() {
    }

    public static String getPrefab(int storageLevel, short stage) {
      return PREFAB_MAP.get(storageLevel).get(stage);
    }

    public static List<Vector3i> getStorageOffsetForLevel(int level) {
      return STORAGE_SPAWNING_OFFSETS.get(level);
    }
  }

  public static final class Road {
    private static final int MAIN_ROAD_SIZE = 5;
    private static final int SIDE_ROAD_SIZE = 4;

    private static final Map<Short, Map<RoadLength, String>> VILLAGE_LVL_MAIN_ROADS = Map.of(
        (short) 1,
        Map.of(
            RoadLength.SMALL, "Structures/Temporary/Main_Road_5x1.prefab.json",
            RoadLength.MEDIUM, "Structures/Temporary/Main_Road_5x2.prefab.json",
            RoadLength.LARGE, "Structures/Temporary/Main_Road_5x4.prefab.json"));

    private static final Map<Short, Map<RoadLength, String>> VILLAGE_LVL_SIDE_ROADS = Map.of(
        (short) 1,
        Map.of(
            RoadLength.SMALL, "Structures/Temporary/Side_Road_2x1.prefab.json",
            RoadLength.MEDIUM, "Structures/Temporary/Side_Road_2x2.prefab.json",
            RoadLength.LARGE, "Structures/Temporary/Side_Road_2x4.prefab.json"));

    private static final RoadLength[] ORDERED_ROAD_LENGTH = new RoadLength[] {
        RoadLength.LARGE, RoadLength.MEDIUM,
        RoadLength.SMALL };

    public enum RoadType {
      MAIN_ROAD, SIDE_ROAD;
    }

    public enum RoadLength {
      SMALL((short) 1), MEDIUM((short) 2), LARGE((short) 4);

      private short length;

      private RoadLength(short length) {
        this.length = length;
      }

      public short getLength() {
        return length;
      }
    }

    public static RoadLength[] getOrderedRoadLength() {
      return ORDERED_ROAD_LENGTH;
    }

    public static String getRoadPrefab(short villageLvl, RoadType type,
        RoadLength length) {
      Map<Short, Map<RoadLength, String>> prefabsByLevel = type.equals(RoadType.MAIN_ROAD)
          ? VILLAGE_LVL_MAIN_ROADS
          : VILLAGE_LVL_SIDE_ROADS;
      Map<RoadLength, String> prefabs = prefabsByLevel.getOrDefault(
          villageLvl,
          prefabsByLevel.get((short) 1));
      return prefabs == null ? null : prefabs.get(length);
    }

    public static int getRoadSize(RoadType type) {
      return switch (type) {
        case MAIN_ROAD -> MAIN_ROAD_SIZE;
        case SIDE_ROAD -> SIDE_ROAD_SIZE;
      };
    }

    public static int getRoadLength(RoadType type) {
      return getRoadSize(type);
    }
  }

  public static final class Tavern {
    public static final String PREFAB_JSON = "Structures/Workplace/Tavern/Level1/Stage3/Tavern.prefab.json";
    // public static final String PREFAB_JSON =
    // "Structures/Workplace/Workplace_Tavern.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Tavern_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final Rotation WORKPLACE_BLOCK_YAW_OFFSET = Rotation.None;
    public static final Rotation CONTAINER_BLOCK_YAW_OFFSET = Rotation.OneEighty;
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(-4, 1, -3);
    public static final Vector3i BENCH_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector3i CONTAINER_ACCESS_OFFSET = new Vector3i(0, 0, -1);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vector3i CONSTRUCTION_SIGN_OFFSET = new Vector3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    private static final Map<Integer, List<Vector3i>> STORAGE_SPAWNING_OFFSETS = Map.of(
        1,
        List.of(new Vector3i(-4, 1, 3)));

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/Tavern/Level1/Stage0/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage1/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage2/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage3/Tavern.prefab.json"),

        2,
        createStagePrefabMap(
            "Structures/Workplace/Tavern/Level1/Stage0/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage1/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage2/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage3/Tavern.prefab.json"),

        3,
        createStagePrefabMap(
            "Structures/Workplace/Tavern/Level1/Stage0/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage1/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage2/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage3/Tavern.prefab.json"));

    private static final Map<Integer, List<Interactable>> INTERACTABLES = Map.of(
        1,
        List.of(
            new Interactable(new Vector3i(0, 1, 0), new Vector3i(0, 2, -1), true),
            new Interactable(new Vector3i(-1, 1, 0), new Vector3i(-1, 2, -1), true),
            new Interactable(new Vector3i(0, 1, -3), new Vector3i(0, 2, -2), true),
            new Interactable(new Vector3i(-1, 1, -3), new Vector3i(-1, 2, -2), true),

            new Interactable(new Vector3i(2, 1, -1), new Vector3i(3, 2, -1), true),
            new Interactable(new Vector3i(2, 1, -2), new Vector3i(3, 2, -2), true),
            new Interactable(new Vector3i(5, 1, -1), new Vector3i(4, 2, -1), true),
            new Interactable(new Vector3i(5, 1, -2), new Vector3i(4, 2, -2), true),

            new Interactable(new Vector3i(2, 1, 1), new Vector3i(3, 2, 1), true),
            new Interactable(new Vector3i(2, 1, 2), new Vector3i(3, 2, 2), true),
            new Interactable(new Vector3i(5, 1, 1), new Vector3i(4, 2, 1), true),
            new Interactable(new Vector3i(5, 1, 2), new Vector3i(4, 2, 2), true)));

    private Tavern() {
    }

    public static List<Vector3i> getStorageOffsetForLevel(int level) {
      return STORAGE_SPAWNING_OFFSETS.get(level);
    }

    public static String getPrefab(int tavernLevel, short stage) {
      return PREFAB_MAP.get(tavernLevel).get(stage);
    }

    public static List<Interactable> getInteractables(Vector3i prefabAnchor,
        Rotation rotation, int level) {
      return toAbsoluteInteractables(INTERACTABLES.get(level), prefabAnchor,
          rotation);
    }
  }

  public static final class TownHall {
    public static final String PREFAB_JSON = "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json";
    // public static final String PREFAB_JSON =
    // "Structures/Workplace/Workplace_Town_Hall.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Town_Hall_Core");
    public static final Rotation WORKPLACE_BLOCK_YAW_OFFSET = Rotation.Ninety;
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(-3, 1, -1);
    public static final Vector3i BENCH_ACCESS_OFFSET = new Vector3i(0, 0, -1);
    public static final Vector3i CONTAINER_ACCESS_OFFSET = new Vector3i(0, 0, 1);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vector3i CONSTRUCTION_SIGN_OFFSET = new Vector3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json"),

        2,
        createStagePrefabMap(
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json"),

        3,
        createStagePrefabMap(
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json"));

    private TownHall() {
    }

    public static String getPrefab(int townHallLevel, short stage) {
      return PREFAB_MAP.get(townHallLevel).get(stage);
    }
  }

  public static final class Mine {
    public static final String PREFAB_JSON = "Structures/Temporary/Mine.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Mine_Core");
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(0, 1, 0);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vector3i CONSTRUCTION_SIGN_OFFSET = new Vector3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    private Mine() {
    }

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        2,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        3,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON));

    public static String getPrefab(int mineLevel, short stage) {
      return PREFAB_MAP.get(mineLevel).get(stage);
    }
  }

  public static final class Sawmill {
    public static final String PREFAB_JSON = "Structures/Temporary/Sawmill.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Sawmill_Core");
    public static final Vector3i WORKPLACE_BLOCK_OFFSET = new Vector3i(0, 1, 0);
    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vector3i CONSTRUCTION_SIGN_OFFSET = new Vector3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    private Sawmill() {
    }

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        2,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        3,
        createStagePrefabMap(
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON));

    public static String getPrefab(int sawmillLevel, short stage) {
      return PREFAB_MAP.get(sawmillLevel).get(stage);
    }
  }

  public static final class House {
    // public static final String PREFAB_JSON =
    // "Structures/Temporary/House.prefab.json";
    public static final String PREFAB_JSON = "Structures/Housing/Level1/Stage0/House.prefab.json";
    public static final BlockType CORE_BLOCK = BlockType.getAssetMap().getAsset("Housing_Core");
    public static final BlockType CRUDE_BED_BLOCK = BlockType.getAssetMap().getAsset("Village_Crude_Bed");
    public static final Vector3i CORE_BLOCK_OFFSET = new Vector3i(-4, 1, 2);
    public static final Rotation WORKPLACE_BLOCK_YAW_OFFSET = Rotation.Ninety;
    public static final Vector3i CONSTRUCTION_SIGN_OFFSET = new Vector3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;
    private static final String SAMPLE_PREFAB = "Structures/Housing/Level1/Stage0/House.prefab.json";

    public static final Vector2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SAMPLE_PREFAB);
    public static final Vector3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SAMPLE_PREFAB);

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Housing/Level1/Stage0/House.prefab.json",
            "Structures/Housing/Level1/Stage1/House.prefab.json",
            "Structures/Housing/Level1/Stage2/House.prefab.json",
            "Structures/Housing/Level1/Stage3/House.prefab.json"),

        2,
        createStagePrefabMap(
            "Structures/Housing/Level2/Stage0/House.prefab.json",
            "Structures/Housing/Level2/Stage1/House.prefab.json",
            "Structures/Housing/Level2/Stage2/House.prefab.json",
            "Structures/Housing/Level2/Stage3/House.prefab.json"),

        3,
        createStagePrefabMap(
            "Structures/Housing/Level3/Stage0/House.prefab.json",
            "Structures/Housing/Level3/Stage1/House.prefab.json",
            "Structures/Housing/Level3/Stage2/House.prefab.json",
            "Structures/Housing/Level3/Stage3/House.prefab.json"));

    private static final Map<Integer, List<List<BlockTransform>>> APPARTMENT_BED_OFFSETS = Map.of(
        1,
        List.of(
            List.of(new BlockTransform(new Vector3i(2, 1, -3), Rotation.None)),
            List.of(new BlockTransform(new Vector3i(4, 1, -3), Rotation.None))));

    public static String getPrefab(int houseLevel, short stage) {
      return PREFAB_MAP.get(houseLevel).get(stage);
    }

    private House() {
    }

    public static HouseAppartmentRegistry getAppartments(Vector3i prefabAnchor,
        Rotation rotation, int level) {
      HouseAppartmentRegistry registry = new HouseAppartmentRegistry();
      List<List<BlockTransform>> appartments = APPARTMENT_BED_OFFSETS.getOrDefault(
          level,
          APPARTMENT_BED_OFFSETS.getOrDefault(1, List.of()));

      for (List<BlockTransform> bedOffsets : appartments) {
        AppartmentComponent appartment = new AppartmentComponent();
        Map<UUID, VillageBedComponent> beds = new HashMap<>();

        for (BlockTransform bedOffset : bedOffsets) {
          VillageBedComponent bed = new VillageBedComponent();
          bed.setTaken(false);
          bed.setPosition(toAbsolutePosition(prefabAnchor, bedOffset.position,
              rotation));
          Rotation finalBedRotation = StructurePlacementUtils.addYaw(bedOffset.rotation(), rotation);
          bed.setRotation(finalBedRotation);
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

  private static Map<Short, String> createStagePrefabMap(
      String stage0Prefab,
      String stage1Prefab,
      String stage2Prefab,
      String stage3Prefab) {
    return Map.of(
        (short) 0, stage0Prefab,
        (short) 1, stage1Prefab,
        (short) 2, stage2Prefab,
        (short) 3, stage3Prefab);
  }
}
