package com.pacificbytestudios.projectciviccore.codecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.store.CodecKey;
import com.hypixel.hytale.codec.store.CodecStore;
import com.hypixel.hytale.codec.store.StoredCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.enums.VillageRoadState;

import it.unimi.dsi.fastutil.Pair;

public class RoadsGraph {
  public static final BuilderCodec<RoadsGraph> CODEC = BuilderCodec
      .builder(RoadsGraph.class, RoadsGraph::new)
      .append(new KeyedCodec<>("RootNode", RoadNode.CODEC),
          RoadsGraph::setRoot,
          RoadsGraph::getRoot)
      .add()

      .afterDecode(RoadsGraph::rebuildParentLinks)

      .build();

  private RoadNode root;

  public RoadsGraph() {
    this.root = new RoadNode();
  }

  public RoadNode getRoot() {
    if (this.root == null) {
      this.root = new RoadNode();
    }
    return root;
  }

  public void setRoot(RoadNode root) {
    this.root = root;
    this.rebuildParentLinks();
  }

  private void rebuildParentLinks() {
    if (this.root != null) {
      this.root.rebuildParentLinks(null);
    }
  }

  @Override
  public RoadsGraph clone() {
    RoadsGraph graph = new RoadsGraph();
    graph.root = this.root == null ? null : this.root.clone();
    return graph;
  }

  public static class RoadNode {
    private static final CodecKey<RoadNode> CODEC_KEY = new CodecKey<>("RoadNode");

    public static final BuilderCodec<RoadNode> CODEC = BuilderCodec
        .builder(RoadNode.class, RoadNode::new)

        .append(new KeyedCodec<>("RoadId", Codec.UUID_BINARY),
            (obj, value) -> obj.roadId = value,
            (obj) -> obj.roadId)
        .add()

        .append(new KeyedCodec<>("Children",
            new ArrayCodec<>(new StoredCodec<>(CODEC_KEY), RoadNode[]::new)),
            (obj, value) -> obj.children = value == null ? new RoadNode[0] : value,
            (obj) -> obj.children)
        .add()

        .append(new KeyedCodec<>("Depth", Codec.BYTE),
            (obj, value) -> obj.depth = value,
            (obj) -> obj.depth)
        .add()

        .append(new KeyedCodec<>("LateralVacancySegments",
            new Vector3iMapCodec<LateralVacancySegments, HashMap<Vec3i, LateralVacancySegments>>(
                LateralVacancySegments.CODEC,
                HashMap::new,
                false),
            false),
            (obj, value) -> obj.lateralVacancySegments = value == null ? new HashMap<>() : value,
            (obj) -> obj.lateralVacancySegments)
        .add()

        .append(new KeyedCodec<>("State", new EnumCodec<>(VillageRoadState.class), false),
            (obj, value) -> obj.state = value,
            (obj) -> obj.state)
        .add()

        .append(new KeyedCodec<>("RoadSegments", new ArrayCodec<>(Vec3i.CODEC, Vec3i[]::new)),
            (obj, value) -> obj.roadSegments = value == null ? new ArrayList<>() : Arrays.asList(value),
            (obj) -> obj.roadSegments.toArray(Vec3i[]::new))
        .add()

        .append(new KeyedCodec<>("Orientation", Rotation.CODEC),
            (obj, value) -> obj.prefabOrientation = value,
            (obj) -> obj.prefabOrientation)
        .add()

        .append(new KeyedCodec<>("OrientationVec", Vec3i.CODEC),
            (obj, value) -> obj.roadDirectionVec = value,
            (obj) -> obj.roadDirectionVec)
        .add()

        .append(new KeyedCodec<>("BranchOriginKey", Vec3i.CODEC),
            (obj, value) -> obj.branchOriginKey = value,
            (obj) -> obj.branchOriginKey)
        .add()

        .afterDecode((node) -> node.rebuildParentLinks(node.parent))

        .build();

    static {
      CodecStore.STATIC.putCodecSupplier(CODEC_KEY, () -> CODEC);
    }

