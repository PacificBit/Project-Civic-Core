package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

public class WorkplaceContainerEntryComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceContainerEntryComponent> CODEC = BuilderCodec
      .builder(WorkplaceContainerEntryComponent.class,
          WorkplaceContainerEntryComponent::new)

      .append(new KeyedCodec<>("ContainerId", Codec.UUID_BINARY),
          (obj, value) -> obj.uuid = value,
          (obj) -> obj.uuid)
      .add()

      .append(new KeyedCodec<>("ContainerTransform", BlockTransform.VEC3I_CODEC),
          (obj, value) -> obj.containerTransform = value,
          (obj) -> obj.containerTransform)
      .add()

      .build();

  private UUID uuid;
  private BlockTransform<Vec3i> containerTransform = new BlockTransform<>();

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
    WorkplaceContainerEntryComponent container = new WorkplaceContainerEntryComponent();
    container.uuid = this.uuid;
    container.containerTransform = BlockTransform.clone(this.containerTransform);
    return container;
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
