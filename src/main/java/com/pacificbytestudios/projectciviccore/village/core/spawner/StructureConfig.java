package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
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

  private StructureConfig() {
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

  public static final class TownHeart {
    public static final String PREFAB_JSON = "Structures/General/CityCenter/Level1/City_Center.prefab.json";
    public static final BlockType MARKER_BLOCK = BlockType.getAssetMap().getAsset("Village_Heart_Marker");
    public static final BlockType HEART_BLOCK = BlockType.getAssetMap().getAsset("Village_Core");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);
    public static final VillageHeartConfigComponent DEFAULT_CONFIG;

    private static final int SPAWNING_SCALE = 6;
    private static final List<Vec3f> VILLAGER_SPAWNING_OFFSETS = List.of(
        Vec3f.NORTH.safeMul(SPAWNING_SCALE),
        Vec3f.SOUTH.safeMul(SPAWNING_SCALE),
        Vec3f.WEST.safeMul(SPAWNING_SCALE),
        Vec3f.EAST.safeMul(SPAWNING_SCALE));

    static {
      DEFAULT_CONFIG = HEART_BLOCK.getBlockEntity().getComponent(VillageHeartComponent.getComponentType()).getConfig();
    }

    private TownHeart() {
    }

    public static List<Vec3f> getVillagerSpawningOffsets() {
      return VILLAGER_SPAWNING_OFFSETS;
    }
  }

  public static final class Farm {
    private static final String SIZE_SAMPLE_PREFAB = "Structures/Workplace/Farm/Level1/Stage0/Farm.prefab.json";
    private static final AreaFootprint WORKING_AREA_OFFSET = new AreaFootprint(
        new Vec2i(-1, -4),
        new Vec2i(7, 4));

    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Farm_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SIZE_SAMPLE_PREFAB);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SIZE_SAMPLE_PREFAB);

    private Farm() {
    }

    public static AreaFootprint getWorkingAreaOffset(int farmLevel) {
      return WORKING_AREA_OFFSET;
    }
  }

  public static final class Storage {
    private static final String SIZE_SAMPLE_PREFAB = "Structures/Workplace/Storage/Level1/Stage3/Storage.prefab.json";

    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Storage_Core");
    public static final BlockType DOUBLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Large");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SIZE_SAMPLE_PREFAB);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SIZE_SAMPLE_PREFAB);

    private Storage() {
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

    private Road() {
    }

    public enum RoadType {
      MAIN_ROAD, SIDE_ROAD;
    }

    public enum RoadLength {
      SMALL((short) 1), MEDIUM((short) 2), LARGE((short) 4);

      private final short length;

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
    private static final String SIZE_SAMPLE_PREFAB = "Structures/Workplace/Tavern/Level1/Stage3/Tavern.prefab.json";

    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Tavern_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SIZE_SAMPLE_PREFAB);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SIZE_SAMPLE_PREFAB);

    private Tavern() {
    }
  }

  public static final class TownHall {
    private static final String SIZE_SAMPLE_PREFAB = "Structures/Workplace/TownHall/Level1/Stage3/Town_Hall.prefab.json";
    private static final Vec3f HEART_CONTAINER_ACCESS_OFFSET = new Vec3f(0, 0, 1);

    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Town_Hall_Core");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SIZE_SAMPLE_PREFAB);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SIZE_SAMPLE_PREFAB);

    private TownHall() {
    }

    public static Vec3f getHeartContainerAccessOffset(int townHallLevel) {
      return HEART_CONTAINER_ACCESS_OFFSET;
    }
  }

  public static final class Mine {
    private static final String SIZE_SAMPLE_PREFAB = "Structures/Workplace/Mine/Level1/Stage3/Mine.prefab.json";
    private static final Map<Vec3i, Vec3i> MINESHAFT_GALLERIES_OFFSETS = Map.of(
        Vec3i.NORTH, new Vec3i(0, 0, -8),
        Vec3i.SOUTH, new Vec3i(0, 0, 4),
        Vec3i.WEST, new Vec3i(-6, 0, -2),
        Vec3i.EAST, new Vec3i(6, 0, -2));

    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Mine_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SIZE_SAMPLE_PREFAB);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SIZE_SAMPLE_PREFAB);

    private Mine() {
    }

    public static Map<Vec3i, Vec3i> getMineshaftGalleriesOffset() {
      return MINESHAFT_GALLERIES_OFFSETS;
    }

    public static final class MineshaftLevelConf {
      public static final String PREFAB_JSON = "Structures/Workplace/Mine/Mineshaft/MineshaftLevel/Mineshaft_Level.prefab.json";
      public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(PREFAB_JSON);
      public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(PREFAB_JSON);

      private MineshaftLevelConf() {
      }

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

      private MineshaftGalleryConf() {
      }
    }
  }

  public static final class Sawmill {
    private static final String SIZE_SAMPLE_PREFAB = "Structures/Workplace/Sawmill/Level1/Stage3/Sawmill.prefab.json";
    private static final AreaFootprint WORKING_AREA_OFFSET = new AreaFootprint(
        new Vec2i(-4, -6),
        new Vec2i(4, 2));

    public static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Sawmill_Core");
    public static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SIZE_SAMPLE_PREFAB);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SIZE_SAMPLE_PREFAB);

    private Sawmill() {
    }

    public static AreaFootprint getWorkingAreaOffset(int sawmillLevel) {
      return WORKING_AREA_OFFSET;
    }
  }

  public static final class House {
    private static final String SIZE_SAMPLE_PREFAB = "Structures/Housing/Level1/Stage0/House.prefab.json";

    public static final BlockType CORE_BLOCK = BlockType.getAssetMap().getAsset("Housing_Core");
    public static final Vec2i SIZE_2I = PrefabManager.getPrefabWidthDepth(SIZE_SAMPLE_PREFAB);
    public static final Vec3i SIZE_3I = PrefabManager.getPrefabWidthHeightDepth(SIZE_SAMPLE_PREFAB);

    private static final Map<Integer, BlockType> BED_BLOCK = Map.of(
        1, BlockType.getAssetMap().getAsset("Village_Crude_Bed"),
        2, BlockType.getAssetMap().getAsset("Village_Bed_Level2"),
        3, BlockType.getAssetMap().getAsset("Village_Bed_Level2"));

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
            List.of(new BlockTransform(new Vec3i(-2, 1, -1), Rotation.None)),
            List.of(new BlockTransform(new Vec3i(3, 5, 0), Rotation.TwoSeventy)),
            List.of(new BlockTransform(new Vec3i(3, 5, -2), Rotation.TwoSeventy))));

    private House() {
    }

    public static BlockType getBedBlockPerLevel(int houseLevel) {
      return BED_BLOCK.get(houseLevel);
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
          bed.setPosition(toAbsoluteBlockPosition(prefabAnchor, bedOffset.position(),
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

  private static Vec3i toAbsoluteBlockPosition(Vec3i prefabAnchor, Vec3i offset, Rotation rotation) {
    return StructurePlacementUtils.withOffset(prefabAnchor, offset, rotation);
  }
}