    private UUID roadId;
    private RoadNode parent;
    private RoadNode[] children = new RoadNode[0];
    private byte depth;
    private Map<Vec3i, LateralVacancySegments> lateralVacancySegments = new HashMap<>();
    private VillageRoadState state;
    private List<Vec3i> roadSegments = new ArrayList<>();
    private Rotation prefabOrientation;
    private Vec3i roadDirectionVec;
    private Vec3i branchOriginKey;

    public RoadNode() {
    }

    public RoadNode(RoadNode parent) {
      this.parent = parent;
    }

    public UUID getRoadId() {
      return roadId;
    }

    public void setRoadId(UUID roadId) {
      this.roadId = roadId;
    }

    public RoadNode getParent() {
      return parent;
    }

    public void setParent(RoadNode parent) {
      this.parent = parent;
    }

    public RoadNode[] getChildren() {
      return children;
    }

    public RoadNode newChild() {
      RoadNode node = new RoadNode();
      node.parent = this;
      node.depth = (byte) (this.depth + 1);
      addChildren(node);
      return node;
    }

    public VillageRoadState getState() {
      return state;
    }

    public void setState(VillageRoadState state) {
      this.state = state;
    }

    public byte getDepth() {
      return depth;
    }

    public void setDepth(byte depth) {
      this.depth = depth;
    }

    public void addChildren(RoadNode child) {
      RoadNode[] newChildArray = new RoadNode[this.children.length + 1];
      for (int i = 0; i < this.children.length; i++) {
        newChildArray[i] = this.children[i];
      }
      if (child != null) {
        child.parent = this;
      }
      newChildArray[this.children.length] = child;
      this.children = newChildArray;
    }

    private void rebuildParentLinks(RoadNode parent) {
      this.parent = parent;
      for (RoadNode child : this.children) {
        if (child != null) {
          child.rebuildParentLinks(this);
        }
      }
    }

    public Map<Vec3i, LateralVacancySegments> getLateralVacancySegments() {
      return lateralVacancySegments;
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

    public void setLateralVacancySegments(Map<Vec3i, LateralVacancySegments> lateralVacancySegments) {
      this.lateralVacancySegments = lateralVacancySegments;
    }

    public List<Vec3i> getRoadSegments() {
      return roadSegments;
    }

    public void appendRoadSegment(Vec3i segment) {
      this.roadSegments.add(segment);
    }

    public Vec3i getBranchOriginKey() {
      return branchOriginKey;
    }

    public void setBranchOriginKey(Vec3i branchOriginKey) {
      this.branchOriginKey = branchOriginKey;
    }

    public Vec3i getRoadDirectionVec() {
      return roadDirectionVec;
    }

    public void setRoadDirectionVec(Vec3i roadDirectionVec) {
      this.roadDirectionVec = roadDirectionVec;
    }

    public Rotation getPrefabOrientation() {
      return prefabOrientation;
    }

    public void setPrefabOrientation(Rotation orientation) {
      this.prefabOrientation = orientation;
    }

    @Override
    public RoadNode clone() {
      RoadNode node = new RoadNode();

      RoadNode[] childCopy = new RoadNode[this.children.length];
      for (int i = 0; i < this.children.length; i++) {
        RoadNode child = this.children[i];
        if (child != null) {
          childCopy[i] = child.clone();
          childCopy[i].parent = node;
        }
      }
      node.children = childCopy;

      node.depth = this.depth;
      if (this.lateralVacancySegments == null) {
        node.lateralVacancySegments = null;
      } else {
        node.lateralVacancySegments = new HashMap<>();
        this.lateralVacancySegments.forEach((position, adjacentSegments) -> node.lateralVacancySegments.put(
            Vec3i.from(position),
            adjacentSegments == null ? null : adjacentSegments.clone()));
      }
      node.state = this.state;

      node.roadSegments = new ArrayList<>();
      this.roadSegments.stream()
          .map(Vec3i::clone)
          .forEach(node.roadSegments::add);

      node.prefabOrientation = this.prefabOrientation;
      node.branchOriginKey = Vec3i.from(this.branchOriginKey);
      node.roadDirectionVec = Vec3i.from(this.roadDirectionVec);
      node.roadId = this.roadId;
      return node;
    }
  }
}
