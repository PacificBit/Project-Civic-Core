package com.pacificbytestudios.projectciviccore.registry.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class VillageFoodComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageFoodComponent> CODEC = VillageFoodComponentCodec.CODEC;

  Short saturation;
  EntityRef tavernRef;

  public boolean hasValidTavernRef() {
    return this.tavernRef != null
        && this.tavernRef.getWorldName() != null
        && this.tavernRef.getPosition() != null;
  }

  @Override
  public VillageFoodComponent clone() {
    return VillageFoodComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, VillageFoodComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageFoodComponentType();
  }
}
