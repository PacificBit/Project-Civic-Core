package com.pacificbytestudios.projectciviccore.codecs;

import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.store.CodecKey;
import com.hypixel.hytale.codec.store.CodecStore;
import com.hypixel.hytale.codec.store.StoredCodec;
import com.hypixel.hytale.math.vector.Vector3i;
import com.pacificbytestudios.projectciviccore.village.enums.VillageRoadState;

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

        .append(new KeyedCodec<>("Children",
            new ArrayCodec<>(new StoredCodec<>(CODEC_KEY), RoadNode[]::new)),
            (obj, value) -> obj.children = value == null ? new RoadNode[0] : value,
            (obj) -> obj.children)
        .add()

        .append(new KeyedCodec<>("Depth", Codec.BYTE),
            (obj, value) -> obj.depth = value,
            (obj) -> obj.depth)
        .add()

        .append(new KeyedCodec<>("Segments",
            new Vector3iMapCodec<RoadAdjacentSegments, HashMap<Vector3i, RoadAdjacentSegments>>(
                RoadAdjacentSegments.CODEC,
                HashMap::new,
                false),
            false),
            (obj, value) -> obj.segments = value == null ? new HashMap<>() : value,
            (obj) -> obj.segments)
        .add()

        .append(new KeyedCodec<>("State", new EnumCodec<>(VillageRoadState.class), false),
            (obj, value) -> obj.state = value,
            (obj) -> obj.state)
        .add()

        .afterDecode((node) -> node.rebuildParentLinks(node.parent))

        .build();

    static {
      CodecStore.STATIC.putCodecSupplier(CODEC_KEY, () -> CODEC);
    }

    private RoadNode parent;
    private RoadNode[] children = new RoadNode[0];
    private byte depth;
    private Map<Vector3i, RoadAdjacentSegments> segments = new HashMap<>();
    private VillageRoadState state;

    public RoadNode() {
    }

    public RoadNode(RoadNode parent) {
      this.parent = parent;
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
      if (this.segments == null) {
        node.segments = null;
      } else {
        node.segments = new HashMap<>();
        this.segments.forEach((position, adjacentSegments) -> node.segments.put(
            position == null ? null : position.clone(),
            adjacentSegments == null ? null : adjacentSegments.clone()));
      }
      node.state = this.state;

      return node;
    }
  }
}
