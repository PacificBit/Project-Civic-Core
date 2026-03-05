package com.pacificbytestudios.projectciviccore.village.components.heart;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class VillageHeartConfigComponent implements Component<ChunkStore> {

  public static final BuilderCodec<VillageHeartConfigComponent> CODEC = BuilderCodec
      .builder(VillageHeartConfigComponent.class, VillageHeartConfigComponent::new)

      .append(new KeyedCodec<>("ClaimRadiusPerLevel", Codec.INTEGER),
          (obj, value) -> obj.claimRadiusPerLevel = value,
          (obj) -> obj.claimRadiusPerLevel)
      .add()

      .build();

  private int claimRadiusPerLevel;

  public int getClaimRadiusPerLevel() {
    return claimRadiusPerLevel;
  }

  public void setClaimRadiusPerLevel(int claimRadiusPerLevel) {
    this.claimRadiusPerLevel = claimRadiusPerLevel;
  }

  @Override
  public VillageHeartConfigComponent clone() {
    VillageHeartConfigComponent component = new VillageHeartConfigComponent();
    component.claimRadiusPerLevel = this.claimRadiusPerLevel;
    return component;
  }
}
