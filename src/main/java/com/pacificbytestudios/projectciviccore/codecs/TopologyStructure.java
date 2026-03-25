package com.pacificbytestudios.projectciviccore.codecs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;

public class TopologyStructure extends AreaFootprint {
  public static final BuilderCodec<TopologyStructure> CODEC = BuilderCodec
      .builder(TopologyStructure.class, TopologyStructure::new,
          AreaFootprint.CODEC)

      .append(new KeyedCodec<>("StructureId", Codec.UUID_BINARY),
          (obj, value) -> obj.structureId = value,
          (obj) -> obj.structureId)
      .add()

      .append(new KeyedCodec<>("StructureType", new EnumCodec<>(
          StructureType.class), false),
          (obj, value) -> obj.structureType = value,
          (obj) -> obj.structureType)
      .add()

      .append(new KeyedCodec<>("PrefabAnchor", Vector3i.CODEC),
          (obj, value) -> obj.prefabAnchor = value,
          obj -> obj.prefabAnchor)
      .add()

      .append(new KeyedCodec<>("Orientation", Rotation.CODEC, false),
          (obj, value) -> obj.orientation = value,
          obj -> obj.orientation)
      .add()

      .append(new KeyedCodec<>("TopologyType", new EnumCodec<>(
          VillageTopologyStructureType.class), false),
          (obj, value) -> obj.topologyType = value,
          (obj) -> obj.topologyType)
      .add()

      .append(new KeyedCodec<>("DistanceFromCenter", Codec.DOUBLE),
          (obj, value) -> obj.distanceFromCenter = value,
          (obj) -> obj.distanceFromCenter)
      .add()

      .append(new KeyedCodec<>("Edges",
          new Vector3iMapCodec<Vector3i, HashMap<Vector3i, Vector3i>>(
              Vector3i.CODEC,
              HashMap::new,
              false),
          false),
          (obj, value) -> {
            obj.edges = decodeEdges(value);
            if (obj.edges.isEmpty()) {
              obj.rebuildEdges();
            }
          },
          obj -> encodeEdges(obj.edges))
      .add()

      .build();

  private UUID structureId;
  private StructureType structureType;
  private VillageTopologyStructureType topologyType;
  private double distanceFromCenter;
  private Vector3i prefabAnchor;
  private Rotation orientation;
  private Map<Vector3i, Vector3i> edges = new HashMap<>();

  public TopologyStructure() {
  }

  public TopologyStructure(
      Vector3i center,
      Vector2i size2i,
      VillageTopologyStructureType type,
      Rotation rotation) {
    this(center, size2i, type, rotation, 0);
  }

  public TopologyStructure(
      Vector3i center,
      Vector2i size2i,
      VillageTopologyStructureType type,
      Rotation rotation,
      double distanceFromCenter) {
    super(createCenteredOffset(size2i), center, rotation);
    this.topologyType = type;
    this.distanceFromCenter = distanceFromCenter;
    this.orientation = rotation == null ? Rotation.None : rotation;
    rebuildEdges();
  }

  public UUID getStructureId() {
    return structureId;
  }

  public void setStructureId(UUID structureId) {
    this.structureId = structureId;
  }

  public StructureType getStructureType() {
    return structureType;
  }

  public void setStructureType(StructureType structureType) {
    this.structureType = structureType;
  }

  public double getDistanceFromCenter() {
    return distanceFromCenter;
  }

  public void setDistanceFromCenter(double distanceFromCenter) {
    this.distanceFromCenter = distanceFromCenter;
  }

  public VillageTopologyStructureType getTopologyType() {
    return this.topologyType;
  }

  public void setTopologyType(VillageTopologyStructureType topologyType) {
    this.topologyType = topologyType;
  }

  public Vector3i getPrefabAnchor() {
    return prefabAnchor;
  }

  public void setPrefabAnchor(Vector3i prefabAnchor) {
    this.prefabAnchor = prefabAnchor;
  }

  public Rotation getOrientation() {
    return this.orientation == null ? Rotation.None : this.orientation;
  }

  public void setOrientation(Rotation orientation) {
    this.orientation = orientation == null ? Rotation.None : orientation;
    rebuildEdges();
  }

  public int getWidth() {
    if (getTopLeft() == null || getBotRight() == null) {
      return 0;
    }

    return isWidthAlignedWithXAxis()
        ? getAxisAlignedWidth()
        : getAxisAlignedDepth();
  }

  public int getDepth() {
    if (getTopLeft() == null || getBotRight() == null) {
      return 0;
    }

    return isWidthAlignedWithXAxis()
        ? getAxisAlignedDepth()
        : getAxisAlignedWidth();
  }

  public Vector3i getEdge(Vector3i normalizedDirection) {
    Vector3i edge = this.edges.get(normalizedDirection);
    return edge == null ? null : edge.clone();
  }

  public Vector3i getForwardVector() {
    return getOrientation().getAxisDirection().clone();
  }

  public Vector3i getBackVector() {
    return getOrientation().add(Rotation.OneEighty).getAxisDirection().clone();
  }

  public Vector3i getLeftVector() {
    return getOrientation().add(Rotation.Ninety).getAxisDirection().clone();
  }

  public Vector3i getRightVector() {
    return getOrientation().add(Rotation.TwoSeventy).getAxisDirection().clone();
  }

