package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudio.codecbutler.annotations.SerialName;

@SerializableClass
public class MayorComponent implements Component<EntityStore> {
  public static final BuilderCodec<MayorComponent> CODEC = MayorComponentCodec.CODEC;

  @SerialName("ExecutingProjectPlan")
  VillageProjectPlan executingVillagePlan;
  @SerialName("CurrentlProject")
  VillageProject currentProject;

  public VillageProjectPlan getExecutingVillagePlan() {
    return executingVillagePlan;
  }

  public void setExecutingVillagePlan(VillageProjectPlan villagePlanToCheck) {
    this.executingVillagePlan = villagePlanToCheck == null ? null : villagePlanToCheck.clone();
    this.currentProject = null;
  }

  public VillageProject getCurrentProject() {
    return currentProject;
  }

  public void setCurrentProject(VillageProject currentProject) {
    this.currentProject = currentProject;
  }

  public boolean hasProject() {
    return this.currentProject != null ||
        (this.executingVillagePlan != null && this.executingVillagePlan.hasProjects());
  }

  @Override
  public MayorComponent clone() {
    return MayorComponentCodec.clone(this);
  }

  public static ComponentType<EntityStore, MayorComponent> getComponentType() {
    return ProjectCivicCore.get().getMayorComponentType();
  }
}
