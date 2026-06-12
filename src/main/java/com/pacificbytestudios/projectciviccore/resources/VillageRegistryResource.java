package com.pacificbytestudios.projectciviccore.resources;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.VillageRegistryEntry;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;

@SerializableClass
public class VillageRegistryResource implements Resource<ChunkStore> {
  public static final BuilderCodec<VillageRegistryResource> CODEC = VillageRegistryResourceCodec.CODEC;

  Set<VillageRegistryEntry> villageEntries = new CopyOnWriteArraySet<>();

  public void registerVillage(VillageRegistryEntry entry) {
    this.villageEntries.add(entry);
  }

  public void unregisterVillage(UUID villageId) {
    this.villageEntries.removeIf(entry -> Objects.equals(entry.getVillageId(), villageId));
  }

  public VillageRegistryEntry findVillageAtCoord(Vec3d coords) {
    Vec2i simplified = coords.to2i();
    for (VillageRegistryEntry entry : this.villageEntries) {
      if (entry.getClaimedArea().isInside(simplified)) {
        return entry;
      }
    }
    return null;
  }

  public static ResourceType<ChunkStore, VillageRegistryResource> getResourceType() {
    return ProjectCivicCore.get().getVillageRegistryResourceType();
  }

  @Override
  public VillageRegistryResource clone() {
    return VillageRegistryResourceCodec.clone(this);
  }
}
