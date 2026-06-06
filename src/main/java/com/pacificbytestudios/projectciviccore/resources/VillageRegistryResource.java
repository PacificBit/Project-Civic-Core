package com.pacificbytestudios.projectciviccore.resources;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.VillageRegistryEntry;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;

public class VillageRegistryResource implements Resource<ChunkStore> {
  public static final BuilderCodec<VillageRegistryResource> CODEC = BuilderCodec
      .builder(VillageRegistryResource.class, VillageRegistryResource::new)

      .append(new KeyedCodec<>("VillageEntries",
          new ArrayCodec<>(
              VillageRegistryEntry.CODEC,
              VillageRegistryEntry[]::new)),
          (obj, value) -> obj.villageEntries = value == null ? new CopyOnWriteArraySet<>()
              : new CopyOnWriteArraySet<>(Arrays.asList(value)),
          (obj) -> obj.villageEntries == null ? new VillageRegistryEntry[0]
              : obj.villageEntries.toArray(VillageRegistryEntry[]::new))
      .add()

      .build();

  private Set<VillageRegistryEntry> villageEntries = new CopyOnWriteArraySet<>();

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
    VillageRegistryResource resource = new VillageRegistryResource();

    resource.villageEntries = new CopyOnWriteArraySet<>();
    if (this.villageEntries != null) {
      for (VillageRegistryEntry entry : this.villageEntries) {
        resource.villageEntries.add(entry == null ? null : entry.clone());
      }
    }

    return resource;
  }
}
