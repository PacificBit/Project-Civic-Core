package com.pacificbytestudios.projectciviccore.village.commands.debugcommands.subcommands;

import java.util.Collection;

import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;

public class Structures extends VillageCommand {

    public Structures() {
        super("structures", "Debug structured");
    }

    @Override
    public void execute(ExecutionContext exeContext) {
        World world = exeContext.world();
        VillageTopologyComponent topology = CommandUtils.getVillageTopologyComponent(exeContext);

        Collection<TopologyStructure> structures = topology.getRegisteredTopologyStructures().values();
        for (TopologyStructure structure : structures) {
            structure.debug(world, DrawUtils.COLOR_BLUE);
        }
    }
}
