package com.pacificbytestudios.projectciviccore.villagers.components;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;

public class VillagerHousingComponent implements Component<EntityStore> {
  public static final BuilderCodec<VillagerHousingComponent> CODEC = BuilderCodec
      .builder(VillagerHousingComponent.class, VillagerHousingComponent::new)

      .append(new KeyedCodec<>("House", HouseComponent.CODEC),
          (obj, value) -> obj.house = value,
          (obj) -> obj.house)
      .add()

      .append(new KeyedCodec<>("BedPosition", Vector3iUtil.CODEC),
          (obj, value) -> obj.bedPosition = value,
          (obj) -> obj.bedPosition)
      .add()

      .build();

  private HouseComponent house;
  private Vector3i bedPosition;

  public HouseComponent getHouse() {
    return house;
  }

  public void setHouse(HouseComponent house) {
    this.house = house;
  }

  public Vector3i getBedPosition() {
    return bedPosition;
  }

  public void setBedPosition(Vector3i bedPosition) {
    this.bedPosition = bedPosition;
  }

  @Override
  public VillagerHousingComponent clone() {
    VillagerHousingComponent component = new VillagerHousingComponent();
    component.house = this.house == null ? null : this.house.clone();
    component.bedPosition = VectorUtil.clone(this.bedPosition);
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
