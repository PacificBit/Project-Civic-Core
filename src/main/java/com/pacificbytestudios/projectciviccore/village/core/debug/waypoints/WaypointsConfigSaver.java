package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.bson.Document;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.assetstore.AssetPack;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.StructureLevelConfig;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.structures.VillageStructuresConfig;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

/**
 * Writes the waypoints of an editing session back to the Village_Core item asset:
 * both the in-memory BlockType component (so an asset reload picks it up) and the
 * Village_Core.json source file of the mod resources.
 *
 * Config positions are stored relative to the structure prefab anchor, with the
 * structure orientation removed; this class performs the world-to-local conversion.
 */
public final class WaypointsConfigSaver {
  private static final String ASSET_NAME = "Village_Core";
  private static final String CONFIG_RELATIVE_PATH = "Server/Item/Items/VillageCore/Village_Core.json";

  public record SaveResult(boolean success, String message) {
  }

  private WaypointsConfigSaver() {
  }

  public static SaveResult save(
      World world,
      VillageHeartComponent village,
      TopologyStructure structure,
      WaypointsEditingComponent session) {
    StructureType type = structure.getStructureType();

    Short level = resolveLevel(village, structure);
    if (level == null) {
      return new SaveResult(false, "Could not resolve the structure level");
    }

    Rotation orientation = StructurePlacementUtils.normalize(structure.getOrientation());
    Vec3i anchor = resolveAnchor(session, structure);
    if (anchor == null) {
      return new SaveResult(false, "Could not resolve the structure anchor");
    }

    StructureLevelConfig assetConfig = getAssetLevelConfig(type);
    if (assetConfig == null) {
      return new SaveResult(false, "No " + ASSET_NAME + " config found for " + type);
    }

    // Build the local-space model from the session waypoints.
    BlockTransform<Vec3i> coreBlock = buildCoreBlock(session, assetConfig, level, anchor, orientation);
    BlockTransform<Vec3i>[] containers = buildContainers(session, anchor, orientation);
    Interactable[] interactables = buildInteractables(session, assetConfig, level, anchor, orientation);

    // Apply to the in-memory asset, then push it onto the village heart like
    // "/village asset reload" does, so the changes are active immediately.
    if (coreBlock != null) {
      assetConfig.putCoreBlock(level, coreBlock.clone());
    }
    assetConfig.putContainers(level, cloneTransforms(containers));
    if (interactables != null) {
      assetConfig.putInteractables(level, interactables);
    }
    applyConfigToVillage(world, village);

    // Persist to the JSON source file.
    Path file = findConfigFile();
    if (file == null) {
      return new SaveResult(false,
          "Applied in memory, but no writable " + CONFIG_RELATIVE_PATH + " was found");
    }

    try {
      Document root = Document.parse(Files.readString(file));
      Document levelConfig = navigateToLevelConfig(root, type);
      if (levelConfig == null) {
        return new SaveResult(false,
            "Applied in memory, but " + file.getFileName() + " has no section for " + type);
      }

      String levelKey = Short.toString(level);
      if (coreBlock != null) {
        getOrCreate(levelConfig, "CoreBlocks").put(levelKey, transformToJson(coreBlock));
      }
      if (containers.length > 0 || levelConfig.containsKey("Containers")) {
        List<Document> containerDocs = new ArrayList<>();
        for (BlockTransform<Vec3i> container : containers) {
          containerDocs.add(transformToJson(container));
        }
        getOrCreate(levelConfig, "Containers").put(levelKey, containerDocs);
      }
      if (interactables != null && (interactables.length > 0 || levelConfig.containsKey("Interactables"))) {
        List<Document> interactableDocs = new ArrayList<>();
        for (Interactable interactable : interactables) {
          interactableDocs.add(interactableToJson(interactable));
        }
        getOrCreate(levelConfig, "Interactables").put(levelKey, interactableDocs);
      }

      Files.writeString(file, toPrettyJson(root));
    } catch (Exception exception) {
      Logger.logSevere("Failed to write waypoint config to " + file + ": " + exception);
      return new SaveResult(false,
          "Applied in memory, but writing " + file.getFileName() + " failed: " + exception.getMessage());
    }

    return new SaveResult(true,
        type + " level " + level + " waypoints saved to " + file.getFileName());
  }

