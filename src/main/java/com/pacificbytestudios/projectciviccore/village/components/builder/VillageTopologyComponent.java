package com.pacificbytestudios.projectciviccore.village.components.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.map.Short2ObjectMapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.RoadsGraph;
import com.pacificbytestudios.projectciviccore.codecs.RoadsGraph.RoadNode;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.Vector3iMapCodec;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class VillageTopologyComponent implements Component<ChunkStore> {
  public static final Byte CELL_SIZE = 7;
  public static final BuilderCodec<VillageTopologyComponent> CODEC = BuilderCodec
      .builder(VillageTopologyComponent.class, VillageTopologyComponent::new)

      .append(new KeyedCodec<>("VillageRadius", Codec.SHORT, false),
          (obj, value) -> obj.villageRadius = value,
          obj -> obj.villageRadius)
      .add()

      .append(new KeyedCodec<>("VillageOrientation", Rotation.CODEC, false),
          (obj, value) -> obj.villageOrientation = value,
          obj -> obj.villageOrientation)
      .add()

      .append(new KeyedCodec<>("Seed", Codec.LONG, false),
          (obj, value) -> obj.seed = value,
          obj -> obj.seed)
      .add()

      .append(new KeyedCodec<>(
          "RegisteredTopologyStructures",
          new MapCodec<TopologyStructure, HashMap<String, TopologyStructure>>(
              TopologyStructure.CODEC,
              HashMap::new),
          false),
          (obj, value) -> obj.setRegisteredTopologyStructures(decodeTopologyStructures(value)),
          obj -> encodeTopologyStructures(obj.structureIdToTopology))
      .add()

      .append(new KeyedCodec<>("Roads",
          new Vector3iMapCodec<RoadsGraph, HashMap<Vector3i, RoadsGraph>>(
              RoadsGraph.CODEC,
              HashMap::new,
              false)),
          (obj, value) -> obj.roads = value,
          (obj) -> obj.roads)
      .add()

      .append(new KeyedCodec<>(
          "RoadLevelPerDepth",
          new Short2ObjectMapCodec<>(
              new ArrayCodec<>(RoadNode.CODEC, RoadNode[]::new),
              Short2ObjectOpenHashMap::new),
          false),
          (obj, value) -> obj.roadGroupPerDepth = decodeRoadLevelPerDepth(value),
          obj -> encodeRoadLevelPerDepth(obj.roadGroupPerDepth))
      .add()

      .build();

  private Short villageRadius;
  private Map<UUID, TopologyStructure> structureIdToTopology = new HashMap<>();
  private Map<Pair<Byte, Byte>, Set<UUID>> villageGrid = new HashMap<>();

  private Map<Vector3i, RoadsGraph> roads;
  private Map<Byte, List<RoadNode>> roadGroupPerDepth = new HashMap<>();

  private Rotation villageOrientation;
  private Long seed;

  public VillageTopologyComponent() {
  }

  public Long getSeed() {
    return seed;
  }

  public void setSeed(Long seed) {
    this.seed = seed;
  }

  public Rotation getVillageOrientation() {
    if (this.villageOrientation == null) {
      int size = Rotation.values().length;
      this.villageOrientation = Rotation.values()[new FastRandom(this.seed).nextInt(0, size)];
    }
    return villageOrientation;
  }

  public void setVillageOrientation(Rotation villageOrientation) {
    this.villageOrientation = villageOrientation;
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
    // TODO: delete this
    System.out.println(this.toString());
  }

  public void registerStructure(TopologyStructure structure) {
    if (structure == null) {
      return;
    }

    TopologyStructure clonedStructure = structure.clone();
    if (clonedStructure.getStructureId() == null) {
      clonedStructure.setStructureId(UUID.randomUUID());
    }

    this.structureIdToTopology.put(clonedStructure.getStructureId(), clonedStructure);
    rebuildVillageGrid();
  }

  public Short getVillageRadius() {
    return this.villageRadius;
  }

  public Map<UUID, TopologyStructure> getRegisteredTopologyStructures() {
    return Collections.unmodifiableMap(this.structureIdToTopology);
  }

  public TopologyStructure getTopologyStructureFromStructureId(UUID structureId) {
    return this.structureIdToTopology.get(structureId);
  }

  public Set<UUID> getRegisteredStructureIdsAt(byte xCell, byte zCell) {
    Pair<Byte, Byte> key = Pair.of(xCell, zCell);
    Set<UUID> structureIds = this.villageGrid.get(key);
    if (structureIds == null || structureIds.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(new HashSet<>(structureIds));
  }

  private static int blockToCellIndex(int blockOffset) {
    return Math.floorDiv(blockOffset + (CELL_SIZE / 2), CELL_SIZE);
  }

  private void setRegisteredTopologyStructures(Map<UUID, TopologyStructure> structures) {
    this.structureIdToTopology = new HashMap<>();
    if (structures != null) {
      structures.forEach((structureId, structure) -> {
        if (structureId != null && structure != null) {
          this.structureIdToTopology.put(structureId, structure);
        }
      });
    }

    rebuildVillageGrid();
  }

  private void rebuildVillageGrid() {
    this.villageGrid = new HashMap<>();
    Vector2i villageCenter = getCoreCenter2D();

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

  private void registerStructureOnGrid(TopologyStructure structure, Vector2i villageCenter) {
    if (structure == null || structure.getStructureId() == null || villageCenter == null) {
      return;
    }

    Vector2i topLeft = structure.getAxisAlignedTopLeft();
    Vector2i botRight = structure.getAxisAlignedBotRight();
    if (topLeft == null || botRight == null) {
      return;
    }

    Vector2i relativeTopLeft = new Vector2i(
        topLeft.x - villageCenter.x,
        topLeft.y - villageCenter.y);
    Vector2i relativeBotRight = new Vector2i(
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
      this.roads.forEach((position, graph) -> component.roads.put(
          position == null ? null : position.clone(),
          graph == null ? null : graph.clone()));
    }

    component.roadGroupPerDepth = copyRoadLevelPerDepth(this.roadGroupPerDepth);
    return component;
  }

  public static ComponentType<ChunkStore, VillageTopologyComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageTopologyComponentType();
  }

  private static Vector2i getStructureCenter2D(TopologyStructure structure) {
    return structure.getCenter2D();
  }

  private Vector2i getCoreCenter2D() {
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
      Vector2i relativeTopLeft,
      Vector2i relativeBotRight) {
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

  private static Map<Byte, List<RoadNode>> decodeRoadLevelPerDepth(Short2ObjectMap<RoadNode[]> encoded) {
    Map<Byte, List<RoadNode>> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    encoded.forEach((depth, nodes) -> decoded.put(depth.byteValue(), decodeRoadNodes(nodes)));
    return decoded;
  }

  private static Short2ObjectMap<RoadNode[]> encodeRoadLevelPerDepth(Map<Byte, List<RoadNode>> roadLevelPerDepth) {
    Short2ObjectOpenHashMap<RoadNode[]> encoded = new Short2ObjectOpenHashMap<>();

    if (roadLevelPerDepth == null) {
      return encoded;
    }

    roadLevelPerDepth.forEach((depth, nodes) -> {
      if (depth == null || nodes == null) {
        return;
      }
      encoded.put(depth.shortValue(), encodeRoadNodes(nodes));
    });
    return encoded;
  }

  private static Map<UUID, TopologyStructure> decodeTopologyStructures(Map<String, TopologyStructure> encoded) {
    Map<UUID, TopologyStructure> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    encoded.forEach((structureId, structure) -> {
      if (structureId == null || structure == null) {
        return;
      }

      UUID uuid = UUID.fromString(structureId);
      TopologyStructure storedStructure = structure.clone();
      storedStructure.setStructureId(uuid);
      decoded.put(uuid, storedStructure);
    });

    return decoded;
  }

  private static HashMap<String, TopologyStructure> encodeTopologyStructures(Map<UUID, TopologyStructure> structures) {
    HashMap<String, TopologyStructure> encoded = new HashMap<>();

    if (structures == null) {
      return encoded;
    }

    structures.forEach((structureId, structure) -> {
      if (structureId == null || structure == null) {
        return;
      }

      TopologyStructure storedStructure = structure.clone();
      storedStructure.setStructureId(structureId);
      encoded.put(structureId.toString(), storedStructure);
    });

    return encoded;
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

  private static Map<Byte, List<RoadNode>> copyRoadLevelPerDepth(Map<Byte, List<RoadNode>> roadLevelPerDepth) {
    Map<Byte, List<RoadNode>> copy = new HashMap<>();

    if (roadLevelPerDepth == null) {
      return copy;
    }

    roadLevelPerDepth.forEach((depth, nodes) -> {
      if (depth != null) {
        copy.put(depth, copyRoadNodes(nodes));
      }
    });
    return copy;
  }

  private static List<RoadNode> decodeRoadNodes(RoadNode[] nodes) {
    List<RoadNode> decoded = new ArrayList<>();

    if (nodes == null) {
      return decoded;
    }

    for (RoadNode node : nodes) {
      decoded.add(node);
    }
    return decoded;
  }

  private static RoadNode[] encodeRoadNodes(List<RoadNode> nodes) {
    if (nodes == null) {
      return new RoadNode[0];
    }

    return nodes.toArray(RoadNode[]::new);
  }

  private static List<RoadNode> copyRoadNodes(List<RoadNode> nodes) {
    List<RoadNode> copy = new ArrayList<>();

    if (nodes == null) {
      return copy;
    }

    for (RoadNode node : nodes) {
      copy.add(node == null ? null : node.clone());
    }
    return copy;
  }
}
