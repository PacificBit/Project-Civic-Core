package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.core.interfaces.IWorkplaceMissingResource;

public class MineComponent implements Component<ChunkStore>, IWorkplaceMissingResource {
  public static final BuilderCodec<MineComponent> CODEC = BuilderCodec
      .builder(MineComponent.class, MineComponent::new)

      .append(new KeyedCodec<Set<ItemRequestCriterion>>(
          "MissingResources",
          new SetCodec<ItemRequestCriterion, HashSet<ItemRequestCriterion>>(
              ItemRequestCriterion.CODEC,
              HashSet::new,
              false)),
          (obj, value) -> obj.missingResources = value,
          obj -> obj.missingResources)
      .add()

      .append(new KeyedCodec<>("MineshaftData", Mineshaft.CODEC),
          (obj, value) -> obj.mineshaft = value,
          (obj) -> obj.mineshaft)
      .add()

      .append(new KeyedCodec<>(
          "WorkerGalleryReservations",
          new MapCodec<UUID, HashMap<String, UUID>>(Codec.UUID_BINARY, HashMap::new)),
          (obj, value) -> obj.workerIdToGalleryId = decodeWorkerGalleryReservations(value),
          obj -> encodeWorkerGalleryReservations(obj.workerIdToGalleryId))
      .add()

      .build();

  private Set<ItemRequestCriterion> missingResources = new HashSet<>();
  private Mineshaft mineshaft;
  private Map<UUID, UUID> workerIdToGalleryId = new HashMap<>();

  public Mineshaft getMineshaft() {
    return mineshaft;
  }

  public void setMineshaft(Mineshaft mineshaftData) {
    this.mineshaft = mineshaftData;
  }

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
    MineComponent component = new MineComponent();
    component.missingResources = new HashSet<>();

    for (ItemRequestCriterion criterion : this.missingResources) {
      component.missingResources.add(criterion != null ? criterion.clone() : null);
    }

    component.mineshaft = this.mineshaft == null ? null : this.mineshaft.clone();
    component.workerIdToGalleryId = new HashMap<>(this.workerIdToGalleryId);
    return component;
  }

  public static ComponentType<ChunkStore, MineComponent> getComponentType() {
    return ProjectCivicCore.get().getMineComponentType();
  }

  private static Map<UUID, UUID> decodeWorkerGalleryReservations(Map<String, UUID> encoded) {
    Map<UUID, UUID> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    encoded.forEach((workerId, galleryId) -> decoded.put(UUID.fromString(workerId), galleryId));
    return decoded;
  }

  private static HashMap<String, UUID> encodeWorkerGalleryReservations(Map<UUID, UUID> decoded) {
    HashMap<String, UUID> encoded = new HashMap<>();

    if (decoded == null) {
      return encoded;
    }

    decoded.forEach((workerId, galleryId) -> encoded.put(workerId.toString(), galleryId));
    return encoded;
  }

}
