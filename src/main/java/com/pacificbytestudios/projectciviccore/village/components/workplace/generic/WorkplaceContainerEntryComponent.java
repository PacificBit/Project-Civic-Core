package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class WorkplaceContainerEntryComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceContainerEntryComponent> CODEC = WorkplaceContainerEntryComponentCodec.CODEC;

  @SerialName("ContainerId")
  UUID uuid;
  BlockTransform3D containerTransform = new BlockTransform3D();

  public Vec3i getPosition() {
    return this.containerTransform.getPosition3i();
  }

  public void setPosition(Vec3i position) {
    this.containerTransform.setPosition(position);
  }

  public Vec3f getAccessPosition() {
    return this.containerTransform.getPosition3f().add(this.containerTransform.getAccessOffset3f());
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
