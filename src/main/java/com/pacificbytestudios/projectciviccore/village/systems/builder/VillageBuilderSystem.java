package com.pacificbytestudios.projectciviccore.village.systems.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.LateralVacancySegments;
import com.pacificbytestudios.projectciviccore.codecs.LateralVacancySegments.Segment;
import com.pacificbytestudios.projectciviccore.codecs.RoadsGraph.RoadNode;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudios.projectciviccore.village.core.spawner.RoadSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadLength;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadType;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnHouseTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageRoadState;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;

import it.unimi.dsi.fastutil.Pair;

public class VillageBuilderSystem extends EntityTickingSystem<ChunkStore> {
  private VillageTopologyComponent topology;
  private VillageBuilderComponent builder;
  private VillageHeartComponent heart;
  private VillageProject project;
  private World world;
  private final Map<UUID, Integer> firstVillageHouseSpawnCounter = new HashMap<>();

  @Override
  public void tick(
      float deltaTime,
      int index,
      ArchetypeChunk<ChunkStore> archetypeChunk,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    this.builder = archetypeChunk.getComponent(index,
        VillageBuilderComponent.getComponentType());
    this.topology = archetypeChunk.getComponent(index,
        VillageTopologyComponent.getComponentType());
    this.heart = archetypeChunk.getComponent(index,
        VillageHeartComponent.getComponentType());
    this.world = store.getExternalData().getWorld();

    Ref<ChunkStore> villageRef = archetypeChunk.getReferenceTo(index);
    World world = store.getExternalData().getWorld();

    if (builder.isFirstVillageBuildProject()) {
      handleFirstVillageProjectPlan(villageRef);
      return;
    }

    if (!builder.hasProject()) {
      System.out.println("NO MORE PROJECTS TO DO");
      world.execute(() -> store.removeComponent(villageRef,
          VillageBuilderComponent.getComponentType()));
      System.out.println(this.topology);
      return;
    }

    System.out.println("Is first village project? " +
        builder.isFirstVillageBuildProject());
    System.out.println("Procedo nella costruzione di: ");
    System.out.println(builder.peekNext());

    boolean completed = true;
    this.project = builder.peekNext();
    VillageObjectiveType objectiveType = project.getObjectiveType();

    if (objectiveType.equals(VillageObjectiveType.BUILD_HOUSE) ||
        objectiveType.equals(VillageObjectiveType.BUILD_WORKPLACE)) {
      completed = handleNewBuildingConstruction();
    }

    if (completed) {
      builder.pollNextProject();
    }
  }

  private boolean handleNewBuildingConstruction() {
    // Rotation villageOrientation = topology.getVillageOrientation();
    return true;
  }

  private void handleFirstVillageProjectPlan(Ref<ChunkStore> villageRef) {
    System.out.println("Building NEW VILLAGE!!!");

    if (!builder.hasProject()) {
      builder.setFirstVillageBuildProject(false);
      return;
    }

    TopologyStructure heartTopology = topology.getTopologyStructureFromStructureId(heart.getUuid());

    if (!builder.getHaveFirstRoadsBeenBuilt()) {
      handleFirstVillageRoads(heartTopology);
      builder.setHaveFirstRoadsBeenBuilt(true);
    }

    this.project = builder.pollNextProject();

    if (this.project.getStructureType() != StructureType.TOWN_HALL
        && this.project.getStructureType() != StructureType.HOUSE
        && this.project.getStructureType() != StructureType.STORAGE
        && this.project.getStructureType() != StructureType.TAVERN) {
      return;
    }

    final Vector3i spawningPosition;
    final Rotation rotation;

    if (this.project.getStructureType() == StructureType.TOWN_HALL
        || this.project.getStructureType() == StructureType.HOUSE) {
      spawningPosition = calculateInitialSpawingPosition(heartTopology);

      if (this.project.getStructureType() == StructureType.HOUSE) {
        int houseIndex = firstVillageHouseSpawnCounter.getOrDefault(heart.getUuid(),
            0);
        firstVillageHouseSpawnCounter.put(heart.getUuid(), houseIndex + 1);
      }

      rotation = calculateInitialSpawningRotation(heartTopology, spawningPosition);
      spawningPosition.setY(heartTopology.getY());
    } else if (this.project.getStructureType() == StructureType.TAVERN) {
      Pair<Vector3i, Rotation> result = calculateTavernPosition(heartTopology);
      spawningPosition = result.left();
      rotation = result.right();
    } else if (this.project.getStructureType() == StructureType.STORAGE) {
      Pair<Vector3i, Rotation> result = calculateInitialStoragePosition(heartTopology);
      spawningPosition = result.left();
      rotation = result.right();
    } else {
      throw new IllegalStateException("StructureType not supported");
    }

    final Vector3i queuedSpawningPosition = spawningPosition.clone();
    final Rotation queuedRotation = rotation;

    this.world.execute(() -> enqueueBuildingSpawn(
        villageRef,
        this.project.getStructureType(),
        queuedSpawningPosition,
        queuedRotation));
  }