  // =========================
  // Local-space model
  // =========================

  private static Short resolveLevel(VillageHeartComponent village, TopologyStructure structure) {
    if (structure.getStructureType() == StructureType.HOUSE) {
      HouseComponent house = village.getHouseFromId(structure.getStructureId());
      return house == null ? null : house.getLevel();
    }

    WorkplaceComponent workplace = village.getWorkplaceFromId(structure.getStructureId());
    return workplace == null ? null : workplace.getLevel();
  }

  private static Vec3i resolveAnchor(WaypointsEditingComponent session, TopologyStructure structure) {
    Waypoint anchorWaypoint = firstWaypoint(session, MarkerType.ANCHOR);
    if (anchorWaypoint != null) {
      return anchorWaypoint.getWaypointPosition().to3i();
    }

    return structure.getPrefabAnchor();
  }

  private static BlockTransform<Vec3i> buildCoreBlock(
      WaypointsEditingComponent session,
      StructureLevelConfig assetConfig,
      short level,
      Vec3i anchor,
      Rotation orientation) {
    Waypoint bench = firstWaypoint(session, MarkerType.BENCH);
    if (bench == null) {
      return null;
    }

    BlockTransform<Vec3i> existing = assetConfig.getCoreBlockFor(level);
    BlockTransform<Vec3i> result = existing == null ? new BlockTransform<>() : BlockTransform.clone(existing);

    result.setPosition(toLocal(bench.getWaypointPosition().to3i(), anchor, orientation));

    BlockTransform<?> benchTransform = bench.getTransform();
    if (benchTransform.getRotation() != null) {
      result.setRotation(removeOrientation(benchTransform.getRotation(), orientation));
    }

    Waypoint access = firstWaypoint(session, MarkerType.BENCH_ACCESS);
    if (access != null) {
      result.setAccessOffset(toLocalOffset(
          access.getWaypointPosition(),
          bench.getWaypointPosition().to3i(),
          orientation));
      if (access.getAccessId() != null) {
        result.setAccessId(access.getAccessId());
      }
    }

    result.ensurePositionId();
    result.ensureAccessId();
    return result;
  }

  @SuppressWarnings("unchecked")
  private static BlockTransform<Vec3i>[] buildContainers(
      WaypointsEditingComponent session,
      Vec3i anchor,
      Rotation orientation) {
    List<BlockTransform<Vec3i>> containers = new ArrayList<>();

    for (Waypoint storage : waypointsOf(session, MarkerType.STORAGE)) {
      BlockTransform<Vec3i> container = new BlockTransform<>();
      Vec3i storageWorld = storage.getWaypointPosition().to3i();

      container.setPositionId(storage.getPositionId());
      container.setPosition(toLocal(storageWorld, anchor, orientation));

      BlockTransform<?> storageTransform = storage.getTransform();
      if (storageTransform.getRotation() != null) {
        container.setRotation(removeOrientation(storageTransform.getRotation(), orientation));
      }

      Waypoint access = findAccessFor(session, MarkerType.STORAGE_ACCESS, storage.getPositionId());
      if (access != null) {
        container.setAccessOffset(toLocalOffset(access.getWaypointPosition(), storageWorld, orientation));
        container.setAccessId(access.getAccessId());
      } else {
        container.setAccessOffset(Vec3f.safeZero());
      }

      container.ensurePositionId();
      container.ensureAccessId();
      containers.add(container);
    }

    return containers.toArray((BlockTransform<Vec3i>[]) new BlockTransform<?>[0]);
  }

