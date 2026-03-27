package com.pacificbytestudios.projectciviccore.village.components.workplace.townhall;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;

public class TownHallComponent implements Component<ChunkStore> {

  public static final BuilderCodec<TownHallComponent> CODEC = BuilderCodec
      .builder(TownHallComponent.class, TownHallComponent::new)

      .append(new KeyedCodec<>("LastDayVillageAssessment", Codec.INTEGER),
          (obj, value) -> obj.lastDayVillageAssessment = value,
          (obj) -> obj.lastDayVillageAssessment)
      .add()

      .append(new KeyedCodec<>(
          "VillageProjectPlan",
          VillageProjectPlan.CODEC),
          (obj, value) -> obj.villageProjectPlan = value,
          (obj) -> obj.villageProjectPlan)
      .add()

      .build();

  private Integer lastDayVillageAssessment = -1;
  private VillageProjectPlan villageProjectPlan;

  public Integer getLastDayVillageAssessment() {
    return lastDayVillageAssessment;
  }

  public void setLastDayVillageAssessment(Integer lastDayVillageAssessment) {
    this.lastDayVillageAssessment = lastDayVillageAssessment;
  }

  public VillageProjectPlan getVillageProjectPlan() {
    return villageProjectPlan;
  }

  public void setVillageProjectPlan(VillageProjectPlan villageProjectPlan) {
    this.villageProjectPlan = villageProjectPlan;
  }

  public boolean updateProjectPlan(VillageProject project) {
    if (this.villageProjectPlan == null) {
      return false;
    }
    return this.villageProjectPlan.updateProjectPlan(project);
  }

  public void removeProject(VillageProject project) {
    if (this.villageProjectPlan == null) {
      return;
    }
    this.villageProjectPlan.removeProject(project);
  }

  public TownHallComponent clone() {
    TownHallComponent component = new TownHallComponent();
    component.lastDayVillageAssessment = this.lastDayVillageAssessment;
    component.villageProjectPlan = this.villageProjectPlan == null ? null : this.villageProjectPlan.clone();
    return component;
  }

  public static ComponentType<ChunkStore, TownHallComponent> getComponentType() {
    return ProjectCivicCore.get().getTownHallComponentType();
  }
}
