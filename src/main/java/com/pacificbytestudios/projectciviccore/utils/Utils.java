package com.pacificbytestudios.projectciviccore.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.BlockContext;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;
import com.pacificbytestudios.projectciviccore.utils.SpawnerFunctions.AdvanceConstructionStage;
import com.pacificbytestudios.projectciviccore.utils.SpawnerFunctions.BeginNewConstruction;
import com.pacificbytestudios.projectciviccore.utils.SpawnerFunctions.BeginUpgradeConstruction;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.FarmSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.HouseSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.MineSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.SawmillSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StorageSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.village.core.spawner.TavernSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.TownHallSpawner;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;

import io.sentry.util.Pair;

public class Utils {

  public static BlockContext getWorldBlockContext(
      Ref<ChunkStore> ref,
      CommandBuffer<ChunkStore> commandBuffer) {
    BlockModule.BlockStateInfo info = commandBuffer.getComponent(
        ref,
        BlockModule.BlockStateInfo.getComponentType());

    if (info == null) {
      return null;
    }

    WorldChunk worldChunk = commandBuffer.getComponent(
        info.getChunkRef(),
        WorldChunk.getComponentType());

    return buildWorldBlockContext(info, worldChunk);
  }

  public static WorldChunk getWorldChunk(BlockContext context) {
    if (context == null || context.getWorld() == null || context.getChunkIndex() == null) {
      return null;
    }

    return context.getWorld().getChunk(context.getChunkIndex());
  }

  public static TriggerVolumeManager getTriggerVolumeManager(World world) {
    TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
    Store<EntityStore> store = world.getEntityStore().getStore();
    TriggerVolumeManager manager = store.getResource(plugin.getManagerResourceType());
    return manager;
  }

  public static BlockContext getWorldBlockContext(
      Ref<ChunkStore> ref,
      Store<ChunkStore> chunkStore) {
    BlockModule.BlockStateInfo info = chunkStore.getComponent(
        ref,
        BlockModule.BlockStateInfo.getComponentType());

    if (info == null) {
      return null;
    }

    WorldChunk worldChunk = chunkStore.getComponent(
        info.getChunkRef(),
        WorldChunk.getComponentType());

    return buildWorldBlockContext(info, worldChunk);
  }

