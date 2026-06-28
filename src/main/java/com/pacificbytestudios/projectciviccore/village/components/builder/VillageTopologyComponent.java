package com.pacificbytestudios.projectciviccore.village.components.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialAfterDecode;
import com.pacificbytestudio.codecbutler.annotations.SerialIgnore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerialWithCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.RoadNode;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.Vector3iMapCodec;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;

import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;

@SerializableClass
public class VillageTopologyComponent implements Component<ChunkStore> {
  public static final Byte CELL_SIZE = 7;

  static final Vector3iMapCodec<UUID, HashMap<Vec3i, UUID>> ROADS_CODEC = new Vector3iMapCodec<>(
      Codec.UUID_BINARY,
      HashMap::new,
      false);

  public static final BuilderCodec<VillageTopologyComponent> CODEC = VillageTopologyComponentCodec.CODEC;

  @Getter
  Short villageRadius;

  @SerialName("RegisteredTopologyStructures")
  Map<UUID, TopologyStructure> structureIdToTopology = new HashMap<>();

  @SerialIgnore
  private Map<Pair<Byte, Byte>, Set<UUID>> villageGrid = new HashMap<>();

  @SerialName("Roads")
  @SerialWithCodec("VillageTopologyComponent.ROADS_CODEC")
  Map<Vec3i, UUID> roads = new HashMap<>();

  @SerialName("RoadLevelPerDepth")
  Map<Byte, List<UUID>> roadGroupPerDepth = new HashMap<>();

  @Getter
  @SerialName("RegisteredRoads")
  Map<UUID, RoadNode> roadIdToRoad = new HashMap<>();

  @Setter
  @SerialName("VillageOrientation")
  @SerialWithCodec("com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation.CODEC")
  Rotation villageOrientation;
  @Getter
  @Setter
  Long seed;
  @Getter
  @SerialName("HighestLevel")
  byte highestRoadLevel = -1;

  public VillageTopologyComponent() {
    this.seed = new FastRandom().nextLong();
  }

  public Rotation getVillageOrientation() {
    if (this.villageOrientation == null) {
      int size = Rotation.values().length;
      this.villageOrientation = Rotation.values()[new FastRandom(this.seed).nextInt(0, size)];
    }
    return villageOrientation;
  }

  public void addRoad(RoadNode node) {
    UUID roadId = node.getRoadId();
    if (node.getDepth() == 0 && node.getBranchOriginKey() != null) {
      this.roads.put(node.getBranchOriginKey().clone(), roadId);
    }
    addRoadIdToDepthGroup(roadId, node.getDepth());
    this.roadIdToRoad.put(roadId, node);
  }

  private void addRoadIdToDepthGroup(UUID roadId, byte level) {
    List<UUID> roadDepthLevels = this.roadGroupPerDepth.getOrDefault(level, new ArrayList<>());
    roadDepthLevels.add(roadId);
    if (this.highestRoadLevel < level) {
      this.highestRoadLevel = level;
    }
    this.roadGroupPerDepth.put(level, roadDepthLevels);
  }

  public boolean isStructureAlreadyPresent(UUID structureId) {
    return this.structureIdToTopology.containsKey(structureId);
  }

  public void initializeVillageTopology(TopologyStructure structure, Short radius) {
    this.villageRadius = radius;
    this.structureIdToTopology = new HashMap<>();
    if (structure == null) {
      rebuildVillageGrid();
      return;
    }

    this.structureIdToTopology.put(structure.getStructureId(), structure);
    rebuildVillageGrid();
  }

  public RoadNode getRoadFromId(UUID roadId) {
    return this.roadIdToRoad.get(roadId);
  }

  public UUID getRoadIdAtDirection(Vec3i directionKey) {
    return this.roads.get(directionKey);
  }

  public RoadNode getRootRoadAtDirection(Vec3i directionKey) {
    return getRoadFromId(getRoadIdAtDirection(directionKey));
  }