  /**
   * Rebuilds the interactable list from the session chair/plate waypoints. Existing
   * entries are matched by the transform position id so their identity and flags are
   * preserved; unmatched session waypoints become new entries; missing ones are dropped.
   * Returns null when the structure has no interactable config at all.
   */
  private static Interactable[] buildInteractables(
      WaypointsEditingComponent session,
      StructureLevelConfig assetConfig,
      short level,
      Vec3i anchor,
      Rotation orientation) {
    List<Waypoint> chairs = waypointsOf(session, MarkerType.TAVERN_CHAIR);
    Interactable[] existing = assetConfig.getInteractablesFor(level);
    if (chairs.isEmpty() && existing.length == 0) {
      return null;
    }

    List<Waypoint> plates = new ArrayList<>(waypointsOf(session, MarkerType.TAVERN_PLATE));
    List<Interactable> result = new ArrayList<>();

    for (Waypoint chair : chairs) {
      Interactable template = findInteractableByPositionId(existing, chair.getPositionId());
      Interactable entry = template == null
          ? new Interactable(Vec3f.safeZero(), true)
          : template.clone();
      entry.setOccupiedBy(null);
      entry.setReady(false);

      BlockTransform<Vec3f> interactableTransform = entry.getInteractableTransform();
      interactableTransform.setPositionId(chair.getPositionId());
      interactableTransform.setPosition(toLocalFloat(chair.getWaypointPosition(), anchor, orientation));
      interactableTransform.ensurePositionId();
      interactableTransform.ensureAccessId();

      Waypoint plate = takeDependsOnPlate(plates, entry, chair);
      if (plate != null) {
        BlockTransform<Vec3f> dependsOn = entry.getDependsOnTransform() == null
            ? new BlockTransform<>(Vec3f.safeZero())
            : entry.getDependsOnTransform();
        dependsOn.setPositionId(plate.getPositionId());
        dependsOn.setPosition(toLocalFloat(plate.getWaypointPosition(), anchor, orientation));
        entry.setDependsOnTransform(dependsOn);
      }

      result.add(entry);
    }

    return result.toArray(Interactable[]::new);
  }

  /**
   * Picks the plate waypoint belonging to a chair: first by the previously linked
   * position id, then the nearest unclaimed plate.
   */
  private static Waypoint takeDependsOnPlate(List<Waypoint> plates, Interactable entry, Waypoint chair) {
    if (plates.isEmpty()) {
      return null;
    }

    UUID linkedId = entry.getDependsOnTransform() == null
        ? null
        : entry.getDependsOnTransform().getPositionId();

    Waypoint best = null;
    double bestDistance = Double.MAX_VALUE;
    for (Waypoint plate : plates) {
      if (linkedId != null && linkedId.equals(plate.getPositionId())) {
        best = plate;
        break;
      }

      double distance = plate.getWaypointPosition()
          .distanceSquared(chair.getWaypointPosition());
      if (distance < bestDistance) {
        bestDistance = distance;
        best = plate;
      }
    }

    if (best != null) {
      plates.remove(best);
    }
    return best;
  }

  // =========================
  // World to local conversion
  // =========================

  private static Vec3i toLocal(Vec3i world, Vec3i anchor, Rotation orientation) {
    Vec3i delta = new Vec3i(world.x - anchor.x, world.y - anchor.y, world.z - anchor.z);
    return StructurePlacementUtils.rotateOffset(delta, inverse(orientation));
  }

  // Float points use the block-center rotation pivot (see rotatePointOffset); applying
  // it with the inverse rotation is the exact inverse of the spawn-side conversion.
  private static Vec3f toLocalFloat(Vec3d world, Vec3i anchor, Rotation orientation) {
    Vec3f delta = new Vec3f(
        (float) (world.x - anchor.x),
        (float) (world.y - anchor.y),
        (float) (world.z - anchor.z));
    return StructurePlacementUtils.rotatePointOffset(delta, inverse(orientation));
  }

  private static Vec3f toLocalOffset(Vec3d accessWorld, Vec3i blockWorld, Rotation orientation) {
    Vec3f delta = new Vec3f(
        (float) (accessWorld.x - blockWorld.x),
        (float) (accessWorld.y - blockWorld.y),
        (float) (accessWorld.z - blockWorld.z));
    return StructurePlacementUtils.rotatePointOffset(delta, inverse(orientation));
  }

  private static Rotation removeOrientation(Rotation worldRotation, Rotation orientation) {
    return StructurePlacementUtils.addYaw(worldRotation, inverse(orientation));
  }

  private static Rotation inverse(Rotation rotation) {
    return switch (StructurePlacementUtils.normalize(rotation)) {
      case Ninety -> Rotation.TwoSeventy;
      case TwoSeventy -> Rotation.Ninety;
      case OneEighty -> Rotation.OneEighty;
      default -> Rotation.None;
    };
  }

