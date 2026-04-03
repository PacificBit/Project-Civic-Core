package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;

public class EnqueueVillageStorageRequestTask extends AVillageTask {
  public static final String NO_STORAGE_FOUND = "NO_STORAGE_FOUND";
  public static final String REQUEST_REGISTERED = "REQUEST_REGISTERED";

  public static final BuilderCodec<EnqueueVillageStorageRequestTask> CODEC = BuilderCodec
      .builder(EnqueueVillageStorageRequestTask.class, EnqueueVillageStorageRequestTask::new)

      .append(new KeyedCodec<>("Request", VillageStorageRequest.CODEC),
          (obj, value) -> obj.request = value,
          obj -> obj.request)
      .add()

      .append(
          new KeyedCodec<>(
              "TriedStorageIds",
              new SetCodec<UUID, HashSet<UUID>>(
                  Codec.UUID_BINARY,
                  HashSet::new,
                  false)),
          (obj, value) -> obj.triedStorageIds = value,
          obj -> obj.triedStorageIds)
      .add()

      .build();

  private VillageStorageRequest request;
  private Set<UUID> triedStorageIds;

  public EnqueueVillageStorageRequestTask() {
  }

  public EnqueueVillageStorageRequestTask(
      VillageStorageRequest request) {
    this(request, null);
  }

  public EnqueueVillageStorageRequestTask(
      VillageStorageRequest request,
      @Nullable Set<UUID> triedStorageIds) {
    this.request = request;
    this.triedStorageIds = triedStorageIds;
  }

  public VillageStorageRequest getRequest() {
    return this.request;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    Set<UUID> effectiveTriedStorageIds = this.resolveTriedStorageIds();
    List<UUID> candidateStorages = new ArrayList<>();
    for (UUID storageWorkplaceId : heart.getVillageStorageWorkplaces()) {
      if (effectiveTriedStorageIds == null || !effectiveTriedStorageIds.contains(storageWorkplaceId)) {
        candidateStorages.add(storageWorkplaceId);
      }
    }

    UUID storageToNotify = candidateStorages.isEmpty()
        ? null
        : candidateStorages.get(ThreadLocalRandom.current().nextInt(candidateStorages.size()));
    if (storageToNotify == null) {
      this.callOnError(NO_STORAGE_FOUND, "Could not get a storage to notify");
      return;
    }

    WorkplaceComponent storage = heart.getWorkplaceFromId(storageToNotify);
    if (storage == null) {
      this.callOnError(NO_STORAGE_FOUND, "Could not get the workplace storage component");
      return;
    }

    Ref<ChunkStore> storageRef = storage.getWorkplaceRef().getRef();
    if (storageRef == null || !storageRef.isValid()) {
      this.callOnError(NO_STORAGE_FOUND, "Could not get the storage Ref<ChunkStore>");
      return;
    }

    Store<ChunkStore> chunkStore = chunk.getWorld().getChunkStore().getStore();
    WorkplaceVillageStorageRequestComponent storageWorkplace = chunkStore.getComponent(storageRef,
        WorkplaceVillageStorageRequestComponent.getComponentType());

    if (storageWorkplace == null) {
      this.callOnError(NO_STORAGE_FOUND, "Could not get the storage component");
      return;
    }

    this.request.setStorageWorkplaceId(storage.getUuid());
    this.request.setRegisteredWithStorage(true);
    UUID requestId = storageWorkplace.addRequest(this.request);

    this.callOnSuccess(storage.getUuid().toString() + ":" + requestId.toString());

    BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(storageRef,
        BlockModule.BlockStateInfo.getComponentType());
    blockStateInfo.markNeedsSaving();

  }

  private Set<UUID> resolveTriedStorageIds() {
    if (this.triedStorageIds != null) {
      return this.triedStorageIds;
    }

    return this.request != null ? this.request.getTriedStorageIds() : null;
  }

}
