package com.pacificbytestudios.projectciviccore.village.components.heart;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.state.VillageHeartState;

public class VillageHeartComponent implements Component<ChunkStore> {

  public static final BuilderCodec<VillageHeartComponent> CODEC = BuilderCodec
      .builder(VillageHeartComponent.class, VillageHeartComponent::new)

      .append(
          new KeyedCodec<Set<UUID>>(
              "NpcRegistry",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false)),
          (obj, value) -> obj.npcRegistry = value,
          obj -> obj.npcRegistry)
      .add()

      .append(new KeyedCodec<>("State", Codec.STRING),
          (obj, value) -> obj.state = VillageHeartState.fromString(value),
          (obj) -> obj.state.getState())
      .add()

      .build();

  private Set<UUID> npcRegistry = new HashSet<>();
  private VillageHeartState state = VillageHeartState.NORMAL;

  public Set<UUID> getNpcRegistry() {
    return Collections.unmodifiableSet(this.npcRegistry);
  }

  public void registerNpc(UUID uuid) {
    this.npcRegistry.add(uuid);
  }

  public void unregisterNpc(UUID uuid) {
    this.npcRegistry.remove(uuid);
  }

  public VillageHeartState getState() {
    return this.state;
  }

  public void setState(VillageHeartState state) {
    this.state = state;
  }

  @Override
  public VillageHeartComponent clone() {
    VillageHeartComponent component = new VillageHeartComponent();
    component.npcRegistry = new HashSet<>(this.npcRegistry);
    return component;
  }

  public static ComponentType<ChunkStore, VillageHeartComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageCoreComponentType();
  }

}