  private void enqueueBuildingSpawn(
      Ref<ChunkStore> villageRef,
      StructureType structureType,
      Vector3i position,
      Rotation rotation) {
    VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();

    if (structureType == StructureType.HOUSE) {
      taskQueue.registerTask(new SpawnHouseTask(position.clone(), rotation));
    } else {
      taskQueue.registerTask(new SpawnWorkplaceTask(structureType, position.clone(), rotation));
    }

    Utils.safeVillageTaskEnqueue(villageRef, taskQueue);
  }

  private Vector3i calculateInitialSpawingPosition(
      TopologyStructure heartTopology) {
    return switch (this.project.getStructureType()) {
      case TOWN_HALL -> calculateTownHallPosition(heartTopology);
      case HOUSE -> calculateInitialHousesPosition(
          heartTopology,
          firstVillageHouseSpawnCounter.getOrDefault(heart.getUuid(), 0));
      default -> throw new IllegalArgumentException(
          "[calculateInitialSpawingPosition] Structure type: " +
              this.project.getStructureType() + " not handled");
    };
  }

  private Rotation calculateInitialSpawningRotation(
      TopologyStructure heartTopology,
      Vector3i spawiningPosition) {
    return switch (this.project.getStructureType()) {
      case TOWN_HALL -> heartTopology.getOrientation();
      case HOUSE -> calculateInitialHouseRotation(heartTopology, spawiningPosition);
      default -> throw new IllegalArgumentException(
          "[calculateInitialSpawingPosition] Structure type: " +
              this.project.getStructureType() + " not handled");
    };
  }

  static Vector3i calculateTownHallPosition(TopologyStructure heartTopology) {
    return calculateTownHallPosition(heartTopology, StructureConfig.TownHall.SIZE_2I.y);
  }

  static Vector3i calculateTownHallPosition(TopologyStructure heartTopology, int townHallDepth) {
    Vector3i edge = heartTopology.getFrontEdge();
    Vector3i forward = heartTopology.getForwardVector();
    int halfDepthTowardsCore = townHallDepth / 2;
    int offsetFromCoreEdge = 1 + halfDepthTowardsCore;

    return edge.add(new Vector3i(
        offsetFromCoreEdge * forward.x,
        0,
        offsetFromCoreEdge * forward.z));
  }

  static Vector3i calculateInitialHousesPosition(
      TopologyStructure heartTopology,
      int index) {
    Vector3i forward = heartTopology.getForwardVector();
    boolean spawnOnLeft = index % 2 == 0;
    Vector3i side = spawnOnLeft ? heartTopology.getLeftVector() : heartTopology.getRightVector();
    Vector3i sideEdge = spawnOnLeft ? heartTopology.getLeftEdge() : heartTopology.getRightEdge();

    int heartDepthTowardTownHall = heartTopology.getDepth();
    int forwardOffset = Math.max(0,
        heartDepthTowardTownHall - StructureConfig.House.SIZE_2I.x) - 1;
    int sideOffset = 1 + StructureConfig.House.SIZE_2I.x / 2;

    return sideEdge.add(new Vector3i(
        side.x * sideOffset + forward.x * forwardOffset,
        0,
        side.z * sideOffset + forward.z * forwardOffset));
  }

  private Rotation calculateInitialHouseRotation(
      TopologyStructure heartTopology,
      Vector3i spawiningPosition) {
    Vector3i toHeart = heartTopology.getCenter().clone().subtract(spawiningPosition);
    Vector3i facing = Math.abs(toHeart.x) >= Math.abs(toHeart.z)
        ? new Vector3i(Integer.compare(toHeart.x, 0), 0, 0)
        : new Vector3i(0, 0, Integer.compare(toHeart.z, 0));

    for (Rotation rotation : Rotation.values()) {
      Vector3i axis = Utils.getRotationVector(rotation);
      if (axis.x == facing.x && axis.z == facing.z) {
        return rotation.add(Rotation.OneEighty);
      }
    }

    return heartTopology.getOrientation().add(Rotation.OneEighty);
  }