  private static BlockContext buildWorldBlockContext(
      BlockModule.BlockStateInfo info,
      WorldChunk worldChunk) {
    if (worldChunk == null) {
      return null;
    }

    int localX = ChunkUtil.xFromBlockInColumn(info.getIndex());
    int localY = ChunkUtil.yFromBlockInColumn(info.getIndex());
    int localZ = ChunkUtil.zFromBlockInColumn(info.getIndex());

    int worldX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getX(), localX);
    int worldY = localY;
    int worldZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getZ(), localZ);

    return new BlockContext(
        worldChunk.getWorld().getName(),
        worldChunk.getIndex(),
        new Vec3i(worldX, worldY, worldZ));
  }

  public static AreaFootprint calculateAreaFootprint(Vec3i origin, int radius) {
    Vec2i topLeft = new Vec2i(origin.x - radius, origin.z - radius);
    Vec2i botRight = new Vec2i(origin.x + radius, origin.z + radius);
    return new AreaFootprint(topLeft, botRight);
  }

  public static AreaFootprint claimVillageArea(VillageHeartComponent heart, Vec3i origin) {
    short level = (short) heart.getLevel();
    int radius = heart.getConfig().getClaimRadiusPerLevel(level);
    AreaFootprint area = calculateAreaFootprint(origin, radius);
    heart.setClaimedArea(area);
    return area;
  }

  public static BeginNewConstruction getBeginNewConstructionFunction(StructureType structureType) {
    return switch (structureType) {
      case TOWN_HALL -> TownHallSpawner::beginNewConstruction;
      case MINE -> MineSpawner::beginNewConstruction;
      case FARM -> FarmSpawner::beginNewConstruction;
      case TAVERN -> TavernSpawner::beginNewConstruction;
      case SAWMILL -> SawmillSpawner::beginNewConstruction;
      case STORAGE -> StorageSpawner::beginNewConstruction;
      case HOUSE -> HouseSpawner::beginNewConstruction;
      default -> throw new IllegalArgumentException("Unsupported structureType: "
          + structureType);
    };
  }

  public static BeginUpgradeConstruction getBeginUpgradeConstructionFunction(StructureType structureType) {
    return switch (structureType) {
      case TOWN_HALL -> TownHallSpawner::beginUpgradeConstruction;
      case MINE -> MineSpawner::beginUpgradeConstruction;
      case FARM -> FarmSpawner::beginUpgradeConstruction;
      case TAVERN -> TavernSpawner::beginUpgradeConstruction;
      case SAWMILL -> SawmillSpawner::beginUpgradeConstruction;
      case STORAGE -> StorageSpawner::beginUpgradeConstruction;
      case HOUSE -> HouseSpawner::beginUpgradeConstruction;
      default -> throw new IllegalArgumentException("Unsupported structureType: "
          + structureType);
    };
  }

  public static AdvanceConstructionStage getAdvanceConstructionStageFunction(StructureType structureType) {
    return switch (structureType) {
      case TOWN_HALL -> TownHallSpawner::advanceConstruction;
      case MINE -> MineSpawner::advanceConstruction;
      case FARM -> FarmSpawner::advanceConstruction;
      case TAVERN -> TavernSpawner::advanceConstruction;
      case SAWMILL -> SawmillSpawner::advanceConstruction;
      case STORAGE -> StorageSpawner::advanceConstruction;
      case HOUSE -> HouseSpawner::advanceConstruction;
      default -> throw new IllegalArgumentException("Unsupported structureType: "
          + structureType);
    };
  }

  public static DespawnFunction getDespawnFunction(StructureType structureType) {
    return switch (structureType) {
      case TOWN_HALL -> TownHallSpawner::despawn;
      case MINE -> MineSpawner::despawn;
      case FARM -> FarmSpawner::despawn;
      case TAVERN -> TavernSpawner::despawn;
      case SAWMILL -> SawmillSpawner::despawn;
      case STORAGE -> StorageSpawner::despawn;
      case HOUSE -> HouseSpawner::despawn;
      default -> throw new IllegalArgumentException("Unsupported structureType: "
          + structureType);
    };
  }

  public static DespawnIncompleteConstructionFunction getDespawnIncompleteConstructionFunction(
      StructureType structureType) {
    return switch (structureType) {
      case TOWN_HALL -> TownHallSpawner::despawnIncompleteBuilding;
      case MINE -> MineSpawner::despawnIncompleteBuilding;
      case FARM -> FarmSpawner::despawnIncompleteBuilding;
      case TAVERN -> TavernSpawner::despawnIncompleteBuilding;
      case SAWMILL -> SawmillSpawner::despawnIncompleteBuilding;
      case STORAGE -> StorageSpawner::despawnIncompleteBuilding;
      case HOUSE -> HouseSpawner::despawnIncompleteBuilding;
      default -> throw new IllegalArgumentException("Unsupported structureType: "
          + structureType);
    };
  }

  public static Vec2i getSize2i(StructureType structureType) {
    return switch (structureType) {
      case TOWN_HALL -> StructureConfig.TownHall.SIZE_2I;
      case MINE -> StructureConfig.Mine.SIZE_2I;
      case FARM -> StructureConfig.Farm.SIZE_2I;
      case TAVERN -> StructureConfig.Tavern.SIZE_2I;
      case SAWMILL -> StructureConfig.Sawmill.SIZE_2I;
      case STORAGE -> StructureConfig.Storage.SIZE_2I;
      case HOUSE -> StructureConfig.House.SIZE_2I;
      default -> throw new IllegalArgumentException(
          "Unsupported structure type: " + structureType);
    };
  }

  public static Vec2i getVillageHeartSize2i() {
    return StructureConfig.TownHeart.SIZE_2I;
  }

  public static Vec3i getSize3i(StructureType structureType) {
    return switch (structureType) {
      case TOWN_HALL -> StructureConfig.TownHall.SIZE_3I;
      case MINE -> StructureConfig.Mine.SIZE_3I;
      case FARM -> StructureConfig.Farm.SIZE_3I;
      case TAVERN -> StructureConfig.Tavern.SIZE_3I;
      case SAWMILL -> StructureConfig.Sawmill.SIZE_3I;
      case STORAGE -> StructureConfig.Storage.SIZE_3I;
      case HOUSE -> StructureConfig.House.SIZE_3I;
      default -> throw new IllegalArgumentException(
          "Unsupported structure type: " + structureType);
    };
  }

  public static Vec3i getVillageHeartSize3i() {
    return StructureConfig.TownHeart.SIZE_3I;
  }

  public static WorldChunk setBlockChunkAware(WorldChunk chunk, Vec3i position, BlockType blockType) {
    WorldChunk targetChunk = ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(),
        position.x, position.z)
            ? chunk
            : chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x,
                position.z));

    targetChunk.setBlock(position.x, position.y, position.z, blockType);
    return targetChunk;
  }

  public static WorldChunk setBlockChunkAware(WorldChunk chunk, Vec3i position, BlockType blockType, int rotation) {
    int blockId = BlockType.getAssetMap().getIndex(blockType.getId());

    WorldChunk targetChunk = ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(),
        position.x, position.z)
            ? chunk
            : chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x,
                position.z));

    targetChunk.setBlock(position.x, position.y, position.z, blockId, blockType,
        rotation, FillerBlockUtil.NO_FILLER, 0);

    return targetChunk;
  }

  public static WorldChunk breakBlockChunkAware(WorldChunk chunk, Vec3i position) {
    if (ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x,
        position.z)) {
      chunk.breakBlock(position.x, position.y, position.z);
      return chunk;
    }

    WorldChunk correctChunk = chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x,
        position.z));
    correctChunk.breakBlock(position.x, position.y, position.z);
    return correctChunk;
  }

  public static Pair<WorldChunk, Ref<ChunkStore>> getBlockComponentEntityChunkAware(WorldChunk chunk,
      Vec3i position) {
    if (ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x,
        position.z)) {
      return new Pair<>(chunk, chunk.getBlockComponentEntity(position.x,
          position.y, position.z));
    }

    WorldChunk correctChunk = chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x,
        position.z));
    return new Pair<>(correctChunk,
        correctChunk.getBlockComponentEntity(position.x, position.y, position.z));
  }

  public static void safeVillageTaskEnqueue(Ref<ChunkStore> villageRef,
      VillageTaskQueueComponent task) {
    Store<ChunkStore> chunkStore = villageRef.getStore();
    VillageTaskQueueComponent taskQueue = chunkStore.getComponent(villageRef,
        VillageTaskQueueComponent.getComponentType());

    if (taskQueue == null) {
      chunkStore.putComponent(
          villageRef,
          VillageTaskQueueComponent.getComponentType(),
          task);
      return;
    }

    taskQueue.merge(task);
  }

  public static void markNeedSaving(World world, Vec3i position) {
    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(position.x,
        position.z));
    chunk.markNeedsSaving();
  }

  public static AreaFootprint[] subdivideArea(AreaFootprint area,
      int parts) {
    if (parts <= 0) {
      throw new IllegalArgumentException("parts must be > 0");
    }

    if (parts == 1) {
      return new AreaFootprint[] { area };
    }

    int minX = area.getMinX();
    int maxX = area.getMaxX();
    int minY = area.getMinZ();
    int maxY = area.getMaxZ();

    int width = maxX - minX + 1;
    int height = maxY - minY + 1;

    boolean splitHorizontally = width >= height;
    int totalSize = splitHorizontally ? width : height;

    int baseSize = totalSize / parts;
    int remainder = totalSize % parts;
    int current = splitHorizontally ? minX : minY;

    AreaFootprint[] result = new AreaFootprint[parts];

    for (int i = 0; i < parts; i++) {
      int currentSize = baseSize + (i < remainder ? 1 : 0);
      int start = current;
      int end = start + currentSize - 1;

      result[i] = splitHorizontally
          ? new AreaFootprint(
              new Vec2i(start, minY),
              new Vec2i(end, maxY))
          : new AreaFootprint(
              new Vec2i(minX, start),
              new Vec2i(maxX, end));

      current = end + 1;
    }

    return result;
  }

  public static String safeToString(Object value) {
    return value == null ? "null" : value.toString();
  }

  public static Vec3f centerOf(@Nonnull Vec3i blockPos) {
    return new Vec3f(
        blockPos.x + 0.5f,
        blockPos.y + 0.5f,
        blockPos.z + 0.5f);
  }

  public static Vec3i getRotationVector(Rotation rotation) {
    return Vec3i.from(rotation.getAxisDirection());
  }

  public static Rotation getRotationFromCardinalDirection(Vec3i cardinalDirection) {
    for (Rotation rotation : Rotation.values()) {
      Vec3i rotationVector = getRotationVector(rotation);
      if (rotationVector.x == cardinalDirection.x &&
          rotationVector.y == cardinalDirection.y &&
          rotationVector.z == cardinalDirection.z) {
        return rotation;
      }
    }
    return null;
  }

  public static Vec3i rotateNormalizedVector(Vec3i cardinalDirection, Rotation rotation) {
    return getRotationVector(getRotationFromCardinalDirection(cardinalDirection).add(rotation));
  }

  public static Vec3i getCenter(Vec3i start, Vec3i end) {
    if (start == null || end == null) {
      return null;
    }

    return new Vec3i(
        (int) Math.floorDiv((long) start.x + end.x, 2L),
        (int) Math.floorDiv((long) start.y + end.y, 2L),
        (int) Math.floorDiv((long) start.z + end.z, 2L));
  }

  public static WorldChunk getWorldChunk(World world, Vec3i position) {
    return world.getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));
  }

  public static WorldChunk getWorldChunk(World world, WorldChunk chunk, Vec3i position) {
    if (ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x, position.z)) {
      return chunk;
    }
    return getWorldChunk(world, position);
  }

  public static BlockType getBlockTypeChunkAware(World world, Vec3i position) {
    WorldChunk chunk = getWorldChunk(world, position);
    return chunk.getBlockType(position);
  }

  public static BlockType getBlockTypeChunkAware(World world, WorldChunk chunk, Vec3i position) {
    if (ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x, position.z)) {
      return chunk.getBlockType(position);
    }
    WorldChunk correctChunk = getWorldChunk(world, position);
    return correctChunk.getBlockType(position);
  }

  public static MayorComponent getMayorComponent(World world, VillageHeartComponent heart) {
    UUID villagerId = heart.getTownHall().getBossId();
    if (villagerId == null) {
      return null;
    }

    Ref<EntityStore> villagerRef = world.getEntityRef(villagerId);
    if (villagerRef == null || !villagerRef.isValid()) {
      return null;
    }

    return villagerRef.getStore().getComponent(
        villagerRef,
        MayorComponent.getComponentType());
  }

  @Deprecated(forRemoval = true)
  public static <S, T extends Copyable<T>> Map<S, T> copyMap(Map<S, T> source) {
    Map<S, T> copy = new HashMap<>();

    if (source == null) {
      return copy;
    }

    source.forEach((key, value) -> copy.put(key, value == null ? null : value.clone()));
    return copy;
  }

  @Deprecated(forRemoval = true)
  public static <S, T extends Copyable<T>> Map<S, T[]> copyMapArray(Map<S, T[]> source) {
    Map<S, T[]> copy = new HashMap<>();

    if (source == null) {
      return copy;
    }

    source.forEach((key, values) -> {
      if (values == null) {
        copy.put(key, null);
        return;
      }

      T[] clonedValues = Arrays.copyOf(values, values.length);
      for (int i = 0; i < values.length; i++) {
        clonedValues[i] = values[i] == null ? null : values[i].clone();
      }

      copy.put(key, clonedValues);
    });
    return copy;
  }
}