  // =========================
  // Asset / file access
  // =========================

  /** Mirrors "/village asset reload": puts the asset config onto the village heart. */
  private static void applyConfigToVillage(World world, VillageHeartComponent village) {
    BlockType blockType = BlockType.getAssetMap().getAsset(ASSET_NAME);
    if (blockType == null || blockType.getBlockEntity() == null) {
      return;
    }

    VillageStructuresConfig config = blockType.getBlockEntity().getComponent(
        VillageStructuresConfig.getComponetType());
    if (config == null) {
      return;
    }

    Ref<ChunkStore> heartRef = village.getEntityRef() == null ? null : village.getEntityRef().getRef();
    if (heartRef == null || !heartRef.isValid()) {
      Logger.logSevere("Cannot apply waypoint config to the village: heart ref is invalid");
      return;
    }

    heartRef.getStore().putComponent(
        heartRef,
        VillageStructuresConfig.getComponetType(),
        config);
    Utils.markNeedSaving(world, village.getBlockContext().getPosition());
  }

  private static StructureLevelConfig getAssetLevelConfig(StructureType type) {
    BlockType blockType = BlockType.getAssetMap().getAsset(ASSET_NAME);
    if (blockType == null || blockType.getBlockEntity() == null) {
      return null;
    }

    VillageStructuresConfig config = blockType.getBlockEntity().getComponent(
        VillageStructuresConfig.getComponetType());
    if (config == null) {
      return null;
    }

    return config.getStructureLevelConfigsFor(type);
  }