  private Pair<Vector3i, Rotation> calculateTavernPosition(TopologyStructure heartTopology) {
    Vector3i lowerDirection = Utils.getRotationVector(heartTopology.getOrientation().add(Rotation.OneEighty));
    UUID roadId = this.topology.getRoadIdAtDirection(lowerDirection);
    RoadNode roadNode = this.topology.getRoadFromId(roadId);

    Rotation[] validDirections = new Rotation[] { Rotation.Ninety, Rotation.TwoSeventy };

    int indexDir = new FastRandom(this.topology.getSeed()).nextInt(0, validDirections.length);
    Vector3i roadSide = Utils.rotateNormalizedVector(lowerDirection, validDirections[indexDir]);

    LateralVacancySegments segments = roadNode.getLateralVacancySegment(roadSide);
    Vector2i tavernSize2i = StructureConfig.Tavern.SIZE_2I;
    Segment segment = segments.buildValidSegment((short) tavernSize2i.x);
    if (segment == null) {
      throw new IllegalStateException("No valid segment found when should exist");
    }

    Vector3i center = Utils.getCenter(segment.getStart(), segment.getEnd());
    Vector3i prefabPosition = center.add(roadSide.clone().scale(Math.floorDiv(tavernSize2i.y, 2)));
    segments.removeSegment(segment);
    return Pair.of(prefabPosition, Utils.getRotationFromCardinalDirection(roadSide));
  }

  private Pair<Vector3i, Rotation> calculateInitialStoragePosition(TopologyStructure heartTopology) {
    Vector3i lowerDirection = Utils.getRotationVector(heartTopology.getOrientation().add(Rotation.OneEighty));
    UUID roadId = this.topology.getRoadIdAtDirection(lowerDirection);
    RoadNode roadNode = this.topology.getRoadFromId(roadId);

    Vector2i size2i = StructureConfig.Storage.SIZE_2I;
    Pair<Vector3i, LateralVacancySegments> result = roadNode
        .getLateralVacancySegmentWithFittingSpace((short) size2i.x);

    LateralVacancySegments segments = result.right();

    Segment segment = segments.buildValidSegment((short) size2i.x);
    if (segment == null) {
      throw new IllegalStateException("No valid segment found when should exist");
    }

    Vector3i center = Utils.getCenter(segment.getStart(), segment.getEnd());

    Vector3i roadSide = result.left();
    Vector3i prefabPosition = center.add(roadSide.clone().scale(Math.floorDiv(size2i.y, 2)));
    segments.removeSegment(segment);
    return Pair.of(prefabPosition, Utils.getRotationFromCardinalDirection(roadSide));
  }

