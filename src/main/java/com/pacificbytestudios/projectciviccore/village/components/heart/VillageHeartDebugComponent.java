package com.pacificbytestudios.projectciviccore.village.components.heart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.DebugDrawUtils;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;

/**
 * Central registry for every waypoint visualization in a village. The debug draw command
 * and the live editing session both publish their markers here so they can be drawn
 * together without colliding: each structure keeps its own marker set, and the structure
 * currently being edited is replaced by the live editing-session markers.
 */
@SerializableClass
public class VillageHeartDebugComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageHeartDebugComponent> CODEC = VillageHeartDebugComponentCodec.CODEC;

  UUID waypointEditingStructureId;
  UUID waypointEditorPlayerId;
  Map<MarkerType, List<Waypoint>> waypointEditinSessionMarkers = new HashMap<>();
  Map<UUID, Map<MarkerType, List<Waypoint>>> structureIdToMarkersMap = new HashMap<>();

  public void addStructureWaypoint(UUID structureId, MarkerType type, Waypoint waypoint) {
    this.structureIdToMarkersMap
        .computeIfAbsent(structureId, ignored -> new HashMap<>())
        .computeIfAbsent(type, ignored -> new ArrayList<>())
        .add(waypoint);
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
   * Publishes the live markers of an editing session. While set, the matching structure's
   * static markers are skipped in favor of these.
   */
  public void setEditingSession(
      UUID structureId,
      UUID playerId,
      Map<MarkerType, List<Waypoint>> markers) {
    this.waypointEditingStructureId = structureId;
    this.waypointEditorPlayerId = playerId;
    this.waypointEditinSessionMarkers = markers == null ? new HashMap<>() : markers;
  }

  public void clearEditingSession() {
    this.waypointEditingStructureId = null;
    this.waypointEditorPlayerId = null;
    this.waypointEditinSessionMarkers = new HashMap<>();
  }

  public UUID getWaypointEditingStructureId() {
    return this.waypointEditingStructureId;
  }

  public UUID getWaypointEditorPlayerId() {
    return this.waypointEditorPlayerId;
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
  }

  public static ComponentType<ChunkStore, VillageHeartDebugComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageHeartDebugComponentType();
  }

  @Override
  public VillageHeartDebugComponent clone() {
    return VillageHeartDebugComponentCodec.clone(this);
  }
}
