package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;

public class MayorComponent implements Component<EntityStore> {
  public static final BuilderCodec<MayorComponent> CODEC = BuilderCodec
      .builder(MayorComponent.class, MayorComponent::new)

      .append(new KeyedCodec<>(
          "ExecutingProjectPlan",
          VillageProjectPlan.CODEC),
          (obj, value) -> obj.executingVillagePlan = value,
          (obj) -> obj.executingVillagePlan)
      .add()

      .append(new KeyedCodec<>(
          "CurrentlProject",
          VillageProject.CODEC),
          (obj, value) -> obj.currentProject = value,
          (obj) -> obj.currentProject)
      .add()

      .build();

  private VillageProjectPlan executingVillagePlan;
  private VillageProject currentProject;

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
    MayorComponent component = new MayorComponent();
    component.executingVillagePlan = this.executingVillagePlan == null ? null : this.executingVillagePlan.clone();
    component.currentProject = this.currentProject == null ? null : this.currentProject.clone();
    return component;
  }

  public static ComponentType<EntityStore, MayorComponent> getComponentType() {
    return ProjectCivicCore.get().getMayorComponentType();
  }
}
