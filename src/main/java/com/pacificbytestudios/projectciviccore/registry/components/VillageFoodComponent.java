package com.pacificbytestudios.projectciviccore.registry.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;

public class VillageFoodComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageFoodComponent> CODEC = BuilderCodec
      .builder(VillageFoodComponent.class, VillageFoodComponent::new)

      .append(new KeyedCodec<>("Saturation", Codec.SHORT),
          (obj, value) -> obj.saturation = value,
          (obj) -> obj.saturation)
      .add()

      .append(new KeyedCodec<>("TavernRef", EntityRef.CODEC, false),
          (obj, value) -> obj.tavernRef = value,
          (obj) -> obj.hasValidTavernRef() ? obj.tavernRef : null)
      .add()

      .build();

  private Short saturation;
  private EntityRef tavernRef;

  public EntityRef getTavernRef() {
    return this.tavernRef;
  }

  public void setTavernRef(EntityRef tavernRef) {
    this.tavernRef = tavernRef;
  }

  public boolean hasValidTavernRef() {
    return this.tavernRef != null
        && this.tavernRef.getWorldName() != null
        && this.tavernRef.getPosition() != null;
  }

  @Override
  public VillageFoodComponent clone() {
    VillageFoodComponent food = new VillageFoodComponent();
    food.saturation = this.saturation;
    food.tavernRef = this.tavernRef != null ? this.tavernRef.clone() : null;
    return food;
  }

  public static ComponentType<ChunkStore, VillageFoodComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageFoodComponentType();
  }
}
