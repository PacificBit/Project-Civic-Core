package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

public abstract class AVillageTask {

  protected BiConsumer<String, String> onErrorCallback;
  protected Consumer<String> onSuccessCallback;

  public abstract void execute(WorldChunk chunk, Ref<ChunkStore> heartRef, Vector3i heartPos,
      VillageHeartComponent heart);

  public void setOnError(BiConsumer<String, String> callback) {
    this.onErrorCallback = callback;
  }

  public void setOnSuccessCallback(Consumer<String> callback) {
    this.onSuccessCallback = callback;
  }

  protected void callOnError(String status, String msg) {
    if (this.onErrorCallback == null) {
      return;
    }
    this.onErrorCallback.accept(status, msg);
  }

  protected void callOnSuccess(String status) {
    if (this.onSuccessCallback == null) {
      return;
    }
    this.onSuccessCallback.accept(status);
  }

}
