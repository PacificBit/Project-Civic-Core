package com.pacificbytestudios.projectciviccore.villagers.core.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.mounts.BlockMountAPI.BlockMountResult;
import com.hypixel.hytale.component.system.EcsEvent;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class SitOnChairEvent extends EcsEvent {

  @Nonnull
  private final Vec3i targetBlock;

  @Nonnull
  private final Vec3f interactPos;

  @Setter
  @Nullable
  private BlockMountResult result;

  public SitOnChairEvent(@Nonnull Vec3i targetBlock) {
    this(targetBlock, Utils.centerOf(targetBlock));
  }

  public SitOnChairEvent(@Nonnull Vec3i targetBlock, @Nonnull Vec3f interactPos) {
    this.targetBlock = new Vec3i(targetBlock);
    this.interactPos = new Vec3f(interactPos);
  }

}