  private static Path findConfigFile() {
    try {
      for (AssetPack pack : AssetModule.get().getAssetPacks()) {
        if (pack.isImmutable()) {
          continue;
        }

        try {
          Path candidate = pack.getRoot().resolve(CONFIG_RELATIVE_PATH);
          if (Files.exists(candidate) && Files.isWritable(candidate)) {
            return candidate;
          }
        } catch (Exception ignored) {
          // Zip-backed packs may not support resolution/writing; skip them.
        }
      }
    } catch (Exception exception) {
      Logger.logSevere("Failed to inspect asset packs: " + exception);
    }

    // Fallback for dev runs: walk up from the working directory to the mod sources.
    Path current = Path.of("").toAbsolutePath();
    for (int i = 0; i < 6 && current != null; i++) {
      Path candidate = current.resolve("src/main/resources/" + CONFIG_RELATIVE_PATH);
      if (Files.exists(candidate) && Files.isWritable(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }

    return null;
  }

  // =========================
  // JSON helpers
  // =========================

  private static Document navigateToLevelConfig(Document root, StructureType type) {
    Document node = root;
    for (String key : new String[] { "BlockType", "BlockEntity", "Components",
        "VillageStructuresConfig", "StructureTypeLevelConfig" }) {
      node = node == null ? null : node.get(key, Document.class);
    }
    if (node == null) {
      return null;
    }

    String normalizedType = type.name().replace("_", "");
    for (String key : node.keySet()) {
      if (key.replace("_", "").equalsIgnoreCase(normalizedType)) {
        return node.get(key, Document.class);
      }
    }

    return null;
  }

  private static Document getOrCreate(Document parent, String key) {
    Document child = parent.get(key, Document.class);
    if (child == null) {
      child = new Document();
      parent.put(key, child);
    }
    return child;
  }

  private static Document transformToJson(BlockTransform<Vec3i> transform) {
    Document document = new Document();
    document.put("PositionId", toBase64(transform.ensurePositionId()));
    document.put("Position", vectorToJson(transform.getPosition()));
    document.put("Rotation", transform.getRotation() == null ? "None" : transform.getRotation().name());
    document.put("AccessOffset", vectorToJson(transform.getAccessOffset()));
    document.put("AccessId", toBase64(transform.ensureAccessId()));
    return document;
  }

  private static Document interactableToJson(Interactable interactable) {
    Document document = new Document();
    document.put("Id", toBase64(interactable.getId() == null ? UUID.randomUUID() : interactable.getId()));

    BlockTransform<Vec3f> transform = interactable.getInteractableTransform();
    Document transformDoc = new Document();
    transformDoc.put("PositionId", toBase64(transform.ensurePositionId()));
    transformDoc.put("Position", vectorToJson(transform.getPosition()));
    transformDoc.put("AccessOffset", vectorToJson(transform.getAccessOffset()));
    transformDoc.put("AccessId", toBase64(transform.ensureAccessId()));
    document.put("InteractableTransform", transformDoc);

    BlockTransform<Vec3f> dependsOn = interactable.getDependsOnTransform();
    if (dependsOn != null && dependsOn.getPosition() != null) {
      Document dependsOnDoc = new Document();
      dependsOnDoc.put("PositionId", toBase64(dependsOn.ensurePositionId()));
      dependsOnDoc.put("Position", vectorToJson(dependsOn.getPosition()));
      document.put("DependsOnTransform", dependsOnDoc);
    }

    document.put("OccupiedBy", null);
    document.put("InvalidateOnUse", interactable.isInvalidateOnUse());
    document.put("IsReady", false);
    return document;
  }

  private static Document vectorToJson(Object vector) {
    Document document = new Document();
    if (vector instanceof Vec3i vec3i) {
      document.put("X", vec3i.x);
      document.put("Y", vec3i.y);
      document.put("Z", vec3i.z);
      return document;
    }
    if (vector instanceof Vec3f vec3f) {
      document.put("X", numberValue(vec3f.x));
      document.put("Y", numberValue(vec3f.y));
      document.put("Z", numberValue(vec3f.z));
      return document;
    }

    document.put("X", 0);
    document.put("Y", 0);
    document.put("Z", 0);
    return document;
  }

  /** Writes whole floats as integers to keep the JSON file diff-friendly. */
  private static Object numberValue(float value) {
    if (value == Math.floor(value) && !Float.isInfinite(value)) {
      return (int) value;
    }
    return (double) value;
  }

  private static String toBase64(UUID uuid) {
    ByteBuffer buffer = ByteBuffer.allocate(16);
    buffer.putLong(uuid.getMostSignificantBits());
    buffer.putLong(uuid.getLeastSignificantBits());
    return Base64.getEncoder().encodeToString(buffer.array());
  }

  private static String toPrettyJson(Document root) {
    return root.toJson(org.bson.json.JsonWriterSettings.builder()
        .indent(true)
        .outputMode(org.bson.json.JsonMode.RELAXED)
        .build());
  }

  // =========================
  // Session lookups
  // =========================

  private static Waypoint firstWaypoint(WaypointsEditingComponent session, MarkerType type) {
    List<Waypoint> waypoints = waypointsOf(session, type);
    return waypoints.isEmpty() ? null : waypoints.get(0);
  }

  private static List<Waypoint> waypointsOf(WaypointsEditingComponent session, MarkerType type) {
    List<Waypoint> result = new ArrayList<>();
    Waypoint[] waypoints = session.getStructureWaypoints().get(type);
    if (waypoints == null) {
      return result;
    }

    for (Waypoint waypoint : waypoints) {
      if (waypoint != null) {
        result.add(waypoint);
      }
    }
    return result;
  }

  private static Waypoint findAccessFor(
      WaypointsEditingComponent session,
      MarkerType accessType,
      UUID parentPositionId) {
    if (parentPositionId == null) {
      return null;
    }

    for (Waypoint waypoint : waypointsOf(session, accessType)) {
      if (parentPositionId.equals(waypoint.getPositionId())) {
        return waypoint;
      }
    }
    return null;
  }

  private static Interactable findInteractableByPositionId(Interactable[] interactables, UUID positionId) {
    if (positionId == null) {
      return null;
    }

    for (Interactable interactable : interactables) {
      if (interactable == null || interactable.getInteractableTransform() == null) {
        continue;
      }
      if (positionId.equals(interactable.getInteractableTransform().getPositionId())) {
        return interactable;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static BlockTransform<Vec3i>[] cloneTransforms(BlockTransform<Vec3i>[] transforms) {
    BlockTransform<Vec3i>[] clones = (BlockTransform<Vec3i>[]) new BlockTransform<?>[transforms.length];
    for (int i = 0; i < transforms.length; i++) {
      clones[i] = BlockTransform.clone(transforms[i]);
    }
    return clones;
  }
}
