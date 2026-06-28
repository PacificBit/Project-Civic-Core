package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudio.codecbutler.annotations.SerialEncode;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import lombok.Getter;
import lombok.Setter;

@SerializableClass
public class VillagerWorkerComponent implements Component<EntityStore> {
  public static final BuilderCodec<VillagerWorkerComponent> CODEC = VillagerWorkerComponentCodec.CODEC;

  @Getter
  @Setter
  WorkplaceComponent workplace;
  @Getter
  @Setter
  WorkType typeOfWorker;
  @SerialName("WorkplaceAreaSnapshot")
  @SerialEncode("(obj, value) -> obj.setWorkareaSnapshot(value)")
  WorkplaceAreaSnapshot workareaSnapshot = new WorkplaceAreaSnapshot();

  @Override
  public VillagerWorkerComponent clone() {
    return VillagerWorkerComponentCodec.clone(this);
  }

  public WorkplaceAreaSnapshot getWorkareaSnapshot() {
    if (this.workareaSnapshot == null) {
      this.workareaSnapshot = new WorkplaceAreaSnapshot();
    }
    return this.workareaSnapshot;
  }

  public void setWorkareaSnapshot(WorkplaceAreaSnapshot workareaSnapshot) {
    this.workareaSnapshot = workareaSnapshot == null ? new WorkplaceAreaSnapshot() : workareaSnapshot;
  }

  @Override
  public String toString() {
    return "VillagerWorkerComponent{" +
        "workplace=" + Utils.safeToString(workplace) +
        ", typeOfWorker=" + Utils.safeToString(typeOfWorker) +
        ", workareaSnapshot=" + Utils.safeToString(workareaSnapshot) +
        '}';
  }

}
