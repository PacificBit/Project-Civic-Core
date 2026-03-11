package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;

public class EnqueueVillageStorageRequestTask implements IVillageTask {
  public static final String NO_STORAGE_FOUND = "NO_STORAGE_FOUND";

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
  private transient BiConsumer<String, Object> onErrorCallback;

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

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    UUID storageToNotify = null;
    for (UUID storageWorkplaceId : heart.getVillageStorageWorkplaces()) {
      if (!this.triedStorageIds.contains(storageWorkplaceId)) {
        storageToNotify = storageWorkplaceId;
      }
    }

    if (storageToNotify == null) {
      this.onErrorCallback.accept(NO_STORAGE_FOUND, null);
      return;
    }

    heart.getWorkplaceFromId(storageToNotify);
  }

  @Override
  public void setOnError(BiConsumer<String, Object> callback) {
    this.onErrorCallback = callback;
  }

}
