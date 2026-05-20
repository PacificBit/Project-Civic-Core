package com.pacificbytestudios.projectciviccore.villagers.core.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3f;
import org.joml.Vector3i;

import com.hypixel.hytale.builtin.mounts.BlockMountAPI.BlockMountResult;
import com.hypixel.hytale.component.system.EcsEvent;
import com.pacificbytestudios.projectciviccore.utils.Utils;

public final class SitOnChairEvent extends EcsEvent {
  @Nonnull
  private final Vector3i targetBlock;

  @Nonnull
  private final Vector3f interactPos;

  @Nullable
  private BlockMountResult result;

  public SitOnChairEvent(@Nonnull Vector3i targetBlock) {
    this(targetBlock, Utils.centerOf(targetBlock));
  }

  public SitOnChairEvent(@Nonnull Vector3i targetBlock, @Nonnull Vector3f interactPos) {
    this.targetBlock = new Vector3i(targetBlock);
    this.interactPos = new Vector3f(interactPos);
  }

  @Nonnull
  public Vector3i getTargetBlock() {
    return this.targetBlock;
  }

  @Nonnull
  public Vector3f getInteractPos() {
    return this.interactPos;
  }

  @Nullable
  public BlockMountResult getResult() {
    return this.result;
  }

  public void setResult(@Nullable BlockMountResult result) {
    this.result = result;
  }

}
