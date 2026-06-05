package com.pacificbytestudios.projectciviccore.village.commands.coresubcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.RoadsGraph.RoadNode;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.DespawnFunction;
import com.pacificbytestudios.projectciviccore.utils.DespawnIncompleteConstructionFunction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudios.projectciviccore.village.core.spawner.RoadSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.TownHeartSpawner;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public class Despawn extends VillageCommand {

  private byte currentStep, totalSteps = 7;

  public Despawn() {
    super("despawn", "server.system.village.despawn.desc", true, true);
  }

  /**
   * It tries to despawn the village the player is currently in
   *
   * @param exeContext Contains the command context and the player, current
   *                   position, village ref
   */
  @Override
  public void execute(ExecutionContext exeContext) {
    this.currentStep = 1;
    VillageHeartComponent heart = this.getVillageHeart(exeContext);
    World world = exeContext.world();

    if (heart == null) {
      return;
    }
    VillageTopologyComponent topology = heart.getComponent(VillageTopologyComponent.getComponentType());

    // ==============================================================
    // Step 1 - despawn houses
    // ==============================================================
    despawnHouses(exeContext, heart, world);

    // ==============================================================
    // Step 2 - despawn work in progress constructions
    // ==============================================================
    despawnIncompleteBuildings(exeContext, heart, topology, world);

    // ==============================================================
    // Step 3 - despawn workplaces
    // ==============================================================
    despawnWorkplaces(exeContext, heart, world);

    // ==============================================================
    // Step 4 - despawn roads
    // ==============================================================
    despawnRoads(exeContext, heart, topology, world);

    // ==============================================================
    // Step 5 - despawn villagers
    // ==============================================================
    despawnVillagers(exeContext, heart, world);

    // ==============================================================
    // Step 6 - despawn city center
    // ==============================================================
    despawnCityCenter(exeContext, heart, topology, world);

    // ==============================================================
    // Step 7 - despawn trigger volume
    // ==============================================================
    despawnTriggerVolume(exeContext, heart, world);
  }

  private void despawnHouses(ExecutionContext exeContext, VillageHeartComponent heart, World world) {
    sendExecutionStepInfo(exeContext, this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.houses.start");
    var housesEntrySet = heart.getHouseIdToHouse().entrySet();
    for (var entry : housesEntrySet) {
      HouseComponent house = entry.getValue();
      DespawnFunction despawnFunction = Utils.getDespawnFunction(StructureType.HOUSE);
      Vec3i benchPos = house.getCoreBlockPosition();
      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(benchPos.x, benchPos.z));
      despawnFunction.apply(chunk, benchPos);
    }
    sendExecutionStepInfo(exeContext, this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.done");
  }

  private void despawnIncompleteBuildings(ExecutionContext exeContext, VillageHeartComponent heart,
      VillageTopologyComponent topology, World world) {
    sendExecutionStepInfo(exeContext, ++this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.incompleteBuildings.start");
    Ref<ChunkStore> townHallRef = heart.getTownHall().getWorkplaceRef().getRef();
    TownHallComponent townHall = exeContext.chunkStore().getComponent(
        townHallRef,
        TownHallComponent.getComponentType());
    var projectPlan = townHall.getVillageProjectPlan();

    if (projectPlan != null) {
      var projectList = projectPlan.getProjectList();
      for (VillageProject project : projectList) {
        DespawnIncompleteConstructionFunction despawn = Utils.getDespawnIncompleteConstructionFunction(
            project.getStructureType());
        TopologyStructure structureTopology = topology.getTopologyStructureFromStructureId(project.getStructureId());
        Vec3i anchor = structureTopology.getPrefabAnchor();
        WorldChunk chunk = Utils.getWorldChunk(world, anchor);
        despawn.apply(chunk, anchor, project.getTargetLevel(), structureTopology.getOrientation());
      }
    }

    sendExecutionStepInfo(exeContext, this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.done");
  }

  private void despawnWorkplaces(ExecutionContext exeContext, VillageHeartComponent heart, World world) {
    sendExecutionStepInfo(exeContext, ++this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.workplaces.start");
    var workplaceEntrySet = heart.getWorkplaceIdToWorkplace().entrySet();
    for (var entry : workplaceEntrySet) {
      WorkplaceComponent workplace = entry.getValue();
      DespawnFunction despawnFunction = Utils.getDespawnFunction(workplace.getWorkplaceType());
      Vec3i benchPos = workplace.getBenchPos();
      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(benchPos.x, benchPos.z));
      despawnFunction.apply(chunk, benchPos);
    }
    sendExecutionStepInfo(exeContext, this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.done");
  }

  private void despawnRoads(
      ExecutionContext exeContext,
      VillageHeartComponent heart,
      VillageTopologyComponent topology,
      World world) {
    sendExecutionStepInfo(exeContext, ++this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.roads.start");
    WorldChunk chunk = Utils.getWorldChunk(world, heart.getEntityRef().getPosition());
    for (var entry : topology.getRoadIdToRoad().entrySet()) {
      RoadNode road = entry.getValue();
      for (var segment : road.getRoadSegments()) {
        chunk = Utils.getWorldChunk(world, chunk, segment);
        RoadSpawner.despawn(chunk, segment);
      }
    }
    sendExecutionStepInfo(exeContext, this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.done");
  }

  private void despawnVillagers(ExecutionContext exeContext, VillageHeartComponent heart, World world) {
    sendExecutionStepInfo(exeContext, ++this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.villagers.start");
    for (var entry : heart.getVillagerIdToComponent().entrySet()) {
      Ref<EntityStore> villager = world.getEntityRef(entry.getKey());
      exeContext.entityStore().removeEntity(villager, RemoveReason.REMOVE);
    }
    sendExecutionStepInfo(exeContext, this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.done");
  }

  private void despawnCityCenter(ExecutionContext exeContext, VillageHeartComponent heart,
      VillageTopologyComponent topology,
      World world) {
    sendExecutionStepInfo(exeContext, ++this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.cityCenter.start");

    Rotation rotation = topology.getVillageOrientation();

    Vec3i position = Vec3i.safeDown().add(heart.getBlockContext().getPosition());
    WorldChunk chunk = Utils.getWorldChunk(world, position);
    TownHeartSpawner.despawn(chunk, position, rotation);

    sendExecutionStepInfo(exeContext, this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.done");
  }

  private void despawnTriggerVolume(ExecutionContext exeContext, VillageHeartComponent heart, World world) {
    sendExecutionStepInfo(exeContext, ++this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.triggerVolume.start");

    heart.getTriggerVolumeEntry().markPendingDestroy();

    sendExecutionStepInfo(exeContext, this.currentStep, this.totalSteps,
        "server.system.village.despawn.step.done");
  }

  private void sendExecutionStepInfo(ExecutionContext exeContext, byte currentStep, byte totalSteps,
      String translationKey) {
    CommandUtils.sendExecutionResultMessage(
        exeContext.cmdContext(),
        ExecutionResult.Info,
        Message.translation(translationKey)
            .param("currentStep", currentStep)
            .param("totalSteps", totalSteps));
  }

}
