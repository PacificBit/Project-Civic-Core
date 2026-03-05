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
import com.pacificbytestudios.projectciviccore.village.codec.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.village.enums.VillageHeartState;

public class VillageHeartComponent implements Component<ChunkStore> {

  public static final BuilderCodec<VillageHeartComponent> CODEC = BuilderCodec
      .builder(VillageHeartComponent.class, VillageHeartComponent::new)

      .append(new KeyedCodec<>("Config", VillageHeartConfigComponent.CODEC),
          (obj, value) -> obj.config = value,
          (obj) -> obj.config)
      .add()

      .append(
          new KeyedCodec<>(
              "NpcRegistry",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false)),
          (obj, value) -> obj.npcRegistry = value,
          obj -> obj.npcRegistry)
      .add()

      .append(new KeyedCodec<>("State", Codec.STRING),
          (obj, value) -> obj.state = VillageHeartState.valueOf(value.toUpperCase()),
          (obj) -> obj.state.getState())
      .add()

      .append(new KeyedCodec<>("Level", Codec.INTEGER),
          (obj, value) -> obj.level = value,
          (obj) -> obj.level)
      .add()

      .append(
          new KeyedCodec<>("ClaimedArea", ClaimedAreaComponent.CODEC),
          (obj, value) -> obj.claimedArea = value,
          (obj) -> obj.claimedArea)
      .add()

      .build();

  private VillageHeartConfigComponent config;
  private Set<UUID> npcRegistry = new HashSet<>();
  private VillageHeartState state = VillageHeartState.NORMAL;
  private ClaimedAreaComponent claimedArea;
  private int level;

  // INFO: Read only
  public VillageHeartConfigComponent getConfig() {
    return config.clone();
  }

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

  public ClaimedAreaComponent getClaimedArea() {
    return claimedArea;
  }

  public void setClaimedArea(ClaimedAreaComponent claimedChunks) {
    this.claimedArea = claimedChunks;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  @Override
  public VillageHeartComponent clone() {
    VillageHeartComponent component = new VillageHeartComponent();
    component.config = this.config;
    component.npcRegistry = this.npcRegistry;
    component.state = this.state;
    component.claimedArea = this.claimedArea;
    component.level = this.level;
    return component;
  }

  public static ComponentType<ChunkStore, VillageHeartComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageCoreComponentType();
  }

}
