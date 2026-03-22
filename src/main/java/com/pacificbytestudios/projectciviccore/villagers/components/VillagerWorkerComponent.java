package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

public class VillagerWorkerComponent implements Component<EntityStore> {
  public static final BuilderCodec<VillagerWorkerComponent> CODEC = BuilderCodec
      .builder(VillagerWorkerComponent.class, VillagerWorkerComponent::new)

      .append(new KeyedCodec<>("Workplace", WorkplaceComponent.CODEC),
          (obj, value) -> obj.workplace = value,
          (obj) -> obj.workplace)
      .add()

      .append(
          new KeyedCodec<>("TypeOfWorker", new EnumCodec<WorkType>(WorkType.class)),
          (obj, value) -> obj.typeOfWorker = value,
          (obj) -> obj.typeOfWorker)
      .add()

      .append(new KeyedCodec<>("WorkplaceAreaSnapshot", WorkplaceAreaSnapshot.CODEC),
          VillagerWorkerComponent::setWorkareaSnapshot,
          (obj) -> obj.workareaSnapshot)
      .add()

      .build();

  private WorkplaceComponent workplace;
  private WorkType typeOfWorker;
  private WorkplaceAreaSnapshot workareaSnapshot = new WorkplaceAreaSnapshot();

  @Override
  public VillagerWorkerComponent clone() {
    VillagerWorkerComponent component = new VillagerWorkerComponent();
    component.workplace = this.workplace.clone();
    component.typeOfWorker = this.typeOfWorker;
    component.workareaSnapshot = this.getWorkareaSnapshot().clone();
    return component;
  }

  public WorkplaceComponent getWorkplace() {
    return workplace;
  }

  public void setWorkplace(WorkplaceComponent workplace) {
    this.workplace = workplace;
  }

  public WorkType getTypeOfWorker() {
    return typeOfWorker;
  }

  public void setTypeOfWorker(WorkType typeOfWorker) {
    this.typeOfWorker = typeOfWorker;
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
