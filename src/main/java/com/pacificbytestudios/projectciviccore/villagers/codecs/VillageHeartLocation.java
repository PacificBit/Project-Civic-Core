package com.pacificbytestudios.projectciviccore.villagers.codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class VillageHeartLocation {
  public static final BuilderCodec<VillageHeartLocation> CODEC = BuilderCodec
      .builder(VillageHeartLocation.class, VillageHeartLocation::new)

      .append(new KeyedCodec<>("WorldName", Codec.STRING),
          (obj, value) -> obj.worldName = value,
          (obj) -> obj.worldName)
      .add()

      .append(new KeyedCodec<>("Position", Vector3i.CODEC),
          (obj, value) -> obj.position = value,
          (obj) -> obj.position)
      .add()

      .build();

  private String worldName;
  private Vector3i position;

  public String getWorldName() {
    return worldName;
  }

  public void setWorldName(String worldName) {
    this.worldName = worldName;
  }

  public Vector3i getPosition() {
    return position;
  }

  public void setPosition(Vector3i position) {
    this.position = position;
  }

  public VillageHeartLocation clone() {
    VillageHeartLocation newLocation = new VillageHeartLocation();
    newLocation.setWorldName(this.worldName);
    newLocation.setPosition(this.position);
    return newLocation;
  }

  @Override
  public String toString() {
    return "VillageHeartLocation[" + this.worldName + " | pos=" + this.position + "]";
  }
}
