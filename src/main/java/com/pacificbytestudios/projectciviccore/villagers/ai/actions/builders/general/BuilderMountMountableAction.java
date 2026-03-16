package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.MountMountableAction;

public class BuilderMountMountableAction extends BuilderActionBase {
  private EnumHolder<WorkplaceInteractibles> interactableType = new EnumHolder<>();
  private EnumHolder<StructureType> structureType = new EnumHolder<>();

  @Override
  public Action build(BuilderSupport support) {
    return new MountMountableAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Mount on target mountable";
  }

  @Override
  public String getShortDescription() {
    return "Mount on target mountable";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.requireEnum(data, "StructureType", this.structureType, StructureType.class,
        BuilderDescriptorState.Experimental, "Structure type", null);

    this.requireEnum(data, "InteractableType", this.interactableType, WorkplaceInteractibles.class,
        BuilderDescriptorState.Experimental, "Workplace Ineractable type", null);

    return this;
  }

  public WorkplaceInteractibles getInteractibleType(BuilderSupport support) {
    return this.interactableType.get(support.getExecutionContext());
  }

  public StructureType getStructureType(BuilderSupport support) {
    return this.structureType.get(support.getExecutionContext());
  }
}