  private void handleFirstVillageRoads(TopologyStructure heart) {
    short roadLength = (short) (StructureConfig.getLongestSide() +
        StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD));
    buildTopRoad(heart, roadLength);
    buildLeftRoad(heart, roadLength);
    buildRightRoad(heart, roadLength);
    buildBottomRoad(heart, roadLength);
  }

  private void buildTopRoad(TopologyStructure heart, short roadLength) {
    Vector3i topRoadDirectionVec = heart.getForwardVector();

    int townHallWidth = StructureConfig.TownHall.SIZE_2I.x;
    int roadWidth = StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD);
    int halfRoadSize = Math.floorDiv(roadWidth, 2);
    int townHallRightOffset = Math.floorDiv(townHallWidth, 2) + halfRoadSize + 1;

    Vector3i startingPoint = heart.getFrontEdge()
        .add(heart.getLeftVector().scale(townHallRightOffset));

    buildOriginRoad(
        RoadType.MAIN_ROAD,
        startingPoint,
        heart.getOrientation(),
        topRoadDirectionVec,
        roadLength);
  }

  private void buildBottomRoad(TopologyStructure heart, short roadLength) {
    Vector3i bottomRoadDirectionVec = heart.getBackVector();
    Vector3i bottomRoadStartPoint = heart.getBackEdge();

    buildOriginRoad(
        RoadType.MAIN_ROAD,
        bottomRoadStartPoint,
        heart.getOrientation().add(Rotation.OneEighty),
        bottomRoadDirectionVec,
        roadLength);
  }

  private void buildLeftRoad(TopologyStructure heart, short roadLength) {
    Vector3i leftRoadDirectionVec = heart.getLeftVector();
    Vector3i leftEdge = heart.getLeftEdge();

    int halfRoadSize = Math.floorDiv(StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD), 2);
    int halfDepth = Math.floorDiv(heart.getDepth(), 2) - halfRoadSize;
    Vector3i startingPoint = leftEdge.add(heart.getBackVector().scale(halfDepth));

    buildOriginRoad(
        RoadType.MAIN_ROAD,
        startingPoint,
        heart.getOrientation().add(Rotation.Ninety),
        leftRoadDirectionVec,
        roadLength);
  }

  private void buildRightRoad(TopologyStructure heart, short roadLength) {
    Vector3i rightRoadDirectionVec = heart.getRightVector();
    Vector3i rightEdge = heart.getRightEdge();

    int halfRoadSize = Math.floorDiv(StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD), 2);
    int halfDepth = Math.floorDiv(heart.getDepth(), 2) - halfRoadSize;
    Vector3i startingPoint = rightEdge.add(heart.getBackVector().scale(halfDepth));

    buildOriginRoad(
        RoadType.MAIN_ROAD,
        startingPoint,
        heart.getOrientation().add(Rotation.TwoSeventy),
        rightRoadDirectionVec,
        roadLength);
  }

  private void buildOriginRoad(
      RoadType type,
      Vector3i startPoint,
      Rotation prefabRotation,
      Vector3i roadDirectionVec,
      short length) {
    Map<RoadLength, Short> sectionsToBuild = calculateNeededRoadSections(length);
    List<RoadLength> orderedSegments = expandRoadSections(sectionsToBuild);

    if (orderedSegments.isEmpty()) {
      return;
    }

    System.out.println("~~~~~~~~~~~~~~~~~~~~~~");
    System.out.println("STARTING POINT: ");
    System.out.println(startPoint);
    System.out.println("PREFAB ROTATION: ");
    System.out.println(prefabRotation);
    System.out.println("ROAD DIRECTION VEC: ");
    System.out.println(roadDirectionVec);

    Map<Vector3i, LateralVacancySegments> lateralVacancySegments = new HashMap<>();
    Vector3i left = Utils.rotateNormalizedVector(roadDirectionVec, Rotation.Ninety);
    Vector3i right = Utils.rotateNormalizedVector(roadDirectionVec, Rotation.TwoSeventy);

    int size = StructureConfig.Road.getRoadSize(type);
    Vector3i leftSegmentStart = startPoint.clone().add(left.clone().scale(Math.floorDiv(size, 2))).add(left)
        .add(roadDirectionVec);
    Vector3i leftSegmentEnd = leftSegmentStart.clone().add(roadDirectionVec.clone().scale(length - 1));
    Vector3i rigthSegmentStart = startPoint.clone().add(right.clone().scale(Math.floorDiv(size, 2))).add(right)
        .add(roadDirectionVec);
    Vector3i rightSegmentEnd = rigthSegmentStart.clone().add(roadDirectionVec.clone().scale(length - 1));

    LateralVacancySegments leftSegments = new LateralVacancySegments();
    leftSegments.setRoadDirectionVec(roadDirectionVec);
    leftSegments.appendSegment(new Segment(leftSegmentStart, leftSegmentEnd));

    LateralVacancySegments rigthSegments = new LateralVacancySegments();
    rigthSegments.setRoadDirectionVec(roadDirectionVec);
    rigthSegments.appendSegment(new Segment(rigthSegmentStart, rightSegmentEnd));
    lateralVacancySegments.put(left, leftSegments);
    lateralVacancySegments.put(right, rigthSegments);

    RoadNode node = new RoadNode();
    node.setRoadId(UUID.randomUUID());
    node.setPrefabOrientation(prefabRotation);
    node.setRoadDirectionVec(roadDirectionVec);
    node.setBranchOriginKey(roadDirectionVec);
    node.setState(VillageRoadState.EXPANDIBLE);
    node.setDepth((byte) 0);
    node.setLateralVacancySegments(lateralVacancySegments);

    this.topology.addInitialRoad(roadDirectionVec, node);
    TopologyStructure street = buildStreetTopology(
        node.getRoadId(),
        type,
        startPoint,
        prefabRotation,
        roadDirectionVec,
        length);
    this.topology.registerStructure(street);

    this.world.execute(() -> {
      RoadLength firstSegment = orderedSegments.get(0);

      Vector3i currentPosition = startPoint.clone()
          .add(roadDirectionVec.clone().scale(firstSegment.getLength()));

      for (int i = 0; i < orderedSegments.size(); i++) {
        RoadLength currentSegment = orderedSegments.get(i);

        WorldChunk chunk = Utils.getWorldChunk(this.world, currentPosition);
        node.appendRoadSegment(currentPosition.clone());
        RoadSpawner.spawn(chunk, currentPosition, prefabRotation, this.heart, type,
            currentSegment);

        if (i + 1 < orderedSegments.size()) {
          RoadLength nextSegment = orderedSegments.get(i + 1);
          currentPosition = currentPosition.clone()
              .add(roadDirectionVec.clone().scale(nextSegment.getLength()));
        }
      }
    });
  }

  private TopologyStructure buildStreetTopology(
      UUID roadId,
      RoadType type,
      Vector3i startPoint,
      Rotation prefabRotation,
      Vector3i roadDirectionVec,
      short length) {
    Vector3i left = Utils.rotateNormalizedVector(roadDirectionVec, Rotation.Ninety);
    Vector3i right = Utils.rotateNormalizedVector(roadDirectionVec, Rotation.TwoSeventy);

    int roadSize = StructureConfig.Road.getRoadSize(type);
    int leftSpan = Math.floorDiv(roadSize - 1, 2);
    int rightSpan = Math.floorDiv(roadSize, 2);

    Vector3i roadStart = startPoint.clone().add(roadDirectionVec);
    Vector3i roadEnd = startPoint.clone().add(roadDirectionVec.clone().scale(length));

    Vector3i startLeftCorner = roadStart.clone().add(left.clone().scale(leftSpan));
    Vector3i startRightCorner = roadStart.clone().add(right.clone().scale(rightSpan));
    Vector3i endLeftCorner = roadEnd.clone().add(left.clone().scale(leftSpan));
    Vector3i endRightCorner = roadEnd.clone().add(right.clone().scale(rightSpan));

    int minX = Math.min(
        Math.min(startLeftCorner.x, startRightCorner.x),
        Math.min(endLeftCorner.x, endRightCorner.x));
    int maxX = Math.max(
        Math.max(startLeftCorner.x, startRightCorner.x),
        Math.max(endLeftCorner.x, endRightCorner.x));
    int minZ = Math.min(
        Math.min(startLeftCorner.z, startRightCorner.z),
        Math.min(endLeftCorner.z, endRightCorner.z));
    int maxZ = Math.max(
        Math.max(startLeftCorner.z, startRightCorner.z),
        Math.max(endLeftCorner.z, endRightCorner.z));

    TopologyStructure structure = new TopologyStructure(
        roadStart,
        new Vector2i(1, 1),
        VillageTopologyStructureType.fromRoadType(type),
        prefabRotation);
    structure.setStructureId(roadId);
    structure.setTopLeft(new Vector2i(minX, minZ));
    structure.setBotRight(new Vector2i(maxX, maxZ));
    return structure;
  }

  private List<RoadLength> expandRoadSections(Map<RoadLength, Short> sectionsToBuild) {
    List<RoadLength> orderedSegments = new ArrayList<>();

    for (Map.Entry<RoadLength, Short> entry : sectionsToBuild.entrySet()) {
      for (short i = 0; i < entry.getValue(); i++) {
        orderedSegments.add(entry.getKey());
      }
    }

    return orderedSegments;
  }

  private Map<RoadLength, Short> calculateNeededRoadSections(short length) {
    Map<RoadLength, Short> count = new LinkedHashMap<>();
    short remaining = length;

    for (RoadLength roadLengthEnum : StructureConfig.Road.getOrderedRoadLength()) {
      short roadLength = roadLengthEnum.getLength();

      while ((remaining - roadLength) >= 0) {
        short curr = count.getOrDefault(roadLengthEnum, (short) 0);
        count.put(roadLengthEnum, (short) (curr + 1));
        remaining -= roadLength;

        if (remaining == 0) {
          return count;
        }
      }
    }

    return count;
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        VillageBuilderComponent.getComponentType(),
        VillageHeartComponent.getComponentType(),
        VillageTopologyComponent.getComponentType());
  }
}
