package com.pacificbytestudios.projectciviccore.codecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.store.CodecKey;
import com.hypixel.hytale.codec.store.CodecStore;
import com.hypixel.hytale.codec.store.StoredCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudio.codecbutler.annotations.SerialAfterDecode;
import com.pacificbytestudio.codecbutler.annotations.SerialIgnore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerialWithCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.enums.VillageRoadState;

import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class RoadNode {
  // These codecs are referenced by the generated RoadNodeCodec.<clinit>, so they
  // must be initialized before CODEC (which forces RoadNodeCodec to load).
  static final Vector3iMapCodec<LateralVacancySegments, HashMap<Vec3i, LateralVacancySegments>> LATERAL_VACANCY_SEGMENTS_CODEC = new Vector3iMapCodec<>(
      LateralVacancySegments.CODEC,
      HashMap::new,
      false);

  static final ArrayCodec<RoadNode> CHILDREN_CODEC = new ArrayCodec<>(
      new StoredCodec<>(new CodecKey<>("RoadNode")),
      RoadNode[]::new);

  public static final BuilderCodec<RoadNode> CODEC = RoadNodeCodec.CODEC;

  static {
    CodecStore.STATIC.putCodecSupplier(new CodecKey<>("RoadNode"), () -> CODEC);
  }

  UUID roadId;

  @SerialIgnore
  RoadNode parent;

  @SerialWithCodec("RoadNode.CHILDREN_CODEC")
  RoadNode[] children = new RoadNode[0];

  byte depth;

  @SerialWithCodec("RoadNode.LATERAL_VACANCY_SEGMENTS_CODEC")
  Map<Vec3i, LateralVacancySegments> lateralVacancySegments = new HashMap<>();

  VillageRoadState state;
  List<Vec3i> roadSegments = new ArrayList<>();

  @SerialName("Orientation")
  @SerialWithCodec("com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation.CODEC")
  Rotation prefabOrientation;

  @SerialName("OrientationVec")
  Vec3i roadDirectionVec;

  Vec3i branchOriginKey;

  public RoadNode() {
  }

  public RoadNode(RoadNode parent) {
    this.parent = parent;
  }

  public RoadNode newChild() {
    RoadNode node = new RoadNode();
    node.parent = this;
    node.depth = (byte) (this.depth + 1);
    addChildren(node);
    return node;
  }

  public void addChildren(RoadNode child) {
    RoadNode[] sourceChildren = this.children == null ? new RoadNode[0] : this.children;
    RoadNode[] newChildArray = new RoadNode[sourceChildren.length + 1];
    for (int i = 0; i < sourceChildren.length; i++) {
      newChildArray[i] = sourceChildren[i];
    }
    if (child != null) {
      child.parent = this;
    }
    newChildArray[sourceChildren.length] = child;
    this.children = newChildArray;
  }

  @SerialAfterDecode
  void rebuildParentLinks() {
    this.rebuildParentLinks(this.parent);
  }

  private void rebuildParentLinks(RoadNode parent) {
    this.parent = parent;
    RoadNode[] sourceChildren = this.children == null ? new RoadNode[0] : this.children;
    for (RoadNode child : sourceChildren) {
      if (child != null) {
        child.rebuildParentLinks(this);
      }
    }
  }

  public LateralVacancySegments getLateralVacancySegment(Vec3i cardinalDirection) {
    return this.lateralVacancySegments.get(cardinalDirection);
  }

  public Pair<Vec3i, LateralVacancySegments> getLateralVacancySegmentWithFittingSpace(short size) {
    for (Map.Entry<Vec3i, LateralVacancySegments> segments : this.lateralVacancySegments.entrySet()) {
      if (segments.getValue() != null && segments.getValue().hasFittinSegment(size)) {
        return Pair.of(segments.getKey(), segments.getValue());
      }
    }
    return null;
  }

  public void appendRoadSegment(Vec3i segment) {
    this.roadSegments.add(segment);
  }

  @Override
  public RoadNode clone() {
    RoadNode node = RoadNodeCodec.clone(this);
    node.rebuildParentLinks(null);
    return node;
  }
}
