package com.pacificbytestudios.projectciviccore.village.commands.markercommands.subcommands;

import java.util.UUID;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartDebugComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsSessionUtil;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;

public class ToggleWaypoints extends VillageCommand {

  private final FlagArg showWorkerWaypoints;

  public ToggleWaypoints() {
    super("toggle-wp", "server.system.debug.toggleWp.desc");
    this.showWorkerWaypoints = this.withFlagArg(
        "show-worker-waypoints",
        "server.system.debug.toggleWp.showWorkerWaypoints.desc");
  }

  @Override
  public void execute(ExecutionContext exeContext) {
    VillageHeartComponent heart = this.getVillageHeart(exeContext);
    if (heart == null) {
      return;
    }

    if (!heart.getConfig().isDebugActive()) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Err,
          Message.translation("server.system.debug.err.debugNotEnable"));
      return;
    }

    VillageHeartDebugComponent debug = heart.getComponent(VillageHeartDebugComponent.getComponentType());
    if (debug == null) {
      return;
    }

    // A real toggle: if anything is currently shown, hide everything; otherwise show.
    if (debug.hasStructureMarkers() || debug.isDisplayWorkerSnapshots()) {
      debug.clearStructureWaypoints();
      debug.clearSnapshotWaypoints();
      debug.setDisplayWorkerSnapshots(false);
    } else {
      VillageTopologyComponent topology = heart.getComponent(VillageTopologyComponent.getComponentType());
      if (topology == null) {
        return;
      }

      debug.setDisplayWorkerSnapshots(exeContext.cmdContext().get(this.showWorkerWaypoints));
      heart.getWorkplaceIdToWorkplace().keySet()
          .forEach(structureId -> registerStructure(debug, heart, topology, structureId));
      heart.getHouseIdToHouse().keySet()
          .forEach(structureId -> registerStructure(debug, heart, topology, structureId));
    }

    debug.drawAll(exeContext.world());
  }

  /**
   * Builds the structure's waypoints through the same population the editing
   * session uses so
   * the debug view matches the editing view exactly, then publishes them to the
   * debug
   * component keyed by structure.
   */
  private void registerStructure(
      VillageHeartDebugComponent debug,
      VillageHeartComponent heart,
      VillageTopologyComponent topology,
      UUID structureId) {
    TopologyStructure structure = topology.getTopologyStructureFromStructureId(structureId);
    if (structure == null) {
      return;
    }

    WaypointsEditingComponent waypoints = new WaypointsEditingComponent();
    waypoints.setStructureId(structureId);
    if (!WaypointsSessionUtil.populateWaypoints(waypoints, heart, structure)) {
      return;
    }

    debug.setStructureWaypoints(
        structureId,
        WaypointsSessionUtil.toMarkerListMap(waypoints.getStructureWaypoints()));
  }

}
