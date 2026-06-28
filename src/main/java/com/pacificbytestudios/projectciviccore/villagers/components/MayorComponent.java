package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;

import lombok.Getter;
import lombok.Setter;

@SerializableClass
public class MayorComponent implements Component<EntityStore> {
  public static final BuilderCodec<MayorComponent> CODEC = MayorComponentCodec.CODEC;

  @Getter
  @SerialName("ExecutingProjectPlan")
  VillageProjectPlan executingVillagePlan;
  @Getter
  @Setter
  @SerialName("CurrentlProject")
  VillageProject currentProject;

  public void setExecutingVillagePlan(VillageProjectPlan villagePlanToCheck) {
    this.executingVillagePlan = villagePlanToCheck == null ? null : villagePlanToCheck.clone();
    this.currentProject = null;
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
