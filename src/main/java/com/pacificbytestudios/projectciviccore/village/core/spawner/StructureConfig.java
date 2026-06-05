package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartConfigComponent;
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

  private static int getLongestSide(Vec2i size) {
    return Math.max(size.x, size.y);
  }

  private static int getSmallestSide(Vec2i size) {
    return Math.min(size.x, size.y);
  }

  public record BlockTransform(Vec3i position, Rotation rotation) {
  }

  public record ContainerTransform(Vec3i position, Rotation rotation, Vec3f accessOffset) {
  }

  private StructureConfig() {
  }

  public static final class TownHeart {
    public static final String PREFAB_JSON = "Structures/General/CityCenter/Level1/City_Center.prefab.json";
    public static final BlockType MARKER_BLOCK = BlockType.getAssetMap().getAsset("Village_Heart_Marker");
    public static final BlockType HEART_BLOCK = BlockType.getAssetMap().getAsset("Village_Core");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final VillageHeartConfigComponent DEFAULT_CONFIG;

    static {
      DEFAULT_CONFIG = HEART_BLOCK.getBlockEntity().getComponent(VillageHeartComponent.getComponentType()).getConfig();
    }

    private TownHeart() {
    }

    private static final int SPAWNING_SCALE = 6;

    private static final List<Vec3f> VILLAGER_SPAWNING_OFFSETS = List.of(
        Vec3f.NORTH.safeMul(SPAWNING_SCALE),
        Vec3f.SOUTH.safeMul(SPAWNING_SCALE),
        Vec3f.WEST.safeMul(SPAWNING_SCALE),
        Vec3f.EAST.safeMul(SPAWNING_SCALE));

    public static List<Vec3f> getVillagerSpawningOffsets() {
      return VILLAGER_SPAWNING_OFFSETS;
    }
  }

  public static final class Farm {
    public record LevelLayout(
        BlockTransform workplace,
        BlockTransform constructionSign,
        Vec3f benchAccessOffset,
        ContainerTransform container,
        AreaFootprint workingAreaOffset) {
    }

    public static final String PREFAB_JSON = "Structures/Workplace/Farm/Level1/Stage0/Farm.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Farm_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final Vec3i WORKPLACE_BLOCK_OFFSET = new Vec3i(-5, 1, -5);
    public static final Vec3f BENCH_ACCESS_OFFSET = new Vec3f(0, 0, 1);

    public static final Vec3i CONTAINER_OFFSET = new Vec3i(0, 0, 8);
    public static final Rotation CONTAINER_ROTATION = Rotation.Ninety;

    public static final Vec3f CONTAINER_ACCESS_OFFSET = new Vec3f(0, 0, -1);
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    public static final Vec3i CONSTRUCTION_SIGN_OFFSET = new Vec3i(3, 1, 6);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    public static final AreaFootprint WORKING_AREA_OFFSET = new AreaFootprint(
        new Vec2i(-1, -4),
        new Vec2i(7, 4));

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/Farm/Level1/Stage0/Farm.prefab.json",
            "Structures/Workplace/Farm/Level1/Stage1/Farm.prefab.json",
            "Structures/Workplace/Farm/Level1/Stage2/Farm.prefab.json",
            "Structures/Workplace/Farm/Level1/Stage3/Farm.prefab.json"),

        2,
        createStagePrefabMap(
            // TODO: complete this
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        3,
        createStagePrefabMap(
            // TODO: complete this
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON));

    private static final Map<Integer, LevelLayout> LEVEL_LAYOUTS = Map.of(
        1,
        new LevelLayout(
            new BlockTransform(new Vec3i(-5, 1, -4), Rotation.None),
            new BlockTransform(new Vec3i(3, 1, 6), Rotation.None),
            new Vec3f(0, 0, 1),
            new ContainerTransform(new Vec3i(-5, 1, 3), Rotation.TwoSeventy, new Vec3f(0, 0, -1)),
            new AreaFootprint(new Vec2i(-1, -4), new Vec2i(7, 4))),

        2,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(-5, 1, -5), Rotation.None),
            new BlockTransform(new Vec3i(3, 1, 6), Rotation.None),
            new Vec3f(0, 0, 1),
            new ContainerTransform(new Vec3i(-5, 1, 3), Rotation.Ninety, new Vec3f(0, 0, -1)),
            new AreaFootprint(new Vec2i(-1, -4), new Vec2i(7, 4))),

        3,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(-5, 1, -5), Rotation.None),
            new BlockTransform(new Vec3i(3, 1, 6), Rotation.None),
            new Vec3f(0, 0, 1),
            new ContainerTransform(new Vec3i(-5, 1, 3), Rotation.Ninety, new Vec3f(0, 0, -1)),
            new AreaFootprint(new Vec2i(-1, -4), new Vec2i(7, 4))));

    private Farm() {
    }

    public static String getPrefab(int farmLevel, short stage) {
      Map<Short, String> stagePrefabs = PREFAB_MAP.getOrDefault(farmLevel, PREFAB_MAP.get(1));
      return stagePrefabs == null ? null : stagePrefabs.get(stage);
    }

    public static String getCompletedPrefab(int farmLevel) {
      return getPrefab(farmLevel, (short) 3);
    }

    public static LevelLayout getLevelLayout(int farmLevel) {
      return LEVEL_LAYOUTS.getOrDefault(farmLevel, LEVEL_LAYOUTS.get(1));
    }
  }

  public static final class Storage {
    public record LevelLayout(
        BlockTransform workplace,
        BlockTransform constructionSign,
        Vec3f benchAccessOffset,
        List<ContainerTransform> containers) {
    }

    public static final String PREFAB_JSON = "Structures/Workplace/Storage/Level1/Stage3/Storage.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Storage_Core");
    public static final BlockType DOUBLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Large");
    public static final Rotation LEFT_CONTAINER_YAW_OFFSET = Rotation.Ninety;
    public static final Rotation RIGHT_CONTAINER_YAW_OFFSET = Rotation.TwoSeventy;
    public static final Vec3i WORKPLACE_BLOCK_OFFSET = new Vec3i(0, 1, -3);
    public static final Vec3f BENCH_ACCESS_OFFSET = new Vec3f(0, 0, 1);
    public static final Vec3f LEFT_CONTAINER_ACCESS_OFFSET = new Vec3f(1,
        0, 0);
    public static final Vec3f RIGHT_CONTAINER_ACCESS_OFFSET = new Vec3f(-1,
        0, 0);
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/Storage/Level1/Stage0/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage1/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage2/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage3/Storage.prefab.json"),

        2,
        createStagePrefabMap(
            "Structures/Workplace/Storage/Level2/Stage0/Storage.prefab.json",
            "Structures/Workplace/Storage/Level2/Stage1/Storage.prefab.json",
            "Structures/Workplace/Storage/Level2/Stage2/Storage.prefab.json",
            "Structures/Workplace/Storage/Level2/Stage3/Storage.prefab.json"),

        3,
        createStagePrefabMap(
            // TODO: complete this
            "Structures/Workplace/Storage/Level1/Stage0/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage1/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage2/Storage.prefab.json",
            "Structures/Workplace/Storage/Level1/Stage3/Storage.prefab.json"));

    private static final Map<Integer, LevelLayout> LEVEL_LAYOUTS = Map.of(
        1,
        new LevelLayout(
            new BlockTransform(new Vec3i(0, 1, -3), Rotation.None),
            new BlockTransform(new Vec3i(0, 1, 7), Rotation.None),
            new Vec3f(0, 0, 1),
            List.of(
                new ContainerTransform(new Vec3i(-3, 1, 1), Rotation.Ninety, new Vec3f(1, 0, 0)),
                new ContainerTransform(new Vec3i(-3, 1, -2), Rotation.Ninety, new Vec3f(1, 0, 0)),
                new ContainerTransform(new Vec3i(3, 1, 2), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)),
                new ContainerTransform(new Vec3i(3, 1, -1), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)))),

        2,
        new LevelLayout(
            new BlockTransform(new Vec3i(-1, 1, -4), Rotation.None),
            new BlockTransform(new Vec3i(0, 1, 7), Rotation.None),
            new Vec3f(0, 0, 2),
            List.of(
                new ContainerTransform(new Vec3i(2, 1, -2), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)),
                new ContainerTransform(new Vec3i(2, 1, 1), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)),
                new ContainerTransform(new Vec3i(2, 1, 4), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)),

                new ContainerTransform(new Vec3i(-5, 5, -1), Rotation.OneEighty, new Vec3f(0, 0, -1)),
                new ContainerTransform(new Vec3i(3, 5, 4), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)),
                new ContainerTransform(new Vec3i(-1, 5, 1), Rotation.Ninety, new Vec3f(1, 0, 0)),
                new ContainerTransform(new Vec3i(1, 5, 5), Rotation.OneEighty, new Vec3f(0, 0, -1)),

                new ContainerTransform(new Vec3i(-4, 5, -3), Rotation.None, new Vec3f(0, 0, 1)),
                new ContainerTransform(new Vec3i(-5, 5, -4), Rotation.OneEighty, new Vec3f(0, 0, -1)),

                new ContainerTransform(new Vec3i(-1, 5, -3), Rotation.None, new Vec3f(0, 0, 1)),
                new ContainerTransform(new Vec3i(-2, 5, -4), Rotation.OneEighty, new Vec3f(0, 0, -1)),

                new ContainerTransform(new Vec3i(2, 5, -3), Rotation.None, new Vec3f(0, 0, 1)),
                new ContainerTransform(new Vec3i(1, 5, -4), Rotation.OneEighty, new Vec3f(0, 0, -1))

            )),

        3,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(0, 1, -3), Rotation.None),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None),
            new Vec3f(0, 0, 1),
            List.of(
                new ContainerTransform(new Vec3i(-3, 1, 1), Rotation.Ninety, new Vec3f(1, 0, 0)),
                new ContainerTransform(new Vec3i(-3, 1, -2), Rotation.Ninety, new Vec3f(1, 0, 0)),
                new ContainerTransform(new Vec3i(3, 1, 2), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)),
                new ContainerTransform(new Vec3i(3, 1, -1), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)))));

    private Storage() {
    }

    public static String getPrefab(int storageLevel, short stage) {
      Map<Short, String> stagePrefabs = PREFAB_MAP.getOrDefault(storageLevel, PREFAB_MAP.get(1));
      return stagePrefabs == null ? null : stagePrefabs.get(stage);
    }

    public static String getCompletedPrefab(int storageLevel) {
      return getPrefab(storageLevel, (short) 3);
    }

    public static LevelLayout getLevelLayout(int storageLevel) {
      return LEVEL_LAYOUTS.getOrDefault(storageLevel, LEVEL_LAYOUTS.get(1));
    }

    public static List<Vec3i> getStorageOffsetForLevel(int level) {
      LevelLayout layout = getLevelLayout(level);
      List<Vec3i> offsets = new ArrayList<>();
      for (ContainerTransform container : layout.containers()) {
        offsets.add(container.position());
      }
      return offsets;
    }
  }

  public static final class Road {
    private static final int MAIN_ROAD_SIZE = 5;
    private static final int SIDE_ROAD_SIZE = 4;

    private static final Map<Short, Map<RoadLength, String>> VILLAGE_LVL_MAIN_ROADS = Map.of(
        (short) 1,
        Map.of(
            RoadLength.SMALL, "Structures/Roads/Main_Road_5x1.prefab.json",
            RoadLength.MEDIUM, "Structures/Roads/Main_Road_5x2.prefab.json",
            RoadLength.LARGE, "Structures/Roads/Main_Road_5x4.prefab.json"));

    private static final Map<Short, Map<RoadLength, String>> VILLAGE_LVL_SIDE_ROADS = Map.of(
        (short) 1,
        Map.of(
            RoadLength.SMALL, "Structures/Roads/Side_Road_2x1.prefab.json",
            RoadLength.MEDIUM, "Structures/Roads/Side_Road_2x2.prefab.json",
            RoadLength.LARGE, "Structures/Roads/Side_Road_2x4.prefab.json"));

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
    public record LevelLayout(
        BlockTransform workplace,
        BlockTransform constructionSign,
        Vec3f benchAccessOffset,
        List<ContainerTransform> containers) {
    }

    public static final String PREFAB_JSON = "Structures/Workplace/Tavern/Level1/Stage3/Tavern.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Tavern_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final Rotation CONTAINER_BLOCK_YAW_OFFSET = Rotation.OneEighty;
    public static final Vec3f BENCH_ACCESS_OFFSET = new Vec3f(0, 0, 1);
    public static final Vec3f CONTAINER_ACCESS_OFFSET = new Vec3f(0, 0, -1);
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vec3i CONSTRUCTION_SIGN_OFFSET = new Vec3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/Tavern/Level1/Stage0/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage1/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage2/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level1/Stage3/Tavern.prefab.json"),

        2,
        createStagePrefabMap(
            "Structures/Workplace/Tavern/Level2/Stage0/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level2/Stage1/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level2/Stage2/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level2/Stage3/Tavern.prefab.json"),

        3,
        createStagePrefabMap(
            // TODO: complete this
            "Structures/Workplace/Tavern/Level2/Stage0/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level2/Stage1/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level2/Stage2/Tavern.prefab.json",
            "Structures/Workplace/Tavern/Level2/Stage3/Tavern.prefab.json"));

    private static final Map<Integer, LevelLayout> LEVEL_LAYOUTS = Map.of(
        1,
        new LevelLayout(
            new BlockTransform(new Vec3i(-4, 1, -3), Rotation.None),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None),
            new Vec3f(0, 0, 1),
            List.of(new ContainerTransform(new Vec3i(-5, 1, 3), Rotation.OneEighty, new Vec3f(0, 0, -1)))),

        2,
        new LevelLayout(
            new BlockTransform(new Vec3i(5, -3, 0), Rotation.TwoSeventy),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None),
            new Vec3f(-1, 0, 0),
            List.of(new ContainerTransform(new Vec3i(5, -3, -3), Rotation.TwoSeventy, new Vec3f(-1, 0, 0)))),

        3,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(5, -3, 0), Rotation.TwoSeventy),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None),
            new Vec3f(0, 0, 1),
            List.of(new ContainerTransform(new Vec3i(5, -3, -3), Rotation.OneEighty, new Vec3f(0, 0, -1)))));

    private static final Map<Integer, List<Interactable>> INTERACTABLES = Map.of(
        1,
        List.of(
            new Interactable(new Vec3f(0, 1, 0), new Vec3f(0, 2, -1), true),
            new Interactable(new Vec3f(-1, 1, 0), new Vec3f(-1, 2, -1), true),
            new Interactable(new Vec3f(0, 1, -3), new Vec3f(0, 2, -2), true),
            new Interactable(new Vec3f(-1, 1, -3), new Vec3f(-1, 2, -2), true),

            new Interactable(new Vec3f(2, 1, -1), new Vec3f(3, 2, -1), true),
            new Interactable(new Vec3f(2, 1, -2), new Vec3f(3, 2, -2), true),
            new Interactable(new Vec3f(5, 1, -1), new Vec3f(4, 2, -1), true),
            new Interactable(new Vec3f(5, 1, -2), new Vec3f(4, 2, -2), true),

            new Interactable(new Vec3f(2, 1, 1), new Vec3f(3, 2, 1), true),
            new Interactable(new Vec3f(2, 1, 2), new Vec3f(3, 2, 2), true),
            new Interactable(new Vec3f(5, 1, 1), new Vec3f(4, 2, 1), true),
            new Interactable(new Vec3f(5, 1, 2), new Vec3f(4, 2, 2), true)),

        2,
        List.of(
            new Interactable(new Vec3f(-1, -3, 3), new Vec3f(0, -2, 3), true),
            new Interactable(new Vec3f(-1, -3, 4), new Vec3f(0, -2, 4), true),
            new Interactable(new Vec3f(2, -3, 3), new Vec3f(1, -2, 3), true),
            new Interactable(new Vec3f(2, -3, 4), new Vec3f(1, -2, 4), true),

            new Interactable(new Vec3f(-2, -3, 1), new Vec3f(-3, -2, 1), true),
            new Interactable(new Vec3f(-2, -3, 2), new Vec3f(-3, -2, 2), true),
            new Interactable(new Vec3f(-5, -3, 1), new Vec3f(-4, -2, 1), true),
            new Interactable(new Vec3f(-5, -3, 2), new Vec3f(-4, -2, 2), true),

            new Interactable(new Vec3f(-2, -3, 3), new Vec3f(-3, -2, 3), true),
            new Interactable(new Vec3f(-2, -3, 4), new Vec3f(-3, -2, 4), true),
            new Interactable(new Vec3f(-5, -3, 3), new Vec3f(-4, -2, 3), true),
            new Interactable(new Vec3f(-5, -3, 4), new Vec3f(-4, -2, 4), true),

            new Interactable(new Vec3f(0, 5, -1), new Vec3f(0, 6, 0), true),
            new Interactable(new Vec3f(1, 5, -1), new Vec3f(1, 6, 0), true),
            new Interactable(new Vec3f(0, 5, 2), new Vec3f(0, 6, 1), true),
            new Interactable(new Vec3f(1, 5, 2), new Vec3f(1, 6, 1), true),

            new Interactable(new Vec3f(-2, 5, -1), new Vec3f(-2, 6, 0), true),
            new Interactable(new Vec3f(-3, 5, -1), new Vec3f(-3, 6, 0), true),
            new Interactable(new Vec3f(-2, 5, 2), new Vec3f(-2, 6, 1), true),
            new Interactable(new Vec3f(-3, 5, 2), new Vec3f(-3, 6, 1), true)),

        3,
        List.of(
            // TODO: complete this
            new Interactable(new Vec3f(-1, -3, 3), new Vec3f(0, -2, 3), true),
            new Interactable(new Vec3f(-1, -3, 4), new Vec3f(0, -2, 4), true),
            new Interactable(new Vec3f(2, -3, 3), new Vec3f(1, -2, 3), true),
            new Interactable(new Vec3f(2, -3, 4), new Vec3f(1, -2, 4), true),

            new Interactable(new Vec3f(-2, -3, 1), new Vec3f(-3, -2, 1), true),
            new Interactable(new Vec3f(-2, -3, 2), new Vec3f(-3, -2, 2), true),
            new Interactable(new Vec3f(-5, -3, 1), new Vec3f(-4, -2, 1), true),
            new Interactable(new Vec3f(-5, -3, 2), new Vec3f(-4, -2, 2), true),

            new Interactable(new Vec3f(-2, -3, 3), new Vec3f(-3, -2, 3), true),
            new Interactable(new Vec3f(-2, -3, 4), new Vec3f(-3, -2, 4), true),
            new Interactable(new Vec3f(-5, -3, 3), new Vec3f(-4, -2, 3), true),
            new Interactable(new Vec3f(-5, -3, 4), new Vec3f(-4, -2, 4), true)));

    private Tavern() {
    }

    public static String getPrefab(int tavernLevel, short stage) {
      Map<Short, String> stagePrefabs = PREFAB_MAP.getOrDefault(tavernLevel, PREFAB_MAP.get(1));
      return stagePrefabs == null ? null : stagePrefabs.get(stage);
    }

    public static String getCompletedPrefab(int tavernLevel) {
      return getPrefab(tavernLevel, (short) 3);
    }

    public static LevelLayout getLevelLayout(int tavernLevel) {
      return LEVEL_LAYOUTS.getOrDefault(tavernLevel, LEVEL_LAYOUTS.get(1));
    }

    public static List<Vec3i> getStorageOffsetForLevel(int level) {
      LevelLayout layout = getLevelLayout(level);
      List<Vec3i> offsets = new ArrayList<>();
      for (ContainerTransform container : layout.containers()) {
        offsets.add(container.position());
      }
      return offsets;
    }

    public static List<Interactable> getInteractables(Vec3i prefabAnchor,
        Rotation rotation, int level) {
      return toAbsoluteInteractables(INTERACTABLES.getOrDefault(level, INTERACTABLES.get(1)), prefabAnchor,
          rotation);
    }

    public static BlockTransform getWorkplaceSpawningOffset(int level) {
      return getLevelLayout(level).workplace();
    }
  }

  public static final class Market {
    public static final String PREFAB_JSON = "Structures/Workplace/Market/Level1/Stage3/Town_Hall.prefab.json";
  }

  public static final class TownHall {
    public record LevelLayout(
        BlockTransform workplace,
        BlockTransform constructionSign,
        Vec3f benchAccessOffset,
        Vec3f heartContainerAccessOffset) {
    }

    public static final String PREFAB_JSON = "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Town_Hall_Core");
    public static final Rotation WORKPLACE_BLOCK_YAW_OFFSET = Rotation.Ninety;
    public static final Vec3i WORKPLACE_BLOCK_OFFSET = new Vec3i(-3, 1, -1);
    public static final Vec3f BENCH_ACCESS_OFFSET = new Vec3f(1, 0, 1);
    public static final Vec3f CONTAINER_ACCESS_OFFSET = new Vec3f(0, 0, 1);
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/TownHall/Level1/Stage0/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage1/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage2/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json"),

        2,
        createStagePrefabMap(
            "Structures/Workplace/TownHall/Level2/Stage0/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level2/Stage1/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level2/Stage2/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level2/Stage3/Town_Hall.prefab.json"),

        3,
        createStagePrefabMap(
            // TODO: complete this
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json",
            "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json"));

    private static final Map<Integer, LevelLayout> LEVEL_LAYOUTS = Map.of(
        1,
        new LevelLayout(
            new BlockTransform(new Vec3i(-2, 1, 0), Rotation.None),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None),
            new Vec3f(1, 0, 0),
            new Vec3f(0, 0, 1)),

        2,
        new LevelLayout(
            new BlockTransform(new Vec3i(-4, 2, 0), Rotation.OneEighty),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None),
            new Vec3f(-1, 0, 0),
            new Vec3f(0, 0, 1)),

        3,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(-3, 1, -1), Rotation.Ninety),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None),
            new Vec3f(1, 0, 1),
            new Vec3f(0, 0, 1)));

    private TownHall() {
    }

    public static String getPrefab(int townHallLevel, short stage) {
      Map<Short, String> stagePrefabs = PREFAB_MAP.getOrDefault(townHallLevel, PREFAB_MAP.get(1));
      return stagePrefabs == null ? null : stagePrefabs.get(stage);
    }

    public static String getCompletedPrefab(int townHallLevel) {
      return getPrefab(townHallLevel, (short) 3);
    }

    public static LevelLayout getLevelLayout(int townHallLevel) {
      return LEVEL_LAYOUTS.getOrDefault(townHallLevel, LEVEL_LAYOUTS.get(1));
    }
  }

  public static final class Mine {
    public record LevelLayout(
        BlockTransform workplace,
        BlockTransform constructionSign,
        Vec3f benchAccessOffset,
        ContainerTransform container) {
    }

    public static final String PREFAB_JSON = "Structures/Workplace/Mine/Level1/Stage3/Mine.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Mine_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");

    public static final Vec3i WORKPLACE_BLOCK_OFFSET = new Vec3i(-5, 1, 6);
    public static final Rotation WORKPLACE_BLOCK_YAW_OFFSET = Rotation.Ninety;
    public static final Vec3f BENCH_ACCESS_OFFSET = new Vec3f(1, 0, 0);
    public static final Vec3i CONTAINER_OFFSET = new Vec3i(-5, 1, 5);

    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vec3i CONSTRUCTION_SIGN_OFFSET = new Vec3i(0, 1, 8);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;

    public static final Rotation CONTAINER_BLOCK_YAW_OFFSET = Rotation.Ninety;
    public static final Vec3f CONTAINER_ACCESS_OFFSET = new Vec3f(1, 0, 0);

    private static final Map<Vec3i, Vec3i> MINESHAFT_GALLERIES_OFFSETS = Map.of(
        Vec3i.NORTH, new Vec3i(0, 0, -8),
        Vec3i.SOUTH, new Vec3i(0, 0, 4),
        Vec3i.WEST, new Vec3i(-6, 0, -2),
        Vec3i.EAST, new Vec3i(6, 0, -2));

    private Mine() {
    }

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/Mine/Level1/Stage0/Mine.prefab.json",
            "Structures/Workplace/Mine/Level1/Stage1/Mine.prefab.json",
            "Structures/Workplace/Mine/Level1/Stage2/Mine.prefab.json",
            "Structures/Workplace/Mine/Level1/Stage3/Mine.prefab.json"),

        2,
        createStagePrefabMap(
            // TODO: complete this
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        3,
        createStagePrefabMap(
            // TODO: complete this
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON));

    private static final Map<Integer, LevelLayout> LEVEL_LAYOUTS = Map.of(
        1,
        new LevelLayout(
            new BlockTransform(new Vec3i(-5, 1, 6), Rotation.Ninety),
            new BlockTransform(new Vec3i(0, 1, 8), Rotation.None),
            new Vec3f(1, 0, 0),
            new ContainerTransform(new Vec3i(-5, 1, 5), Rotation.Ninety, new Vec3f(1, 0, 0))),

        2,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(-5, 1, 6), Rotation.Ninety),
            new BlockTransform(new Vec3i(0, 1, 8), Rotation.None),
            new Vec3f(1, 0, 0),
            new ContainerTransform(new Vec3i(-5, 1, 5), Rotation.Ninety, new Vec3f(1, 0, 0))),

        3,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(-5, 1, 6), Rotation.Ninety),
            new BlockTransform(new Vec3i(0, 1, 8), Rotation.None),
            new Vec3f(1, 0, 0),
            new ContainerTransform(new Vec3i(-5, 1, 5), Rotation.Ninety, new Vec3f(1, 0, 0))));

    public static String getPrefab(int mineLevel, short stage) {
      Map<Short, String> stagePrefabs = PREFAB_MAP.getOrDefault(mineLevel, PREFAB_MAP.get(1));
      return stagePrefabs == null ? null : stagePrefabs.get(stage);
    }

    public static String getCompletedPrefab(int mineLevel) {
      return getPrefab(mineLevel, (short) 3);
    }

    public static LevelLayout getLevelLayout(int mineLevel) {
      return LEVEL_LAYOUTS.getOrDefault(mineLevel, LEVEL_LAYOUTS.get(1));
    }

    public static Map<Vec3i, Vec3i> getMineshaftGalleriesOffset() {
      return MINESHAFT_GALLERIES_OFFSETS;
    }

    public static final class MineshaftLevelConf {
      public static final String PREFAB_JSON = "Structures/Workplace/Mine/Mineshaft/MineshaftLevel/Mineshaft_Level.prefab.json";
      public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
      public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

      public static Map<Vec3i, Vec3i> getMineshaftGalleriesWithPrefabOffset(
          Vec3i prefabPos,
          Rotation rotation) {
        Vec3i northDirection = StructurePlacementUtils.rotateOffset(Vec3i.NORTH, rotation);
        Vec3i westDirection = StructurePlacementUtils.rotateOffset(Vec3i.WEST, rotation);
        Vec3i eastDirection = StructurePlacementUtils.rotateOffset(Vec3i.EAST, rotation);
        Vec3i southDirection = StructurePlacementUtils.rotateOffset(Vec3i.SOUTH, rotation);

        return Map.of(
            northDirection,
            StructurePlacementUtils.withOffset(prefabPos,
                MINESHAFT_GALLERIES_OFFSETS.get(Vec3i.NORTH), rotation),

            westDirection,
            StructurePlacementUtils.withOffset(prefabPos,
                MINESHAFT_GALLERIES_OFFSETS.get(Vec3i.WEST), rotation),

            eastDirection,
            StructurePlacementUtils.withOffset(prefabPos,
                MINESHAFT_GALLERIES_OFFSETS.get(Vec3i.EAST), rotation),

            southDirection,
            StructurePlacementUtils.withOffset(prefabPos,
                MINESHAFT_GALLERIES_OFFSETS.get(Vec3i.SOUTH), rotation));
      }
    }

    public static final class MineshaftGalleryConf {
      public static final String PREFAB_JSON = "Structures/Workplace/Mine/Mineshaft/MineshaftGallery/Mineshaft_Gallery_Section.prefab.json";
      public static final String EMPTY_PREFAB_JSON = "Structures/Workplace/Mine/Mineshaft/MineshaftGallery/Mineshaft_Empty_Gallery_Section.prefab.json";
      public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
      public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    }
  }

  public static final class Sawmill {
    public record LevelLayout(
        BlockTransform workplace,
        BlockTransform constructionSign,
        Vec3f benchAccessOffset,
        ContainerTransform container,
        AreaFootprint workingAreaOffset) {
    }

    public static final String PREFAB_JSON = "Structures/Workplace/Sawmill/Level1/Stage3/Sawmill.prefab.json";
    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Sawmill_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");

    public static final Vec3i WORKPLACE_BLOCK_OFFSET = new Vec3i(5, 1, 5);
    public static final Rotation WORKPLACE_BLOCK_ROTATION = Rotation.Ninety;
    public static final Vec3f BENCH_ACCESS_OFFSET = new Vec3f(-1, 0, 1);

    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final Vec3i CONSTRUCTION_SIGN_OFFSET = new Vec3i(0, 1, 8);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;
    public static final Vec3i CONTAINER_OFFSET = new Vec3i(-4, 1, 6);
    public static final Rotation CONTAINER_ROTATION = Rotation.Ninety;
    public static final Vec3f CONTAINER_ACCESS_OFFSET = new Vec3f(1, 0, 0);

    public static final AreaFootprint WORKING_AREA_OFFSET = new AreaFootprint(
        new Vec2i(-4, -6),
        new Vec2i(4, 2));

    private Sawmill() {
    }

    private static final Map<Integer, Map<Short, String>> PREFAB_MAP = Map.of(
        1,
        createStagePrefabMap(
            "Structures/Workplace/Sawmill/Level1/Stage0/Sawmill.prefab.json",
            "Structures/Workplace/Sawmill/Level1/Stage1/Sawmill.prefab.json",
            "Structures/Workplace/Sawmill/Level1/Stage2/Sawmill.prefab.json",
            "Structures/Workplace/Sawmill/Level1/Stage3/Sawmill.prefab.json"),

        2,
        createStagePrefabMap(
            // TODO: complete this
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON),

        3,
        createStagePrefabMap(
            // TODO: complete this
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON,
            PREFAB_JSON));

    private static final Map<Integer, LevelLayout> LEVEL_LAYOUTS = Map.of(
        1,
        new LevelLayout(
            new BlockTransform(new Vec3i(5, 1, 5), Rotation.Ninety),
            new BlockTransform(new Vec3i(0, 1, 8), Rotation.None),
            new Vec3f(-1, 0, 1),
            new ContainerTransform(new Vec3i(-3, 1, 6), Rotation.Ninety, new Vec3f(1, 0, 0)),
            new AreaFootprint(new Vec2i(-4, -6), new Vec2i(4, 2))),

        2,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(5, 1, 5), Rotation.Ninety),
            new BlockTransform(new Vec3i(0, 1, 8), Rotation.None), new Vec3f(-1, 0, 1),
            new ContainerTransform(new Vec3i(-4, 1, 6), Rotation.Ninety, new Vec3f(1, 0, 0)),
            new AreaFootprint(new Vec2i(-4, -6), new Vec2i(4, 2))),

        3,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(5, 1, 5), Rotation.Ninety),
            new BlockTransform(new Vec3i(0, 1, 8), Rotation.None),
            new Vec3f(-1, 0, 1),
            new ContainerTransform(new Vec3i(-4, 1, 6), Rotation.Ninety, new Vec3f(1, 0, 0)),
            new AreaFootprint(new Vec2i(-4, -6), new Vec2i(4, 2))));

    public static String getPrefab(int sawmillLevel, short stage) {
      Map<Short, String> stagePrefabs = PREFAB_MAP.getOrDefault(sawmillLevel, PREFAB_MAP.get(1));
      return stagePrefabs == null ? null : stagePrefabs.get(stage);
    }

    public static String getCompletedPrefab(int sawmillLevel) {
      return getPrefab(sawmillLevel, (short) 3);
    }

    public static LevelLayout getLevelLayout(int sawmillLevel) {
      return LEVEL_LAYOUTS.getOrDefault(sawmillLevel, LEVEL_LAYOUTS.get(1));
    }
  }

  public static final class House {
    public record LevelLayout(
        BlockTransform coreBlock,
        BlockTransform constructionSign) {
    }

    public static final String PREFAB_JSON = "Structures/Housing/Level1/Stage0/House.prefab.json";
    private static final String LEVEL_3_PREFAB_JSON = "Structures/Housing/Level3/House.prefab.json";
    public static final BlockType CORE_BLOCK = BlockType.getAssetMap().getAsset("Housing_Core");
    public static final BlockType CRUDE_BED_BLOCK = BlockType.getAssetMap().getAsset("Village_Crude_Bed");
    public static final Vec3i CORE_BLOCK_OFFSET = new Vec3i(-4, 1, 2);
    public static final Rotation WORKPLACE_BLOCK_YAW_OFFSET = Rotation.Ninety;
    public static final Vec3i CONSTRUCTION_SIGN_OFFSET = new Vec3i(0, 1, 5);
    public static final Rotation CONSTRUCTION_SIGN_ROTATION = Rotation.None;
    private static final String SAMPLE_PREFAB = "Structures/Housing/Level1/Stage0/House.prefab.json";

    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SAMPLE_PREFAB);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SAMPLE_PREFAB);

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
            // TODO: complete this
            LEVEL_3_PREFAB_JSON,
            LEVEL_3_PREFAB_JSON,
            LEVEL_3_PREFAB_JSON,
            LEVEL_3_PREFAB_JSON));

    private static final Map<Integer, BlockType> BED_BLOCK = Map.of(
        1, BlockType.getAssetMap().getAsset("Village_Crude_Bed"),
        2, BlockType.getAssetMap().getAsset("Village_Bed_Level2"),
        3, BlockType.getAssetMap().getAsset("Village_Bed_Level2"));

    private static final Map<Integer, LevelLayout> LEVEL_LAYOUTS = Map.of(
        1,
        new LevelLayout(
            new BlockTransform(new Vec3i(-4, 1, 2), Rotation.Ninety),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None)),

        2,
        new LevelLayout(
            new BlockTransform(new Vec3i(2, 1, -1), Rotation.None),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None)),

        3,
        new LevelLayout(
            // TODO: complete this
            new BlockTransform(new Vec3i(2, 1, -1), Rotation.None),
            new BlockTransform(new Vec3i(0, 1, 5), Rotation.None)));

    private static final Map<Integer, List<List<BlockTransform>>> APPARTMENT_BED_OFFSETS = Map.of(
        1,
        List.of(
            List.of(new BlockTransform(new Vec3i(2, 1, -3), Rotation.None)),
            List.of(new BlockTransform(new Vec3i(4, 1, -3), Rotation.None))),

        2,
        List.of(
            List.of(new BlockTransform(new Vec3i(-2, 1, -1), Rotation.None)),
            List.of(new BlockTransform(new Vec3i(3, 5, 0), Rotation.TwoSeventy)),
            List.of(new BlockTransform(new Vec3i(3, 5, -2), Rotation.TwoSeventy))),

        3,
        List.of(
            // TODO: complete this
            List.of(new BlockTransform(new Vec3i(-2, 1, -1), Rotation.None)),
            List.of(new BlockTransform(new Vec3i(3, 5, 0), Rotation.TwoSeventy)),
            List.of(new BlockTransform(new Vec3i(3, 5, -2), Rotation.TwoSeventy))));

    public static String getPrefab(int houseLevel, short stage) {
      Map<Short, String> stagePrefabs = PREFAB_MAP.get(houseLevel);
      return stagePrefabs == null ? null : stagePrefabs.get(stage);
    }

    public static String getCompletedPrefab(int houseLevel) {
      return getPrefab(houseLevel, (short) 3);
    }

    public static LevelLayout getLevelLayout(int houseLevel) {
      return LEVEL_LAYOUTS.getOrDefault(houseLevel, LEVEL_LAYOUTS.get(1));
    }

    public static BlockTransform getCoreBlockOffset(int houseLevel) {
      return getLevelLayout(houseLevel).coreBlock();
    }

    public static BlockType getBedBlockPerLevel(int houseLevel) {
      return BED_BLOCK.get(houseLevel);
    }

    private House() {
    }

    public static HouseAppartmentRegistry getAppartments(Vec3i prefabAnchor,
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
          bed.setPosition(toAbsoluteBlockPosition(prefabAnchor, bedOffset.position,
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
      Vec3i prefabAnchor,
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
      copy.setInteractablePosition(toAbsolutePoint(
          prefabAnchor,
          interactable.getInteractablePosition(),
          rotation));

      if (interactable.getDependsOn() != null) {
        copy.setDependsOn(toAbsolutePoint(
            prefabAnchor,
            interactable.getDependsOn(),
            rotation));
      }

      result.add(copy);
    }

    return result;
  }

  private static Vec3i toAbsoluteBlockPosition(Vec3i prefabAnchor, Vec3i offset, Rotation rotation) {
    return StructurePlacementUtils.withOffset(prefabAnchor, offset, rotation);
  }

  private static Vec3f toAbsolutePoint(Vec3i prefabAnchor, Vec3f offset, Rotation rotation) {
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
