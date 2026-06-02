package com.pacificbytestudios.projectciviccore.village.commands.debugcommands.subcommands;

import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.LateralVacancySegments;
import com.pacificbytestudios.projectciviccore.codecs.LateralVacancySegments.Segment;
import com.pacificbytestudios.projectciviccore.codecs.RoadsGraph.RoadNode;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;

public class Roads extends VillageCommand {

    public Roads() {
        super("roads", "Roads debug");
    }

    @Override
    public void execute(ExecutionContext exeContext) {
        World world = exeContext.world();
        VillageTopologyComponent topology = CommandUtils.getVillageTopologyComponent(exeContext);

        List<UUID> roadIds = topology.getRegisteredRoadIdsByDepthDescending();
        for (UUID roadId : roadIds) {
            RoadNode road = topology.getRoadFromId(roadId);
            road.debug(world, DrawUtils.COLOR_MAGENTA);

            for (LateralVacancySegments segments : road.getLateralVacancySegments().values()) {
                for (Segment validSegment : segments.getValidSegments()) {
                    validSegment.debug(world, DrawUtils.COLOR_BLUE);
                }
            }
        }
    }
}
