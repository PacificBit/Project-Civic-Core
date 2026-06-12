package com.pacificbytestudios.projectciviccore.village.components.builder;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;

@SerializableClass
public class VillageBuilderComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageBuilderComponent> CODEC = VillageBuilderComponentCodec.CODEC;

  @SerialName("ProjectQueue")
  VillageProjectPlan projectPlan;

  @SerialName("IsFirstVillageBuildProject")
  Boolean firstVillageBuildProject = false;
  Boolean haveFirstRoadsBeenBuilt = false;

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
    return VillageBuilderComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, VillageBuilderComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageBuilderComponentType();
  }
}
