package com.pacificbytestudios.projectciviccore.village.components.heart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudio.codecbutler.annotations.SerialIgnore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.utils.TitleHologramUtil;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.DebugDrawUtils;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;

import lombok.Getter;
import lombok.Setter;

/**
 * Central registry for every waypoint visualization in a village. The debug
 * draw command
 * and the live editing session both publish their markers here so they can be
 * drawn
 * together without colliding: each structure keeps its own marker set, and the
 * structure
 * currently being edited is replaced by the live editing-session markers.
 */
@SerializableClass
public class VillageHeartDebugComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageHeartDebugComponent> CODEC = VillageHeartDebugComponentCodec.CODEC;

  UUID waypointEditingStructureId;
  @Getter
  @Setter
  boolean displayWorkerSnapshots;
  Map<MarkerType, List<Waypoint>> waypointEditinSessionMarkers = new HashMap<>();
  Map<UUID, Map<MarkerType, List<Waypoint>>> structureIdToMarkersMap = new HashMap<>();
  Map<String, Map<MarkerType, List<Waypoint>>> snapshotMarkersByOwner = new HashMap<>();

  // Holograms are entities (not debug shapes); recreating them every redraw makes
  // them
  // flicker, so they are tracked by content and only the delta is
  // spawned/despawned.
  @SerialIgnore
  Map<HologramKey, Ref<EntityStore>> snapshotHolograms = new HashMap<>();

  private record HologramKey(String text, double x, double y, double z) {
  }

  /**
   * Replaces the snapshot marker batch produced by one villager scanning one
   * structure.
   */
  public void setSnapshotWaypoints(UUID structureId, UUID villagerId, Map<MarkerType, List<Waypoint>> markers) {
    this.snapshotMarkersByOwner.put(structureId + "|" + villagerId, markers);
  }

  public void clearSnapshotWaypoints() {
    this.snapshotMarkersByOwner.clear();
  }

  /** Replaces the marker set for a single structure. */
  public void setStructureWaypoints(UUID structureId, Map<MarkerType, List<Waypoint>> markers) {
    this.structureIdToMarkersMap.put(structureId, markers);
  }

  public void clearStructureWaypoints() {
    this.structureIdToMarkersMap.clear();
  }

  public boolean hasStructureMarkers() {
    return !this.structureIdToMarkersMap.isEmpty();
  }

  /**
   * Publishes the live markers of an editing session. While set, the matching
   * structure's
   * static markers are skipped in favor of these.
   */
  public void setEditingSession(UUID structureId, Map<MarkerType, List<Waypoint>> markers) {
    this.waypointEditingStructureId = structureId;
    this.waypointEditinSessionMarkers = markers == null ? new HashMap<>() : markers;
  }

  public void clearEditingSession() {
    this.waypointEditingStructureId = null;
    this.waypointEditinSessionMarkers = new HashMap<>();
  }

  /** Clears the world and redraws every registered visualization in one pass. */
  public void drawAll(World world) {
    DrawUtils.clear(world);

    for (var structureEntry : this.structureIdToMarkersMap.entrySet()) {
      if (structureEntry.getKey().equals(this.waypointEditingStructureId)) {
        continue;
      }
      DebugDrawUtils.draw(world, structureEntry.getValue());
    }

    if (this.waypointEditingStructureId != null) {
      DebugDrawUtils.draw(world, this.waypointEditinSessionMarkers);
    }

    if (this.displayWorkerSnapshots) {
      for (var snapshotMarkers : this.snapshotMarkersByOwner.values()) {
        DebugDrawUtils.draw(world, snapshotMarkers);
      }
    }

    syncSnapshotHolograms(world);
  }

  /**
   * Reconciles the spawned title holograms with the snapshot markers that carry a
   * group name:
   * unchanged holograms are left in place (so they don't flicker), and only
   * removed or new
   * ones are despawned/spawned.
   */
  private void syncSnapshotHolograms(World world) {
    Set<HologramKey> desired = new HashSet<>();
    if (this.displayWorkerSnapshots) {
      for (var snapshotMarkers : this.snapshotMarkersByOwner.values()) {
        for (List<Waypoint> waypoints : snapshotMarkers.values()) {
          for (Waypoint waypoint : waypoints) {
            String group = waypoint.getMetadata("groupName");
            if (group == null) {
              continue;
            }
            Vec3d marker = waypoint.getWaypointPosition();
            desired.add(new HologramKey(group, marker.x, marker.y + 0.3, marker.z));
          }
        }
      }
    }

    Iterator<Map.Entry<HologramKey, Ref<EntityStore>>> spawned = this.snapshotHolograms.entrySet().iterator();
    while (spawned.hasNext()) {
      Map.Entry<HologramKey, Ref<EntityStore>> entry = spawned.next();
      if (!desired.contains(entry.getKey())) {
        TitleHologramUtil.remove(world, entry.getValue());
        spawned.remove();
      }
    }

    for (HologramKey key : desired) {
      if (this.snapshotHolograms.containsKey(key)) {
        continue;
      }
      Ref<EntityStore> hologram = TitleHologramUtil.spawn(world, new Vec3d(key.x(), key.y(), key.z()), key.text());
      if (hologram != null) {
        this.snapshotHolograms.put(key, hologram);
      }
    }
  }

  public static ComponentType<ChunkStore, VillageHeartDebugComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageHeartDebugComponentType();
  }

  @Override
  public VillageHeartDebugComponent clone() {
    return VillageHeartDebugComponentCodec.clone(this);
  }
}
