package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.codecs.VillageHeartLocation;

public class VillagerComponent implements Component<EntityStore> {
  public static final BuilderCodec<VillagerComponent> CODEC = BuilderCodec
      .builder(VillagerComponent.class, VillagerComponent::new)

      .append(new KeyedCodec<>("VillageHeartLocation", VillageHeartLocation.CODEC),
          (obj, value) -> obj.setVillageHeartLocation(value),
          (obj) -> obj.villageHeartLocation)
      .add()

      .build();

  private VillageHeartLocation villageHeartLocation;
  private Ref<ChunkStore> villageHeartRef;

  public void setVillageHeartLocation(VillageHeartLocation location) {
    this.villageHeartLocation = location;
  }

  public Ref<ChunkStore> getVillageHeartRef() {
    if (this.villageHeartRef == null) {
      World world = Universe.get().getWorld(villageHeartLocation.getWorldName());
      Vector3i villageHeartPos = this.villageHeartLocation.getPosition();
      WorldChunk chunk = world.getChunkIfInMemory(
          ChunkUtil.indexChunkFromBlock(villageHeartPos.x, villageHeartPos.z));

      if (chunk != null) {
        this.villageHeartRef = chunk.getBlockComponentEntity(villageHeartPos.x, villageHeartPos.y, villageHeartPos.z);
      }
    }
    return this.villageHeartRef;
  }

  @Override
  public VillagerComponent clone() {
    VillagerComponent newComponent = new VillagerComponent();
    newComponent.villageHeartRef = this.villageHeartRef;
    newComponent.villageHeartLocation = this.villageHeartLocation;
    return newComponent;
  }

  public static ComponentType<EntityStore, VillagerComponent> getComponentType() {
    return ProjectCivicCore.get().getVillagerComponentType();
  }
}
