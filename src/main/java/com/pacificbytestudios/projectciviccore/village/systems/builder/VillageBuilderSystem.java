package com.pacificbytestudios.projectciviccore.village.systems.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.LateralVacancySegments;
import com.pacificbytestudios.projectciviccore.codecs.LateralVacancySegments.Segment;
import com.pacificbytestudios.projectciviccore.codecs.RoadsGraph.RoadNode;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudios.projectciviccore.village.core.spawner.RoadSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadLength;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadType;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.ContinueBuildingTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnHouseTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TeleportVillagerToTownHeartTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.UpgradeBuildingTask;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
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
  private FastRandom random;
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
    this.random = new FastRandom(this.topology.getSeed());

    Ref<ChunkStore> villageRef = archetypeChunk.getReferenceTo(index);
    World world = store.getExternalData().getWorld();

    if (builder.isFirstVillageBuildProject()) {
      handleFirstVillageProjectPlan(villageRef);
      return;
    }

    if (!builder.hasProject()) {
      world.execute(() -> store.removeComponent(villageRef,
          VillageBuilderComponent.getComponentType()));
      return;
    }

    boolean completed = true;
    this.project = builder.peekNext();
    VillageObjectiveType objectiveType = project.getObjectiveType();

    if (objectiveType.equals(VillageObjectiveType.BUILD_HOUSE) ||
        objectiveType.equals(VillageObjectiveType.BUILD_WORKPLACE)) {
      if (this.topology.isStructureAlreadyPresent(this.project.getStructureId())) {
        completed = handleContinueBuildingConstruction(villageRef);
      } else {
        completed = handleNewBuildingConstruction(villageRef);
      }
    }

    if (objectiveType.equals(VillageObjectiveType.UPGRADE_BUILDING)) {
      completed = handleUpgradeBuilding(villageRef);
    }

    if (completed) {
      builder.pollNextProject();
    }
  }

  private boolean handleNewBuildingConstruction(Ref<ChunkStore> villageRef) {
    StructureType structureType = this.project.getStructureType();
    Vector2i size2i = Utils.getSize2i(structureType);
    RoadsideConstructionPlacement placement = findAppropriateSegementFor(size2i);
    if (placement == null) {
      tryExpandRoadNetwork(RoadType.MAIN_ROAD);
      return false;
    }

    placement.segments.removeSegment(placement.segment);
    RoadNode road = reserveInitialStructureStreetSegments(
        structureType,
        placement.prefabAnchor,
        placement.rotation);

    if (road == null) {
      return false;
    }

    final Vector3i queuedSpawningPosition = placement.prefabAnchor.clone();
    final Rotation queuedRotation = placement.rotation;
    this.world.execute(() -> enqueueBuildingSpawn(
        villageRef,
        structureType,
        queuedSpawningPosition,
        queuedRotation,
        ConstructionMode.STAGED));

    randomlySpawnSideStreet(road);
    return true;
  }

  private void randomlySpawnSideStreet(RoadNode parent) {
    short postBuildSideRoadChance = this.heart.getConfig().getPostBuildSideRoadChance();
    boolean spawn = rollChance(postBuildSideRoadChance);

    if (!spawn) {
      return;
    }

    boolean leftFirst = this.random.nextInt(0, 2) == 0;

    int length = StructureConfig.getLongestSide() +
        StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD);
    tryExpandRoadFrom(parent, RoadType.MAIN_ROAD, (short) length, true,
        leftFirst);
  }

  private void tryExpandRoadNetwork(RoadType roadType) {
    int length = StructureConfig.getLongestSide() +
        StructureConfig.Road.getRoadSize(roadType);
    List<UUID> roadIds = getRoadIdsOrderedForExpansion();
    for (UUID roadId : roadIds) {
      RoadNode road = this.topology.getRoadFromId(roadId);
      if (road == null || road.getState() != VillageRoadState.EXPANDIBLE) {
        continue;
      }

      boolean preferTurn = rollChance(this.heart.getConfig().getFallbackMainRoadTurnChance());
      boolean leftFirst = this.random.nextInt(0, 2) == 0;

      if (tryExpandRoadFrom(road, roadType, (short) length, preferTurn, leftFirst)) {
        return;
      }
    }
  }

  private List<UUID> getRoadIdsOrderedForExpansion() {
    List<UUID> orderedRoadIds = new ArrayList<>(this.topology.getRegisteredRoadIdsByDepthAscending());
    Map<UUID, Integer> originalIndexes = new HashMap<>();
    for (int index = 0; index < orderedRoadIds.size(); index++) {
      originalIndexes.putIfAbsent(orderedRoadIds.get(index), index);
    }

    orderedRoadIds.sort((leftRoadId, rightRoadId) -> {
      RoadNode leftRoad = this.topology.getRoadFromId(leftRoadId);
      RoadNode rightRoad = this.topology.getRoadFromId(rightRoadId);

      int bySpan = Integer.compare(getRoadLinearSpan(leftRoad),
          getRoadLinearSpan(rightRoad));
      if (bySpan != 0) {
        return bySpan;
      }

      int leftDepth = leftRoad == null ? Integer.MAX_VALUE : leftRoad.getDepth();
      int rightDepth = rightRoad == null ? Integer.MAX_VALUE : rightRoad.getDepth();
      int byDepth = Integer.compare(leftDepth, rightDepth);
      if (byDepth != 0) {
        return byDepth;
      }

      return Integer.compare(
          originalIndexes.getOrDefault(leftRoadId, Integer.MAX_VALUE),
          originalIndexes.getOrDefault(rightRoadId, Integer.MAX_VALUE));
    });
    return orderedRoadIds;
  }

  private int getRoadLinearSpan(RoadNode road) {
    if (road == null || road.getRoadId() == null || road.getRoadDirectionVec() == null) {
      return Integer.MAX_VALUE;
    }

    TopologyStructure roadTopology = this.topology.getTopologyStructureFromStructureId(road.getRoadId());
    if (roadTopology == null) {
      return Integer.MAX_VALUE;
    }

    Vector3i roadDirectionVec = road.getRoadDirectionVec();
    if (roadDirectionVec.x != 0) {
      return roadTopology.getMaxX() - roadTopology.getMinX() + 1;
    }
    if (roadDirectionVec.z != 0) {
      return roadTopology.getMaxZ() - roadTopology.getMinZ() + 1;
    }

    return Integer.MAX_VALUE;
  }

  private boolean tryExpandRoadFrom(
      RoadNode parent,
      RoadType type,
      short requestedLength,
      boolean preferTurn,
      boolean leftFirst) {
    if (!parent.getState().equals(VillageRoadState.EXPANDIBLE)) {
      return false;
    }

    Vector3i roadDirection = parent.getRoadDirectionVec();
    if (!preferTurn) {
      Vector3i startPoint = resolveForwardRoadStartPoint(parent);
      if (startPoint == null) {
        return false;
      }

      Rotation rotation = Utils.getRotationFromCardinalDirection(roadDirection);
      TopologyStructure streetTopology = buildTransientStreetTopology(
          type,
          startPoint,
          rotation,
          roadDirection,
          requestedLength);

      if (collidesWithRegisteredTopology(streetTopology)) {
        return false;
      }

      buildRoad(
          parent,
          type,
          startPoint,
          rotation,
          roadDirection,
          requestedLength);
      return true;
    }

    short roadSize = (short) StructureConfig.Road.getRoadSize(type);
    Vector3i left = Utils.rotateNormalizedVector(roadDirection, Rotation.Ninety);
    Vector3i right = Utils.rotateNormalizedVector(roadDirection,
        Rotation.TwoSeventy);
    Vector3i[] sidesToLook = leftFirst ? new Vector3i[] { left, right }
        : new Vector3i[] {
            right, left };

    boolean newRoadBuilt = false;
    for (Vector3i side : sidesToLook) {
      LateralVacancySegments lateralVacancySegment = parent.getLateralVacancySegment(side);
      if (lateralVacancySegment == null ||
          !lateralVacancySegment.hasFittinSegment(roadSize)) {
        continue;
      }

      Segment segment = lateralVacancySegment.buildSegmentFromEnd(roadSize);
      Rotation rotation = Utils.getRotationFromCardinalDirection(side);

      Vector3i center = Utils.getCenter(segment.getStart(), segment.getEnd());
      TopologyStructure streetTopology = buildTransientStreetTopology(
          type,
          center.subtract(side),
          rotation,
          side,
          requestedLength);

      if (collidesWithRegisteredTopology(streetTopology)) {
        continue;
      }

      buildRoad(
          parent,
          type,
          center,
          Utils.getRotationFromCardinalDirection(side),
          side,
          requestedLength);
      newRoadBuilt = true;
    }

    return newRoadBuilt;
  }

  private Vector3i resolveForwardRoadStartPoint(RoadNode parent) {
    if (parent == null || parent.getRoadId() == null ||
        parent.getRoadDirectionVec() == null) {
      return null;
    }

    TopologyStructure parentTopology = this.topology.getTopologyStructureFromStructureId(parent.getRoadId());
    if (parentTopology == null) {
      return null;
    }

    Vector3i edge = parentTopology.getEdge(parent.getRoadDirectionVec());
    return edge == null ? null : edge.clone();
  }

  private boolean collidesWithRegisteredTopology(TopologyStructure structure) {
    if (structure == null) {
      return false;
    }

    Set<UUID> possibleCollidingStructures = this.topology.getRegisteredStructureIdsAt(structure);
    for (UUID structureId : possibleCollidingStructures) {
      TopologyStructure collidingStructure = this.topology.getTopologyStructureFromStructureId(structureId);
      if (collidingStructure != null && collidingStructure.collide(structure)) {
        return true;
      }
    }

    return false;
  }

  private RoadsideConstructionPlacement findAppropriateSegementFor(Vector2i size2i) {
    List<UUID> roadIds = this.topology.getRegisteredRoadIdsByDepthAscending();
    short width = (short) size2i.x;
    for (UUID roadId : roadIds) {
      RoadNode road = this.topology.getRoadFromId(roadId);

      for (Map.Entry<Vector3i, LateralVacancySegments> segmentEntry : road.getLateralVacancySegments().entrySet()) {
        Vector3i roadSide = segmentEntry.getKey();
        LateralVacancySegments segments = segmentEntry.getValue();
        Vector3i roadDirectionVec = segments == null ? null : segments.getRoadDirectionVec();
        if (roadSide == null || segments == null || roadDirectionVec == null ||
            segments.getValidSegments() == null || !segments.hasFittinSegment(width)) {
          continue;
        }

        for (Segment validSegment : segments.getValidSegments()) {
          if (validSegment.getSize() == null) {
            validSegment.recalculateSize();
          }
          if (validSegment.getSize() == null || validSegment.getSize() < width) {
            continue;
          }

          int candidateCount = validSegment.getSize() - width + 1;
          for (int offset = 0; offset < candidateCount; offset++) {
            Vector3i candidateStart = validSegment.getStart().clone()
                .add(roadDirectionVec.clone().scale(offset));
            Vector3i candidateEnd = candidateStart.clone().add(roadDirectionVec.clone().scale(width - 1));
            Segment resultSegment = new Segment(candidateStart, candidateEnd);

            Vector3i prefabPosition = calculateRoadsidePrefabAnchor(resultSegment,
                roadSide, size2i);
            if (isValidPlacementPosition(prefabPosition,
                Utils.rotateNormalizedVector(roadSide, Rotation.OneEighty))) {
              return new RoadsideConstructionPlacement(
                  segments,
                  resultSegment,
                  prefabPosition,
                  Utils.getRotationFromCardinalDirection(roadSide));
            }
          }
        }
      }
    }

    return null;
  }

  private boolean handleContinueBuildingConstruction(Ref<ChunkStore> villageRef) {
    TopologyStructure structure = this.topology
        .getTopologyStructureFromStructureId(this.project.getStructureId());

    VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();
    taskQueue.registerTask(
        new ContinueBuildingTask(
            structure.getStructureType(),
            structure.getPrefabAnchor(),
            structure.getOrientation()));

    this.world.execute(() -> {
      Utils.safeVillageTaskEnqueue(villageRef, taskQueue);
    });
    return true;
  }

  private boolean handleUpgradeBuilding(Ref<ChunkStore> villageRef) {
    if (this.project.getStage() > 0) {
      return handleContinueBuildingConstruction(villageRef);
    }

    TopologyStructure structure = this.topology
        .getTopologyStructureFromStructureId(this.project.getStructureId());
    Vector3i existingCoreOrSitePosition = resolveUpgradeStartPosition(structure);
    if (structure == null || existingCoreOrSitePosition == null) {
      ProjectCivicCore.LOGGER.atWarning().log(
          "[VillageBuilderSystem] Could not resolve upgrade start position for project "
              + this.project.getProjectId() + " and structure " + this.project.getStructureId());
      return false;
    }

    VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();

    if (structure.getStructureType() != StructureType.HOUSE) {
      this.heart.getWorkplaceFromId(structure.getStructureId())
          .getWorkers()
          .stream()
          .map(TeleportVillagerToTownHeartTask::new)
          .forEach(taskQueue::registerTask);
    } else {
      this.heart.getHouseFromId(structure.getStructureId())
          .getAppartmentRegistry()
          .getOccupants()
          .stream()
          .map(TeleportVillagerToTownHeartTask::new)
          .forEach(taskQueue::registerTask);
    }

    taskQueue.registerTask(
        new UpgradeBuildingTask(
            structure.getStructureType(),
            existingCoreOrSitePosition,
            this.project.getTargetLevel(),
            ConstructionMode.STAGED));

    this.world.execute(() -> {
      Utils.safeVillageTaskEnqueue(villageRef, taskQueue);
    });
    return true;
  }

  private Vector3i resolveUpgradeStartPosition(TopologyStructure structure) {
    if (structure == null || structure.getStructureId() == null || structure.getStructureType() == null) {
      return null;
    }

    if (structure.getStructureType() == StructureType.HOUSE) {
      HouseComponent house = this.heart.getHouseFromId(structure.getStructureId());
      if (house != null && house.getCoreBlockPosition() != null) {
        return house.getCoreBlockPosition().clone();
      }
    } else {
      WorkplaceComponent workplace = this.heart.getWorkplaceFromId(structure.getStructureId());
      if (workplace != null && workplace.getBenchPos() != null) {
        return workplace.getBenchPos().clone();
      }
    }

    return structure.getPrefabAnchor() == null ? null : structure.getPrefabAnchor().clone();
  }

  private static final class RoadsideConstructionPlacement {
    private final LateralVacancySegments segments;
    private final Segment segment;
    private final Vector3i prefabAnchor;
    private final Rotation rotation;

    private RoadsideConstructionPlacement(
        LateralVacancySegments segments,
        Segment segment,
        Vector3i prefabAnchor,
        Rotation rotation) {
      this.segments = segments;
      this.segment = segment;
      this.prefabAnchor = prefabAnchor;
      this.rotation = rotation;
    }
  }

  private boolean isValidPlacementPosition(
      Vector3i prefabPosition,
      Vector3i directionVec) {
    Rotation rotation = Utils.getRotationFromCardinalDirection(directionVec);
    TopologyStructure topologyStructure = buildTransientStructureFootprint(this.project.getStructureType(),
        prefabPosition, rotation);
    Set<UUID> possibleCollidingStructures = this.topology.getRegisteredStructureIdsAt(topologyStructure);

    for (UUID structureId : possibleCollidingStructures) {
      TopologyStructure collidingStructure = this.topology.getTopologyStructureFromStructureId(structureId);
      if (collidingStructure != null &&
          collidingStructure.collide(topologyStructure)) {
        return false;
      }
    }
    return true;
  }

  private void handleFirstVillageProjectPlan(Ref<ChunkStore> villageRef) {
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
      throw new IllegalArgumentException("Unsupported structure as initial: " +
          this.project.getStructureType());
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

    if (this.project.getStructureType() == StructureType.TOWN_HALL
        || this.project.getStructureType() == StructureType.HOUSE) {
      reserveInitialStructureStreetSegments(
          this.project.getStructureType(),
          spawningPosition,
          rotation);
    }

    this.project.setStructureId(UUID.randomUUID());

    this.world.execute(() -> enqueueBuildingSpawn(
        villageRef,
        this.project.getStructureType(),
        queuedSpawningPosition,
        queuedRotation,
        ConstructionMode.COMPLETED));
  }

  private void enqueueBuildingSpawn(
      Ref<ChunkStore> villageRef,
      StructureType structureType,
      Vector3i position,
      Rotation rotation,
      ConstructionMode constructionMode) {
    VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();

    if (structureType == StructureType.HOUSE) {
      taskQueue.registerTask(new SpawnHouseTask(
          this.project.getStructureId(),
          position.clone(),
          rotation,
          this.project.getTargetLevel(),
          constructionMode));
    } else {
      taskQueue.registerTask(new SpawnWorkplaceTask(
          this.project.getStructureId(),
          structureType,
          position.clone(),
          rotation,
          this.project.getTargetLevel(),
          constructionMode));
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
      case HOUSE -> calculateInitialHouseRotation(heartTopology,
          spawiningPosition);
      default -> throw new IllegalArgumentException(
          "[calculateInitialSpawingPosition] Structure type: " +
              this.project.getStructureType() + " not handled");
    };
  }

  static Vector3i calculateTownHallPosition(TopologyStructure heartTopology) {
    return calculateTownHallPosition(heartTopology,
        StructureConfig.TownHall.SIZE_2I.y);
  }

  static Vector3i calculateTownHallPosition(TopologyStructure heartTopology,
      int townHallDepth) {
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
        heartDepthTowardTownHall - StructureConfig.House.SIZE_2I.x) - 3;
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
    RoadNode roadNode = this.topology.getRootRoadAtDirection(lowerDirection);
    Rotation[] validDirections = new Rotation[] { Rotation.Ninety,
        Rotation.TwoSeventy };

    int indexDir = new FastRandom(this.topology.getSeed()).nextInt(0,
        validDirections.length);
    Vector3i roadSide = Utils.rotateNormalizedVector(lowerDirection,
        validDirections[indexDir]);

    LateralVacancySegments segments = roadNode.getLateralVacancySegment(roadSide);
    return reserveRoadsidePlacement(segments, roadSide,
        StructureConfig.Tavern.SIZE_2I);
  }

  private Pair<Vector3i, Rotation> calculateInitialStoragePosition(TopologyStructure heartTopology) {
    Vector3i lowerDirection = Utils.getRotationVector(heartTopology.getOrientation().add(Rotation.OneEighty));
    RoadNode roadNode = this.topology.getRootRoadAtDirection(lowerDirection);

    Vector2i size2i = StructureConfig.Storage.SIZE_2I;
    Pair<Vector3i, LateralVacancySegments> result = roadNode
        .getLateralVacancySegmentWithFittingSpace((short) size2i.x);
    if (result == null) {
      throw new IllegalStateException("No lateral vacancy segment found when should exist");
    }
    return reserveRoadsidePlacement(result.right(), result.left(), size2i);
  }

  static Pair<Vector3i, Rotation> reserveRoadsidePlacement(
      LateralVacancySegments segments,
      Vector3i roadSide,
      Vector2i footprint) {
    Segment segment = segments.buildSegmentFromStart((short) footprint.x);
    if (segment == null) {
      throw new IllegalStateException("No valid segment found when should exist");
    }

    Vector3i prefabPosition = calculateRoadsidePrefabAnchor(segment, roadSide,
        footprint);
    segments.removeSegment(segment);
    return Pair.of(prefabPosition,
        Utils.getRotationFromCardinalDirection(roadSide));
  }

  static Vector3i calculateRoadsidePrefabAnchor(
      Segment segment,
      Vector3i roadSide,
      Vector2i footprint) {
    Vector3i center = Utils.getCenter(segment.getStart(), segment.getEnd());
    return center.add(roadSide.clone().scale(Math.floorDiv(footprint.y, 2)));
  }

  private void handleFirstVillageRoads(TopologyStructure heart) {
    short roadLength = (short) (StructureConfig.getLongestSide() +
        StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD));
    buildTopRoad(heart, roadLength);

    buildLeftRoad(heart, roadLength);
    buildRightRoad(heart, roadLength);
    buildBottomRoad(heart, roadLength);
  }

  private void buildTopRoad(TopologyStructure heart, short maxRoadLength) {
    Vector3i topRoadDirectionVec = heart.getForwardVector();

    int roadWidth = StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD);
    Vector2i townHallSize = StructureConfig.TownHall.SIZE_2I;

    int townHallDepth = townHallSize.y;
    short roadLength = (short) (townHallDepth + roadWidth);
    int townHallWidth = townHallSize.x;
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

    buildTopHorizontalRoad(heart, maxRoadLength);
  }

  private void buildTopHorizontalRoad(TopologyStructure heart, short maxRoadLength) {
    int roadWidth = StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD);
    Vector3i directionTop = Utils.getRotationVector(heart.getOrientation());
    RoadNode node = this.topology.getRootRoadAtDirection(directionTop);
    Vector3i right = Utils.rotateNormalizedVector(directionTop,
        Rotation.TwoSeventy);
    LateralVacancySegments segments = node.getLateralVacancySegment(right);
    Segment segment = segments.buildSegmentFromEnd((short) roadWidth);

    Vector3i startingPoint = Utils.getCenter(segment.getStart(),
        segment.getEnd());

    buildRoad(
        node,
        RoadType.MAIN_ROAD,
        startingPoint.subtract(right),
        Utils.getRotationFromCardinalDirection(right),
        right,
        (short) (StructureConfig.TownHall.SIZE_2I.x + roadWidth));
    segments.removeSegment(segment);

    List<UUID> roads = this.topology.getRoadsIdsAtDepth(this.topology.getHighestRoadLevel());
    if (roads == null || roads.isEmpty()) {
      throw new IllegalStateException("Unexpeted state, missing roads that should already be initialized");
    }
    RoadNode road = this.topology.getRoadFromId(roads.get(0));
    buildTopRoadDeadEnd(heart, road);
    buildLastTopRoad(heart, road, maxRoadLength);
  }

  private void buildTopRoadDeadEnd(TopologyStructure heart, RoadNode parentRoad) {
    Vector3i roadSide = Utils.rotateNormalizedVector(parentRoad.getRoadDirectionVec(),
        Rotation.TwoSeventy);
    LateralVacancySegments segments = parentRoad.getLateralVacancySegment(roadSide);
    Segment segment = segments.buildSegmentFromEnd((short) StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD));
    Vector3i startingPoint = Utils.getCenter(segment.getStart(),
        segment.getEnd());

    buildRoad(
        parentRoad,
        RoadType.MAIN_ROAD,
        startingPoint.subtract(roadSide),
        Utils.getRotationFromCardinalDirection(roadSide),
        roadSide,
        (short) StructureConfig.TownHall.SIZE_2I.y);
    segments.removeSegment(segment);
  }

  private void buildLastTopRoad(TopologyStructure heart, RoadNode parentRoad,
      short maxRoadLength) {
    Vector3i roadSide = Utils.rotateNormalizedVector(parentRoad.getRoadDirectionVec(),
        Rotation.Ninety);
    LateralVacancySegments segments = parentRoad.getLateralVacancySegment(roadSide);

    short roadSize = (short) StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD);
    short halfRoadSize = (short) Math.floorDiv(roadSize, 2);
    Segment leftSideRoadSegment = segments.getFirstValidSegment(roadSize);

    Vector3i center = Utils.getCenter(leftSideRoadSegment.getStart(),
        leftSideRoadSegment.getEnd());

    Vector3i roadDirectionInverted = Utils.rotateNormalizedVector(parentRoad.getRoadDirectionVec(),
        Rotation.OneEighty);

    Vector3i startSegment = center.clone().add(roadDirectionInverted.clone().scale(halfRoadSize))
        .add(roadDirectionInverted.clone().scale(halfRoadSize));
    Vector3i endSegment = center.clone().add(parentRoad.getRoadDirectionVec().clone().scale(halfRoadSize))
        .add(roadDirectionInverted.clone().scale(halfRoadSize));

    Segment segment = segments.buildSegmentAt(startSegment, endSegment);
    Vector3i startingPoint = Utils.getCenter(segment.getStart(),
        segment.getEnd());

    buildRoad(
        parentRoad,
        RoadType.MAIN_ROAD,
        startingPoint.subtract(roadSide),
        Utils.getRotationFromCardinalDirection(roadSide),
        roadSide,
        maxRoadLength);
    segments.removeSegment(segment);
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
    buildRoadInternal(null, type, startPoint, prefabRotation, roadDirectionVec,
        length);
  }

  private void buildRoad(
      RoadNode parent,
      RoadType type,
      Vector3i startPoint,
      Rotation prefabRotation,
      Vector3i roadDirectionVec,
      short length) {
    buildRoadInternal(parent, type, startPoint, prefabRotation, roadDirectionVec,
        length);
  }

  private void buildRoadInternal(
      RoadNode parent,
      RoadType type,
      Vector3i startPoint,
      Rotation prefabRotation,
      Vector3i roadDirectionVec,
      short length) {
    Map<RoadLength, Short> sectionsToBuild = calculateNeededRoadSections(length);
    List<RoadLength> orderedSegments = expandRoadSections(sectionsToBuild);

    Map<Vector3i, LateralVacancySegments> lateralVacancySegments = createLateralVacancySegments(
        type,
        startPoint,
        roadDirectionVec,
        length);
    RoadNode node;
    if (isSameDirectionContinuation(parent, roadDirectionVec)) {
      node = parent;
      mergeLateralVacancySegments(node, lateralVacancySegments);

      TopologyStructure street = buildTransientStreetTopology(
          type,
          startPoint,
          prefabRotation,
          roadDirectionVec,
          length);
      extendRegisteredRoadTopology(node, street);
    } else {
      node = createRoadNode(parent, prefabRotation, roadDirectionVec,
          lateralVacancySegments);
      this.topology.addRoad(node);

      TopologyStructure street = buildStreetTopology(
          node.getRoadId(),
          type,
          startPoint,
          prefabRotation,
          roadDirectionVec,
          length);
      this.topology.registerStructure(street);
    }

    spawnRoadSegmentsAsync(node, type, startPoint, prefabRotation,
        roadDirectionVec, orderedSegments);
  }

  private boolean isSameDirectionContinuation(
      RoadNode parent,
      Vector3i roadDirectionVec) {
    return parent != null &&
        parent.getRoadDirectionVec() != null &&
        roadDirectionVec != null &&
        parent.getRoadDirectionVec().equals(roadDirectionVec);
  }

  private void mergeLateralVacancySegments(
      RoadNode roadNode,
      Map<Vector3i, LateralVacancySegments> segmentsBySide) {
    if (roadNode == null || segmentsBySide == null || segmentsBySide.isEmpty()) {
      return;
    }

    Map<Vector3i, LateralVacancySegments> currentSegmentsBySide = roadNode.getLateralVacancySegments();
    if (currentSegmentsBySide == null) {
      currentSegmentsBySide = new HashMap<>();
      roadNode.setLateralVacancySegments(currentSegmentsBySide);
    }

    for (Map.Entry<Vector3i, LateralVacancySegments> entry : segmentsBySide.entrySet()) {
      Vector3i roadSide = entry.getKey();
      LateralVacancySegments incomingSegments = entry.getValue();
      if (roadSide == null || incomingSegments == null) {
        continue;
      }

      LateralVacancySegments existingSegments = currentSegmentsBySide.get(roadSide);
      if (existingSegments == null) {
        currentSegmentsBySide.put(roadSide.clone(), incomingSegments.clone());
        continue;
      }

      if (existingSegments.getRoadDirectionVec() == null &&
          incomingSegments.getRoadDirectionVec() != null) {
        existingSegments.setRoadDirectionVec(incomingSegments.getRoadDirectionVec().clone());
      }

      if (incomingSegments.getValidSegments() == null) {
        continue;
      }

      for (Segment segment : incomingSegments.getValidSegments()) {
        if (segment != null) {
          existingSegments.appendSegment(segment.clone());
        }
      }
    }
  }

  private void extendRegisteredRoadTopology(
      RoadNode roadNode,
      TopologyStructure streetExtension) {
    if (roadNode == null || roadNode.getRoadId() == null || streetExtension == null) {
      return;
    }

    TopologyStructure existingStreet = this.topology.getTopologyStructureFromStructureId(roadNode.getRoadId());
    TopologyStructure updatedStreet = existingStreet == null ? streetExtension.clone()
        : existingStreet.clone();
    updatedStreet.setStructureId(roadNode.getRoadId());

    if (existingStreet != null) {
      updatedStreet.setTopLeft(new Vector2i(
          Math.min(existingStreet.getMinX(), streetExtension.getMinX()),
          Math.min(existingStreet.getMinZ(), streetExtension.getMinZ())));
      updatedStreet.setBotRight(new Vector2i(
          Math.max(existingStreet.getMaxX(), streetExtension.getMaxX()),
          Math.max(existingStreet.getMaxZ(), streetExtension.getMaxZ())));
    }

    this.topology.unregisterStructure(roadNode.getRoadId());
    this.topology.registerStructure(updatedStreet);
  }

  private Map<Vector3i, LateralVacancySegments> createLateralVacancySegments(
      RoadType type,
      Vector3i startPoint,
      Vector3i roadDirectionVec,
      short length) {
    Map<Vector3i, LateralVacancySegments> lateralVacancySegments = new HashMap<>();
    Vector3i left = Utils.rotateNormalizedVector(roadDirectionVec,
        Rotation.Ninety);
    Vector3i right = Utils.rotateNormalizedVector(roadDirectionVec,
        Rotation.TwoSeventy);

    int size = StructureConfig.Road.getRoadSize(type);
    Vector3i leftSegmentStart = startPoint.clone()
        .add(left.clone().scale(Math.floorDiv(size, 2)))
        .add(left)
        .add(roadDirectionVec);
    Vector3i leftSegmentEnd = leftSegmentStart.clone().add(roadDirectionVec.clone().scale(length - 1));
    Vector3i rightSegmentStart = startPoint.clone()
        .add(right.clone().scale(Math.floorDiv(size, 2)))
        .add(right)
        .add(roadDirectionVec);
    Vector3i rightSegmentEnd = rightSegmentStart.clone().add(roadDirectionVec.clone().scale(length - 1));

    LateralVacancySegments leftSegments = new LateralVacancySegments();
    leftSegments.setRoadDirectionVec(roadDirectionVec.clone());
    leftSegments.appendSegment(new Segment(leftSegmentStart, leftSegmentEnd));

    LateralVacancySegments rightSegments = new LateralVacancySegments();
    rightSegments.setRoadDirectionVec(roadDirectionVec.clone());
    rightSegments.appendSegment(new Segment(rightSegmentStart, rightSegmentEnd));
    lateralVacancySegments.put(left, leftSegments);
    lateralVacancySegments.put(right, rightSegments);
    return lateralVacancySegments;
  }

  private RoadNode createRoadNode(
      RoadNode parent,
      Rotation prefabRotation,
      Vector3i roadDirectionVec,
      Map<Vector3i, LateralVacancySegments> lateralVacancySegments) {
    RoadNode node = parent == null ? new RoadNode() : parent.newChild();
    node.setRoadId(UUID.randomUUID());
    node.setPrefabOrientation(prefabRotation);
    node.setRoadDirectionVec(roadDirectionVec.clone());
    node.setBranchOriginKey(parent == null
        ? roadDirectionVec.clone()
        : parent.getBranchOriginKey().clone());
    node.setState(VillageRoadState.EXPANDIBLE);
    node.setLateralVacancySegments(lateralVacancySegments);
    if (parent == null) {
      node.setDepth((byte) 0);
    }
    return node;
  }

  private void spawnRoadSegmentsAsync(
      RoadNode node,
      RoadType type,
      Vector3i startPoint,
      Rotation prefabRotation,
      Vector3i roadDirectionVec,
      List<RoadLength> orderedSegments) {
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
    Vector3i left = Utils.rotateNormalizedVector(roadDirectionVec,
        Rotation.Ninety);
    Vector3i right = Utils.rotateNormalizedVector(roadDirectionVec,
        Rotation.TwoSeventy);

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

  private RoadNode reserveInitialStructureStreetSegments(
      StructureType structureType,
      Vector3i prefabAnchor,
      Rotation rotation) {
    TopologyStructure structureFootprint = buildTransientStructureFootprint(
        structureType,
        prefabAnchor,
        rotation);
    if (structureFootprint == null) {
      return null;
    }

    for (UUID roadId : this.topology.getRegisteredRoadIdsByDepthDescending()) {
      RoadNode roadNode = this.topology.getRoadFromId(roadId);
      if (roadNode == null || roadNode.getLateralVacancySegments() == null) {
        continue;
      }

      for (LateralVacancySegments segments : roadNode.getLateralVacancySegments().values()) {
        Segment occupiedSegment = buildOccupiedRoadsideSegment(structureFootprint,
            roadNode, segments);
        if (occupiedSegment != null) {
          segments.removeSegment(occupiedSegment);
          return roadNode;
        }
      }
    }
    return null;
  }

  private TopologyStructure buildTransientStreetTopology(
      RoadType type,
      Vector3i startPoint,
      Rotation prefabRotation,
      Vector3i roadDirectionVec,
      short length) {
    return buildStreetTopology(
        UUID.randomUUID(),
        type,
        startPoint,
        prefabRotation,
        roadDirectionVec,
        length);
  }

  private TopologyStructure buildTransientStructureFootprint(
      StructureType structureType,
      Vector3i prefabAnchor,
      Rotation rotation) {
    Vector2i structureSize = Utils.getSize2i(structureType);
    if (structureSize == null || prefabAnchor == null) {
      return null;
    }

    TopologyStructure structureFootprint = new TopologyStructure(
        prefabAnchor.clone(),
        structureSize,
        VillageTopologyStructureType.fromStructureType(structureType),
        rotation == null ? Rotation.None : rotation);
    structureFootprint.setPrefabAnchor(prefabAnchor);
    structureFootprint.setStructureType(structureType);
    return structureFootprint;
  }

  private Segment buildOccupiedRoadsideSegment(
      TopologyStructure structureFootprint,
      RoadNode roadNode,
      LateralVacancySegments segments) {
    if (structureFootprint == null || roadNode == null || segments == null) {
      return null;
    }

    Segment referenceSegment = getReferenceSegment(segments);
    Vector3i roadDirectionVec = roadNode.getRoadDirectionVec();
    if (referenceSegment == null || roadDirectionVec == null) {
      return null;
    }

    int minX = structureFootprint.getMinX();
    int maxX = structureFootprint.getMaxX();
    int minZ = structureFootprint.getMinZ();
    int maxZ = structureFootprint.getMaxZ();
    int y = referenceSegment.getStart().y;

    if (roadDirectionVec.x != 0) {
      int stripZ = referenceSegment.getStart().z;
      if (stripZ < minZ || stripZ > maxZ) {
        return null;
      }
      return new Segment(
          new Vector3i(minX, y, stripZ),
          new Vector3i(maxX, y, stripZ));
    }

    if (roadDirectionVec.z != 0) {
      int stripX = referenceSegment.getStart().x;
      if (stripX < minX || stripX > maxX) {
        return null;
      }
      return new Segment(
          new Vector3i(stripX, y, minZ),
          new Vector3i(stripX, y, maxZ));
    }

    return null;
  }

  private Segment getReferenceSegment(LateralVacancySegments segments) {
    if (segments.getValidSegments() == null) {
      return null;
    }

    for (Segment segment : segments.getValidSegments()) {
      if (segment != null && segment.getStart() != null && segment.getEnd() != null) {
        return segment;
      }
    }

    return null;
  }

  private boolean rollChance(short chancePercent) {
    int clampedChance = Math.max(0, Math.min(100, chancePercent));
    if (clampedChance == 0) {
      return false;
    }
    if (clampedChance == 100) {
      return true;
    }
    return this.random.nextInt(0, 100) < clampedChance;
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        VillageBuilderComponent.getComponentType(),
        VillageHeartComponent.getComponentType(),
        VillageTopologyComponent.getComponentType());
  }
}
