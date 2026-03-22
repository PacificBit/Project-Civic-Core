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
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.utils.SpawnFunction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudios.projectciviccore.village.core.spawner.RoadSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadLength;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadType;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;

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
      handleFirstVillageProjectPlan();
      return;
    }

    if (!builder.hasProject()) {
      System.out.println("NO MORE PROJECTS TO DO");
      world.execute(() -> store.removeComponent(villageRef,
          VillageBuilderComponent.getComponentType()));
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

  private void handleFirstVillageProjectPlan() {
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
        && this.project.getStructureType() != StructureType.HOUSE) {
      return;
    }

    SpawnFunction spawnFunction = Utils.getSpawnFunction(project.getStructureType());

    Vector3i spawningPosition = calculateInitialSpawingPosition(heartTopology);
    if (this.project.getStructureType() == StructureType.HOUSE) {
      int houseIndex = firstVillageHouseSpawnCounter.getOrDefault(heart.getUuid(), 0);
      firstVillageHouseSpawnCounter.put(heart.getUuid(), houseIndex + 1);
    }
    Rotation rotation = calculateInitialSpawningRotation(heartTopology, spawningPosition);
    spawningPosition.setY(heartTopology.getY());

    Vector3i queuedSpawningPosition = spawningPosition.clone();
    this.world.execute(() -> {
      WorldChunk chunk = Utils.getWorldChunk(this.world, queuedSpawningPosition);
      spawnFunction.apply(chunk, queuedSpawningPosition, rotation, this.heart);
    });
  }

  private Vector3i calculateInitialSpawingPosition(
      TopologyStructure heartTopology) {
    return switch (this.project.getStructureType()) {
      case TOWN_HALL -> calculateTownHallPosition(heartTopology);
      case HOUSE -> calculateInitialHousesPosition(
          heartTopology,
          firstVillageHouseSpawnCounter.getOrDefault(heart.getUuid(), 0));
      // case TAVERN -> calculateTavernPosition();
      // case STORAGE -> calculateStoragePosition(heartTopology, orientationVec);
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

  private void handleFirstVillageRoads(TopologyStructure heart) {
    buildTopRoad(heart);
    buildLeftRoad(heart);
    buildRightRoad(heart);
    buildBottomRoad(heart);
  }

  private void buildTopRoad(TopologyStructure heart) {
    Vector3i topRoadRotationVector = heart.getForwardVector();

    int townHallWidth = StructureConfig.TownHall.SIZE_2I.x;
    int roadWidth = StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD);
    int halfRoadSize = Math.floorDiv(roadWidth, 2);
    int townHallRightOffset = Math.floorDiv(townHallWidth, 2) + halfRoadSize + 1;

    Vector3i startingPoint = heart.getFrontEdge()
        .add(heart.getLeftVector().scale(townHallRightOffset));

    buildRoad(
        RoadType.MAIN_ROAD,
        startingPoint,
        heart.getOrientation(),
        topRoadRotationVector,
        (short) 11);
  }

  private void buildBottomRoad(TopologyStructure heart) {
    Vector3i bottomRoadRotationVector = heart.getBackVector();
    Vector3i bottomRoadStartPoint = heart.getBackEdge();

    buildRoad(
        RoadType.MAIN_ROAD,
        bottomRoadStartPoint,
        heart.getOrientation().add(Rotation.OneEighty),
        bottomRoadRotationVector,
        (short) 11);
  }

  private void buildLeftRoad(TopologyStructure heart) {
    Vector3i leftRoadRotationVector = heart.getLeftVector();
    Vector3i leftEdge = heart.getLeftEdge();

    int halfRoadSize = Math.floorDiv(StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD), 2);
    int halfDepth = Math.floorDiv(heart.getDepth(), 2) - halfRoadSize;
    Vector3i startingPoint = leftEdge.add(heart.getBackVector().scale(halfDepth));

    buildRoad(
        RoadType.MAIN_ROAD,
        startingPoint,
        heart.getOrientation().add(Rotation.Ninety),
        leftRoadRotationVector,
        (short) 11);
  }

  private void buildRightRoad(TopologyStructure heart) {
    Vector3i rightRoadRotationVector = heart.getRightVector();
    Vector3i rightEdge = heart.getRightEdge();

    int halfRoadSize = Math.floorDiv(StructureConfig.Road.getRoadSize(RoadType.MAIN_ROAD), 2);
    int halfDepth = Math.floorDiv(heart.getDepth(), 2) - halfRoadSize;
    Vector3i startingPoint = rightEdge.add(heart.getBackVector().scale(halfDepth));

    buildRoad(
        RoadType.MAIN_ROAD,
        startingPoint,
        heart.getOrientation().add(Rotation.TwoSeventy),
        rightRoadRotationVector,
        (short) 11);
  }

  private void buildRoad(RoadType type, Vector3i startPoint, Rotation rotation,
      Vector3i rotationVec, short length) {
    Map<RoadLength, Short> sectionsToBuild = calculateNeededRoadSections(length);
    List<RoadLength> orderedSegments = expandRoadSections(sectionsToBuild);

    if (orderedSegments.isEmpty()) {
      return;
    }

    this.world.execute(() -> {
      RoadLength firstSegment = orderedSegments.get(0);

      Vector3i currentPosition = startPoint.clone()
          .add(rotationVec.clone().scale(firstSegment.getLength()));

      for (int i = 0; i < orderedSegments.size(); i++) {
        RoadLength currentSegment = orderedSegments.get(i);

        WorldChunk chunk = Utils.getWorldChunk(this.world, currentPosition);
        RoadSpawner.spawn(chunk, currentPosition, rotation, this.heart, type, currentSegment);

        if (i + 1 < orderedSegments.size()) {
          RoadLength nextSegment = orderedSegments.get(i + 1);
          currentPosition = currentPosition.clone()
              .add(rotationVec.clone().scale(nextSegment.getLength()));
        }
      }
    });
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
