package com.pacificbytestudios.projectciviccore.village.commands.structurecommands.subcommands;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject.Weight;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveStatus;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;

public class SpawnStructure extends VillageCommand {

  @Nonnull
  private final RequiredArg<String> structureTypeWrapper = this.withRequiredArg("type", 
    "server.system.structure.spawn.param.type", ArgTypes.STRING);

  @Nonnull
  private final OptionalArg<Integer> levelWrapper = this.withOptionalArg("level",
      "server.system.structure.spawn.param.level", ArgTypes.INTEGER);

  @Nonnull
  private final OptionalArg<Boolean> completeStructure = this.withOptionalArg("complete",
      "server.system.structure.spawn.param.complete", ArgTypes.BOOLEAN);

  public SpawnStructure() {
    super("spawn", "server.system.structure.spawn.desc");
  }

  @Override
  public void execute(ExecutionContext exeContext) {
    Optional<StructureType> optionalStructureType = StructureType.from(
        this.structureTypeWrapper.get(exeContext.cmdContext()));

    if (optionalStructureType.isEmpty()) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Err,
          Message.translation("server.system.structure.spawn.error.missingType")
              .param("structureType", StructureType.getTypes()));
      return;
    }

    StructureType structureType = optionalStructureType.get();
    Optional<Integer> structureLevel = Optional.ofNullable(this.levelWrapper.get(exeContext.cmdContext()));
    Optional<Boolean> completed = Optional.ofNullable(this.completeStructure.get(exeContext.cmdContext()));
    VillageHeartComponent heart = this.getVillageHeart(exeContext);
    if (heart == null) {
      return;
    }

    ConstructionMode constructionMode = completed.orElse(true) ? ConstructionMode.COMPLETED : ConstructionMode.STAGED;

    VillageObjectiveType objectiveType = VillageObjectiveType.getObjectiveTypeFrom(structureType);
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setStructureId(UUID.randomUUID());
    project.setWeight(Weight.TOP);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setObjectiveType(objectiveType);
    project.setStructureType(structureType);
    project.setTargetLevel(structureLevel.orElse(1));
    project.setTotalStages(objectiveType.getStages());
    project.setConstructionMode(constructionMode);

    VillageBuilderComponent builder = heart.getComponent(VillageBuilderComponent.getComponentType());
    if (builder == null) {
      builder = new VillageBuilderComponent();
    }

    builder.addProject(project);
    Ref<ChunkStore> villageRef = exeContext.village().getRef();
    exeContext.chunkStore().putComponent(villageRef, VillageBuilderComponent.getComponentType(), builder);

      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Ok,
          "server.system.structure.spawn.success");
  }

}
