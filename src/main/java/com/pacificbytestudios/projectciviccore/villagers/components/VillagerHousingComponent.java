package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class VillagerHousingComponent implements Component<EntityStore> {
  public static final BuilderCodec<VillagerHousingComponent> CODEC = VillagerHousingComponentCodec.CODEC;

  HouseComponent house;
  Vec3i bedPosition;

  @Override
  public VillagerHousingComponent clone() {
    return VillagerHousingComponentCodec.clone(this);
  }

  @Override
  public String toString() {
    return "VillagerHousingComponent{" +
        "house=" + Utils.safeToString(house) +
        ", bedPosition=" + Utils.safeToString(bedPosition) +
        '}';
  }
}
