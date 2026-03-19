package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.house.AppartmentComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseAppartmentRegistry;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;

import io.sentry.util.Pair;

public class HouseSpawner {
  private static final String PREFAB_JSON = "Structures/Housing/Housing_Level_1.prefab.json";
  private static final BlockType CORE_BLOCK = BlockType.getAssetMap().getAsset("Housing_Core");
  private static final BlockType CRUDE_BED_BLOCK = BlockType.getAssetMap().getAsset("Village_Crude_Bed");

  public static boolean spawn(WorldChunk chunk, Vector3i position) {
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        PREFAB_JSON,
        position.clone(),
        Rotation.None,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    Vector3i houseCoreBlockPosition = getWorkplaceBlockPosition(position);
    WorldChunk houseChunk = Utils.setBlockChunkAware(chunk, houseCoreBlockPosition, CORE_BLOCK);
    Ref<ChunkStore> houseCoreRef = houseChunk.getBlockComponentEntity(
        houseCoreBlockPosition.x,
        houseCoreBlockPosition.y,
        houseCoreBlockPosition.z);

    if (houseCoreRef == null || !houseCoreRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[House - spawn] House ref is invalid");
      return rollbackFailedSpawn(world, houseChunk, trackedPlacement, houseCoreBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = houseCoreRef.getStore();

    HouseComponent house = chunkStore.getComponent(houseCoreRef, HouseComponent.getComponentType());
    if (house == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[House - spawn] Could not fetch house component");
      return rollbackFailedSpawn(world, houseChunk, trackedPlacement, houseCoreBlockPosition, null);
    }

    house.setLevel((short) 1);
    house.setUuid(UUID.randomUUID());
    house.setCoreBlockPosition(houseCoreBlockPosition);
    house.setPrefabAnchor(position.clone());
    house.setHouseRef(new EntityRef(chunk.getWorld().getName(), houseCoreBlockPosition));
    house.setAppartmentRegistry(Config.getAppartments(position, 1));

    spawnBeds(world, chunk, house);

    chunkStore.putComponent(
        houseCoreRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    houseChunk.markNeedsSaving();
    return true;
  }

  private static void spawnBeds(World world, WorldChunk chunk, HouseComponent house) {
    for (AppartmentComponent appartment : house.getAppartmentRegistry().getAppartments().values()) {
      for (VillageBedComponent bed : appartment.getBeds().values()) {
        Utils.setBlockChunkAware(chunk, bed.getPosition(), CRUDE_BED_BLOCK);
        Utils.markNeedSaving(world, bed.getPosition());
      }
    }
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    Vector3i bedBlockPosition = position.clone();
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        bedBlockPosition);

    WorldChunk bedChunk = result.getFirst();
    Ref<ChunkStore> bedRef = result.getSecond();

    if (bedRef == null || !bedRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[House - despawn] Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = bedRef.getStore();

    HouseComponent component = store.getComponent(bedRef, HouseComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(bedRef, TrackedPrefabPlacementComponent.getComponentType());

    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(bedChunk.getWorld(), trackedPlacement);
    } else {
      Vector3i prefabAnchor = component.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(bedBlockPosition);
      }

      despawn = PrefabManager.removePrefab(
          bedChunk.getWorld(),
          PREFAB_JSON,
          prefabAnchor,
          Rotation.None,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere().log("[House - despawn] Could not despawn farm prefab");
      return false;
    }

    Utils.breakBlockChunkAware(bedChunk, bedBlockPosition);
    return despawn;
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor) {
    return prefabAnchor.clone().add(Vector3i.NORTH.clone().scale(3)).add(Vector3i.UP);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition) {
    return workplaceBlockPosition.clone().add(Vector3i.SOUTH.clone().scale(4)).add(Vector3i.DOWN);
  }

  private static boolean rollbackFailedSpawn(
      World world,
      WorldChunk chunk,
      TrackedPrefabPlacementComponent trackedPlacement,
      Vector3i workplaceBlockPosition,
      Vector3i containerPosition) {
    Utils.breakBlockChunkAware(chunk, workplaceBlockPosition);
    if (containerPosition != null) {
      Utils.breakBlockChunkAware(chunk, containerPosition);
    }
    PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
    return false;
  }

  private class Config {
    private static final Map<Integer, List<List<Vector3i>>> APPARTMENT_BED_OFFSETS;

    static {
      APPARTMENT_BED_OFFSETS = new HashMap<>();

      List<List<Vector3i>> level1Appartments = new ArrayList<>();
      level1Appartments.add(List.of(new Vector3i(-2, 1, 0)));
      level1Appartments.add(List.of(new Vector3i(2, 1, 0)));

      APPARTMENT_BED_OFFSETS.put(1, level1Appartments);
    }

    public static HouseAppartmentRegistry getAppartments(Vector3i prefabAnchor, int level) {
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
          bed.setPosition(toAbsolutePosition(prefabAnchor, bedOffset));
          beds.put(UUID.randomUUID(), bed);
        }

        appartment.setBeds(beds);
        registry.registerAppartment(UUID.randomUUID(), appartment);
      }

      return registry;
    }

    private static Vector3i toAbsolutePosition(Vector3i prefabAnchor, Vector3i offset) {
      return prefabAnchor.clone().add(offset);
    }
  }
}
