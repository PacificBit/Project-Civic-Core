package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;

public class VillagerHousingComponent implements Component<EntityStore> {
  public static final BuilderCodec<VillagerHousingComponent> CODEC = BuilderCodec
      .builder(VillagerHousingComponent.class, VillagerHousingComponent::new)

      .append(new KeyedCodec<>("House", HouseComponent.CODEC),
          (obj, value) -> obj.house = value,
          (obj) -> obj.house)
      .add()

      .append(new KeyedCodec<>("BedPosition", Vec3i.CODEC),
          (obj, value) -> obj.bedPosition = value,
          (obj) -> obj.bedPosition)
      .add()

      .build();

  private HouseComponent house;
  private Vec3i bedPosition;

  public HouseComponent getHouse() {
    return house;
  }

  public void setHouse(HouseComponent house) {
    this.house = house;
  }

  public Vec3i getBedPosition() {
    return bedPosition;
  }

  public void setBedPosition(Vec3i bedPosition) {
    this.bedPosition = bedPosition;
  }

  @Override
  public VillagerHousingComponent clone() {
    VillagerHousingComponent component = new VillagerHousingComponent();
    component.house = this.house == null ? null : this.house.clone();
    component.bedPosition = this.bedPosition == null ? null : this.bedPosition.clone();
    return component;
  }

  @Override
  public String toString() {
    return "VillagerHousingComponent{" +
        "house=" + Utils.safeToString(house) +
        ", bedPosition=" + Utils.safeToString(bedPosition) +
        '}';
  }
}