  public Vector3i getFrontEdge() {
    return getEdge(getForwardVector());
  }

  public Vector3i getBackEdge() {
    return getEdge(getBackVector());
  }

  public Vector3i getLeftEdge() {
    return getEdge(getLeftVector());
  }

  public Vector3i getRightEdge() {
    return getEdge(getRightVector());
  }

  @Override
  public void setTopLeft(Vector2i topLeft) {
    super.setTopLeft(topLeft);
    rebuildEdges();
  }

  @Override
  public void setBotRight(Vector2i botRight) {
    super.setBotRight(botRight);
    rebuildEdges();
  }

  @Override
  protected void setY(int y) {
    super.setY(y);
    rebuildEdges();
  }

  @Override
  protected void assignBounds(Vector2i topLeft, Vector2i botRight) {
    super.assignBounds(topLeft, botRight);
    rebuildEdges();
  }

  public Vector3i getCenter() {
    Vector2i center2D = getCenter2D();
    if (center2D == null) {
      return null;
    }

    return new Vector3i(center2D.x, getY(), center2D.y);
  }

  public boolean collide(TopologyStructure other) {
    if (other == null || getTopLeft() == null || getBotRight() == null ||
        other.getTopLeft() == null || other.getBotRight() == null) {
      return false;
    }

    return getMinX() <= other.getMaxX() &&
        other.getMinX() <= getMaxX() &&
        getMinZ() <= other.getMaxZ() &&
        other.getMinZ() <= getMaxZ();
  }

  private static AreaFootprint createCenteredOffset(Vector2i size2i) {
    if (size2i == null) {
      return null;
    }

    int width = Math.max(size2i.x, 1);
    int depth = Math.max(size2i.y, 1);

    int localMinX = -(width / 2);
    int localMinZ = -(depth / 2);
    int localMaxX = localMinX + width - 1;
    int localMaxZ = localMinZ + depth - 1;

    return new AreaFootprint(
        new Vector2i(localMinX, localMinZ),
        new Vector2i(localMaxX, localMaxZ));
  }

  @Override
  public TopologyStructure clone() {
    TopologyStructure component = new TopologyStructure();
    component.setTopLeft(
        getTopLeft() == null ? null : getTopLeft().clone());
    component.setBotRight(
        getBotRight() == null ? null : getBotRight().clone());

    component.setY(this.getY());
    component.orientation = this.orientation;
    component.topologyType = this.topologyType;
    component.structureId = this.structureId;
    component.distanceFromCenter = this.distanceFromCenter;
    component.structureType = this.structureType;
    component.edges = encodeEdges(this.edges);
    component.prefabAnchor = this.prefabAnchor == null ? null : this.prefabAnchor.clone();

    return component;
  }

  @Override
  public String toString() {
    return "TopologyStructure{"
        + "\n structureId=" + this.structureId
        + "\n topologyType=" + this.topologyType
        + ",\n topLeft=" + getTopLeft()
        + ",\n botRight=" + getBotRight()
        + ",\n y=" + getY()
        + ",\n distanceFromCenter=" + this.distanceFromCenter
        + '}';
  }

  private HashMap<Vector3i, Vector3i> buildEdges() {
    Vector2i axisAlignedTopLeft = getAxisAlignedTopLeft();
    Vector2i axisAlignedBotRight = getAxisAlignedBotRight();
    Vector2i center2D = getCenter2D();
    if (axisAlignedTopLeft == null || axisAlignedBotRight == null || center2D == null) {
      return new HashMap<>();
    }

    HashMap<Vector3i, Vector3i> computedEdges = new HashMap<>(4);
    int y = getY();
    int centerX = center2D.x;
    int centerZ = center2D.y;

    computedEdges.put(Vector3i.NORTH, new Vector3i(centerX, y, axisAlignedTopLeft.y));
    computedEdges.put(Vector3i.WEST, new Vector3i(axisAlignedTopLeft.x, y, centerZ));
    computedEdges.put(Vector3i.EAST, new Vector3i(axisAlignedBotRight.x, y, centerZ));
    computedEdges.put(Vector3i.SOUTH, new Vector3i(centerX, y, axisAlignedBotRight.y));
    return computedEdges;
  }

  private boolean isWidthAlignedWithXAxis() {
    Rotation normalizedOrientation = this.orientation == null ? Rotation.None : this.orientation;
    return normalizedOrientation == Rotation.None
        || normalizedOrientation == Rotation.OneEighty;
  }

  private int getAxisAlignedWidth() {
    return getMaxX() - getMinX() + 1;
  }

  private int getAxisAlignedDepth() {
    return getMaxZ() - getMinZ() + 1;
  }

  private void rebuildEdges() {
    this.edges = buildEdges();
  }

  private static HashMap<Vector3i, Vector3i> decodeEdges(Map<Vector3i, Vector3i> encoded) {
    return encodeEdges(encoded);
  }

  private static HashMap<Vector3i, Vector3i> encodeEdges(Map<Vector3i, Vector3i> edges) {
    HashMap<Vector3i, Vector3i> copy = new HashMap<>();

    if (edges == null) {
      return copy;
    }

    edges.forEach((direction, edge) -> {
      if (direction == null || edge == null) {
        return;
      }

      copy.put(direction.clone(), edge.clone());
    });
    return copy;
  }

}
