package com.pacificbytestudios.projectciviccore.village.components.builder;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;

public class VillageBuilderComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageBuilderComponent> CODEC = BuilderCodec
      .builder(VillageBuilderComponent.class, VillageBuilderComponent::new)

      .append(new KeyedCodec<>("ProjectQueue", VillageProjectPlan.CODEC),
          (obj, value) -> obj.projectPlan = value,
          obj -> obj.projectPlan)
      .add()

      .append(new KeyedCodec<>("IsFirstVillageBuildProject", Codec.BOOLEAN),
          (obj, value) -> obj.firstVillageBuildProject = value,
          (obj) -> obj.firstVillageBuildProject)
      .add()

      .append(new KeyedCodec<>("HaveFirstRoadsBeenBuilt", Codec.BOOLEAN),
          (obj, value) -> obj.haveFirstRoadsBeenBuilt = value,
          (obj) -> obj.haveFirstRoadsBeenBuilt)
      .add()

      .build();

  private VillageProjectPlan projectPlan;
  private Boolean firstVillageBuildProject = false;
  private Boolean haveFirstRoadsBeenBuilt = false;

  public boolean hasProject() {
    return this.projectPlan != null && this.projectPlan.hasProjects();
  }

  public VillageProject peekNext() {
    return this.projectPlan == null ? null : this.projectPlan.peekNextProject();
  }

  public VillageProject pollNextProject() {
    return this.projectPlan == null ? null : this.projectPlan.pollNextProject();
  }

  public VillageProjectPlan getProjectPlan() {
    return this.projectPlan;
  }

  public void setProjectPlan(VillageProjectPlan plan) {
    this.projectPlan = plan;
  }

  public void addProject(VillageProject project) {
    if (project == null) {
      return;
    }

    if (this.projectPlan == null) {
      this.projectPlan = new VillageProjectPlan();
    }

    this.projectPlan.addProject(project);
  }

  public Boolean isFirstVillageBuildProject() {
    return firstVillageBuildProject;
  }

  public void setFirstVillageBuildProject(Boolean firstVillageBuildProject) {
    this.firstVillageBuildProject = firstVillageBuildProject;
  }

  public Boolean getHaveFirstRoadsBeenBuilt() {
    return haveFirstRoadsBeenBuilt;
  }

  public void setHaveFirstRoadsBeenBuilt(Boolean haveFirstRoadsBeenBuilt) {
    this.haveFirstRoadsBeenBuilt = haveFirstRoadsBeenBuilt;
  }

  @Override
  public VillageBuilderComponent clone() {
    VillageBuilderComponent component = new VillageBuilderComponent();
    component.projectPlan = this.projectPlan == null ? null : this.projectPlan.clone();
    component.firstVillageBuildProject = this.firstVillageBuildProject;
    component.haveFirstRoadsBeenBuilt = this.haveFirstRoadsBeenBuilt;
    return component;
  }

  public static ComponentType<ChunkStore, VillageBuilderComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageBuilderComponentType();
  }
}
