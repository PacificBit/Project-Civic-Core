package com.pacificbytestudios.projectciviccore.village.components.workplace.townhall;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class TownHallComponent implements Component<ChunkStore> {

  public static final BuilderCodec<TownHallComponent> CODEC = TownHallComponentCodec.CODEC;

  Integer lastDayVillageAssessment = -1;
  VillageProjectPlan villageProjectPlan;

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
    return TownHallComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, TownHallComponent> getComponentType() {
    return ProjectCivicCore.get().getTownHallComponentType();
  }
}