  public List<UUID> getRoadsIdsAtDepth(byte depth) {
    if (this.roadGroupPerDepth == null) {
      return Collections.emptyList();
    }

    List<UUID> roadIds = this.roadGroupPerDepth.get(depth);
    if (roadIds == null || roadIds.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(new ArrayList<>(roadIds));
  }

  public List<UUID> getRegisteredRoadIdsByDepthDescending() {
    if (this.roadGroupPerDepth == null || this.roadGroupPerDepth.isEmpty()) {
      return Collections.emptyList();
    }

    List<Byte> orderedDepths = new ArrayList<>(this.roadGroupPerDepth.keySet());
    orderedDepths.sort(Collections.reverseOrder());

    List<UUID> orderedRoadIds = new ArrayList<>();
    for (byte depth : orderedDepths) {
      orderedRoadIds.addAll(getRoadsIdsAtDepth(depth));
    }

    return Collections.unmodifiableList(orderedRoadIds);
  }

  public List<UUID> getRegisteredRoadIdsByDepthAscending() {
    if (this.roadGroupPerDepth == null || this.roadGroupPerDepth.isEmpty()) {
      return Collections.emptyList();
    }

    List<Byte> orderedDepths = new ArrayList<>(this.roadGroupPerDepth.keySet());
    orderedDepths.sort(Comparator.naturalOrder());

    List<UUID> orderedRoadIds = new ArrayList<>();
    for (byte depth : orderedDepths) {
      orderedRoadIds.addAll(getRoadsIdsAtDepth(depth));
    }

    return Collections.unmodifiableList(orderedRoadIds);
  }

  public Map<Vec3i, UUID> getRootRoadIds() {
    Map<Vec3i, UUID> copy = new HashMap<>();
    if (this.roads == null) {
      return Collections.unmodifiableMap(copy);
    }

    this.roads.forEach((direction, roadId) -> {
      if (direction != null && roadId != null) {
        copy.put(direction.clone(), roadId);
      }
    });
    return Collections.unmodifiableMap(copy);
  }

  public void registerStructure(TopologyStructure structure) {
    TopologyStructure clonedStructure = structure.clone();
    if (clonedStructure.getStructureId() == null) {
      throw new IllegalStateException("[VillageTopologyComponent] Found an empty structureId in: " + structure);
    }

    if (this.structureIdToTopology.containsKey(clonedStructure.getStructureId())) {
      return;
    }

    this.structureIdToTopology.put(clonedStructure.getStructureId(), clonedStructure);
    rebuildVillageGrid();
  }

  public Map<UUID, TopologyStructure> getRegisteredTopologyStructures() {
    return Collections.unmodifiableMap(this.structureIdToTopology);
  }

  public TopologyStructure getTopologyStructureFromStructureId(UUID structureId) {
    return this.structureIdToTopology.get(structureId);
  }

  public TopologyStructure getTopologyStructureAt(@Nonnull Vec3i absolutePosition) {
    Pair<Byte, Byte> cell = getCellPairFromAbsolutePosition(absolutePosition);
    if (cell == null) {
      return null;
    }

    Set<UUID> structureIds = getRegisteredStructureIdsAt(cell.left(), cell.right());
    if (structureIds.isEmpty()) {
      return null;
    }

    Vec2i position2D = new Vec2i(absolutePosition.x, absolutePosition.z);
    for (UUID structureId : structureIds) {
      TopologyStructure structure = getTopologyStructureFromStructureId(structureId);
      if (structure != null && structure.isInside(position2D)) {
        return structure;
      }
    }

    return null;
  }

  public Pair<Byte, Byte> getCellPairFromAbsolutePosition(Vec3i absolutePosition) {
    Vec2i villageCenter = getCoreCenter2D();
    int xCell = blockToCellIndex(absolutePosition.x - villageCenter.x);
    int zCell = blockToCellIndex(absolutePosition.z - villageCenter.y);
    return Pair.of((byte) xCell, (byte) zCell);
  }

  public Set<UUID> getRegisteredStructureIdsAt(byte xCell, byte zCell) {
    Pair<Byte, Byte> key = Pair.of(xCell, zCell);
    Set<UUID> structureIds = this.villageGrid.get(key);
    if (structureIds == null || structureIds.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(new HashSet<>(structureIds));
  }

  public Set<UUID> getRegisteredStructureIdsAt(TopologyStructure topologyStructure) {
    if (topologyStructure == null || this.villageGrid == null || this.villageGrid.isEmpty()) {
      return Collections.emptySet();
    }

    Vec2i topLeft = topologyStructure.getTopLeft();
    Vec2i botRight = topologyStructure.getBotRight();
    Vec2i villageCenter = getCoreCenter2D();

    if (topLeft == null || botRight == null || villageCenter == null) {
      return Collections.emptySet();
    }

    int relativeMinX = Math.min(topLeft.x, botRight.x) - villageCenter.x;
    int relativeMaxX = Math.max(topLeft.x, botRight.x) - villageCenter.x;
    int relativeMinZ = Math.min(topLeft.y, botRight.y) - villageCenter.y;
    int relativeMaxZ = Math.max(topLeft.y, botRight.y) - villageCenter.y;

    int minXCell = blockToCellIndex(relativeMinX);
    int maxXCell = blockToCellIndex(relativeMaxX);
    int minZCell = blockToCellIndex(relativeMinZ);
    int maxZCell = blockToCellIndex(relativeMaxZ);

    Set<UUID> structureIds = new HashSet<>();
    for (int xCell = minXCell; xCell <= maxXCell; xCell++) {
      for (int zCell = minZCell; zCell <= maxZCell; zCell++) {
        Set<UUID> cellStructureIds = this.villageGrid.get(Pair.of((byte) xCell, (byte) zCell));
        if (cellStructureIds != null && !cellStructureIds.isEmpty()) {
          structureIds.addAll(cellStructureIds);
        }
      }
    }

    if (structureIds.isEmpty()) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSet(structureIds);
  }

  private static int blockToCellIndex(int blockOffset) {
    return Math.floorDiv(blockOffset + (CELL_SIZE / 2), CELL_SIZE);
  }

  // villageGrid is @SerialIgnore (spatial index, not persisted); rebuild it from
  // the decoded structures so spatial lookups work right after deserialization.
  @SerialAfterDecode
  void rebuildVillageGrid() {
    this.villageGrid = new HashMap<>();
    Vec2i villageCenter = getCoreCenter2D();

    if (this.villageRadius != null) {
      int minCell = blockToCellIndex(-this.villageRadius);
      int maxCell = blockToCellIndex(this.villageRadius);

      for (int x = minCell; x <= maxCell; x++) {
        for (int z = minCell; z <= maxCell; z++) {
          Pair<Byte, Byte> key = Pair.of((byte) x, (byte) z);
          this.villageGrid.put(key, new HashSet<>());
        }
      }
    }

    if (villageCenter == null) {
      return;
    }

    for (TopologyStructure structure : this.structureIdToTopology.values()) {
      registerStructureOnGrid(structure, villageCenter);
    }
  }

  private void registerStructureOnGrid(TopologyStructure structure, Vec2i villageCenter) {
    if (structure == null || structure.getStructureId() == null || villageCenter == null) {
      return;
    }

    Vec2i topLeft = structure.getAxisAlignedTopLeft();
    Vec2i botRight = structure.getAxisAlignedBotRight();
    if (topLeft == null || botRight == null) {
      return;
    }

    Vec2i relativeTopLeft = new Vec2i(
        topLeft.x - villageCenter.x,
        topLeft.y - villageCenter.y);
    Vec2i relativeBotRight = new Vec2i(
        botRight.x - villageCenter.x,
        botRight.y - villageCenter.y);

    for (Map.Entry<Pair<Byte, Byte>, Set<UUID>> entry : this.villageGrid.entrySet()) {
      Pair<Byte, Byte> key = entry.getKey();
      if (key == null) {
        continue;
      }

      int xCell = key.left().intValue();
      int zCell = key.right().intValue();

      if (doesCellOverlapStructure(xCell, zCell, relativeTopLeft, relativeBotRight)) {
        entry.getValue().add(structure.getStructureId());
      }
    }
  }

  public void unregisterStructure(UUID structureId) {
    this.structureIdToTopology.remove(structureId);
    rebuildVillageGrid();
  }

  @Override
  public VillageTopologyComponent clone() {
    VillageTopologyComponent component = new VillageTopologyComponent();
    component.villageRadius = this.villageRadius;
    component.villageOrientation = this.villageOrientation;
    component.seed = this.seed;
    component.structureIdToTopology = copyTopologyStructures(this.structureIdToTopology);
    component.rebuildVillageGrid();

    if (this.roads == null) {
      component.roads = null;
    } else {
      component.roads = new HashMap<>();
      this.roads.forEach((position, roadId) -> component.roads.put(
          position.clone(),
          roadId));
    }

    component.roadGroupPerDepth = copyRoadLevelPerDepth(this.roadGroupPerDepth);
    component.roadIdToRoad = copyRegisteredRoads(this.roadIdToRoad);
    component.highestRoadLevel = this.highestRoadLevel;
    return component;
  }

  public static ComponentType<ChunkStore, VillageTopologyComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageTopologyComponentType();
  }

  private static Vec2i getStructureCenter2D(TopologyStructure structure) {
    return structure.getCenter2D();
  }

  private Vec2i getCoreCenter2D() {
    for (TopologyStructure structure : this.structureIdToTopology.values()) {
      if (structure == null || structure.getTopologyType() != VillageTopologyStructureType.CITY_CENTER) {
        continue;
      }

      return getStructureCenter2D(structure);
    }

    return null;
  }

  private static boolean doesCellOverlapStructure(
      int xCell,
      int zCell,
      Vec2i relativeTopLeft,
      Vec2i relativeBotRight) {
    int cellMinX = getCellMinBlock(xCell);
    int cellMaxX = getCellMaxBlock(xCell);
    int cellMinZ = getCellMinBlock(zCell);
    int cellMaxZ = getCellMaxBlock(zCell);
    int structureMinX = Math.min(relativeTopLeft.x, relativeBotRight.x);
    int structureMaxX = Math.max(relativeTopLeft.x, relativeBotRight.x);
    int structureMinZ = Math.min(relativeTopLeft.y, relativeBotRight.y);
    int structureMaxZ = Math.max(relativeTopLeft.y, relativeBotRight.y);

    return rangesOverlap(cellMinX, cellMaxX, structureMinX, structureMaxX) &&
        rangesOverlap(cellMinZ, cellMaxZ, structureMinZ, structureMaxZ);
  }

  private static boolean rangesOverlap(int firstMin, int firstMax, int secondMin, int secondMax) {
    return firstMin <= secondMax && secondMin <= firstMax;
  }

  private static int getCellMinBlock(int cellIndex) {
    return (cellIndex * CELL_SIZE) - (CELL_SIZE / 2);
  }

  private static int getCellMaxBlock(int cellIndex) {
    return getCellMinBlock(cellIndex) + CELL_SIZE - 1;
  }

  private static Map<UUID, TopologyStructure> copyTopologyStructures(Map<UUID, TopologyStructure> structures) {
    Map<UUID, TopologyStructure> copy = new HashMap<>();

    if (structures == null) {
      return copy;
    }

    structures.forEach((structureId, structure) -> {
      if (structureId != null && structure != null) {
        copy.put(structureId, structure.clone());
      }
    });

    return copy;
  }

  private static Map<UUID, RoadNode> copyRegisteredRoads(Map<UUID, RoadNode> roads) {
    Map<UUID, RoadNode> copy = new HashMap<>();

    if (roads == null) {
      return copy;
    }

    roads.forEach((roadId, road) -> {
      if (roadId != null && road != null) {
        RoadNode clonedRoad = road.clone();
        clonedRoad.setRoadId(roadId);
        copy.put(roadId, clonedRoad);
      }
    });

    return copy;
  }

  private static Map<Byte, List<UUID>> copyRoadLevelPerDepth(Map<Byte, List<UUID>> roadLevelPerDepth) {
    Map<Byte, List<UUID>> copy = new HashMap<>();

    if (roadLevelPerDepth == null) {
      return copy;
    }

    roadLevelPerDepth.forEach((depth, roadIds) -> {
      if (depth != null) {
        copy.put(depth, copyRoadIds(roadIds));
      }
    });
    return copy;
  }

  private static List<UUID> copyRoadIds(List<UUID> roadIds) {
    List<UUID> copy = new ArrayList<>();

    if (roadIds == null) {
      return copy;
    }

    for (UUID roadId : roadIds) {
      copy.add(roadId);
    }
    return copy;
  }
}
