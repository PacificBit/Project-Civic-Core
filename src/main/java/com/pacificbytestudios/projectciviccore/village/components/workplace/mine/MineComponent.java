package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.core.interfaces.IWorkplaceMissingResource;
import lombok.Getter;
import lombok.Setter;

@SerializableClass
public class MineComponent implements Component<ChunkStore>, IWorkplaceMissingResource {
  public static final BuilderCodec<MineComponent> CODEC = MineComponentCodec.CODEC;

  Set<ItemRequestCriterion> missingResources = new HashSet<>();

  @Getter
  @Setter
  @SerialName("MineshaftData")
  Mineshaft mineshaft;

  @SerialName("WorkerGalleryReservations")
  Map<UUID, UUID> workerIdToGalleryId = new HashMap<>();

  public boolean hasGalleryReservedTo(UUID workerId) {
    return workerIdToGalleryId.containsKey(workerId);
  }

  public UUID getFirstAvailableGalleryId() {
    return this.mineshaft == null ? null : this.mineshaft.getFirstAvailableGalleryId();
  }

  public boolean registerAsGalleryWorker(UUID workerId, UUID galleryId) {
    boolean isGalleryAlreadyTaken = this.workerIdToGalleryId.entrySet()
        .stream()
        .map(Map.Entry::getValue)
        .filter(galleryId::equals).count() > 0;

    if (isGalleryAlreadyTaken) {
      return false;
    }

    this.workerIdToGalleryId.put(workerId, galleryId);
    return true;
  }

  public UUID unregisterFromGallery(UUID workerId) {
    if (!this.workerIdToGalleryId.containsKey(workerId)) {
      return null;
    }

    return this.workerIdToGalleryId.remove(workerId);
  }

  public boolean markGalleryAsCompleted(UUID galleryId) {
    return this.mineshaft.markGalleryAsCompleted(galleryId);
  }

  public MineshaftGallery getWorkerAllocatedGallery(UUID workerId) {
    UUID galleryId = this.workerIdToGalleryId.get(workerId);
    if (galleryId == null) {
      return null;
    }
    return this.mineshaft.getGalleryFromId(galleryId);
  }

  @Override
  public Set<ItemRequestCriterion> getMissingResources() {
    return missingResources;
  }

  @Override
  public void newMissingResouceBuffer() {
    this.missingResources = new HashSet<>();
  }

  @Override
  public void setMissingResource(List<ItemRequestCriterion> criterion) {
    this.missingResources.addAll(criterion);
  }

  @Override
  public void addMissingResource(ItemRequestCriterion criterion) {
    this.missingResources.add(criterion);
  }

  @Override
  public void resetMissingResources() {
    this.newMissingResouceBuffer();
  }

  @Override
  public MineComponent clone() {
    return MineComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, MineComponent> getComponentType() {
    return ProjectCivicCore.get().getMineComponentType();
  }

}
