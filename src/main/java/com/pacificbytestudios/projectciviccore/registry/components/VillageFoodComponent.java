package com.pacificbytestudios.projectciviccore.registry.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class VillageFoodComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageFoodComponent> CODEC = BuilderCodec
      .builder(VillageFoodComponent.class, VillageFoodComponent::new)

      .append(new KeyedCodec<>("Saturation", Codec.SHORT),
          (obj, value) -> obj.saturation = value,
          (obj) -> obj.saturation)
      .add()

      .build();

  private Short saturation;

  @Override
  public VillageFoodComponent clone() {
    VillageFoodComponent food = new VillageFoodComponent();
    food.saturation = this.saturation;
    return food;
  }

  public static ComponentType<ChunkStore, VillageFoodComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageFoodComponentType();
  }
}
