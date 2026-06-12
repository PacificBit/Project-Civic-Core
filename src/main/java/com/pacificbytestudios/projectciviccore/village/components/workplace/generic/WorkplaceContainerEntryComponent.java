package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

@SerializableClass
public class WorkplaceContainerEntryComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceContainerEntryComponent> CODEC = WorkplaceContainerEntryComponentCodec.CODEC;

  @SerialName("ContainerId")
  UUID uuid;
  BlockTransform<Vec3i> containerTransform = new BlockTransform<>();

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public Vec3i getPosition() {
    return this.containerTransform.getPosition();
  }

  public void setPosition(Vec3i position) {
    this.containerTransform.setPosition(position);
  }

  public BlockTransform<Vec3i> getContainerTransform() {
    return this.containerTransform;
  }

  public void setContainerTransform(BlockTransform<Vec3i> containerTransform) {
    this.containerTransform = containerTransform;
  }

  public Vec3f getAccessPosition() {
    return Vec3f.from(this.containerTransform.getPosition()).add(this.containerTransform.getAccessOffset());
  }

  public void setAccessOffset(Vec3f accessOffset) {
    this.containerTransform.setAccessOffset(Vec3f.from(accessOffset));
  }

  @Override
  public WorkplaceContainerEntryComponent clone() {
    return WorkplaceContainerEntryComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, WorkplaceContainerEntryComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceContainerEntryComponentType();
  }

  @Override
  public String toString() {
    return "WorkplaceContainerEntryComponent: {\n\tUUID: %s,\n\tPosition: %s,\n\tAccessPosition: %s\n\t}"
        .formatted(uuid, getPosition(), getAccessPosition());
  }

}
