package com.pacificbytestudios.projectciviccore.codecs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public final class TopologyStructure extends AreaFootprint {
  public static final BuilderCodec<TopologyStructure> CODEC = TopologyStructureCodec.CODEC; // BuilderCodec

  UUID structureId;
  StructureType structureType;
  VillageTopologyStructureType topologyType;
  double distanceFromCenter;
  Vec3i prefabAnchor;
  Rotation orientation;

  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  Map<Vec3i, Vec3i> edges = new HashMap<>();

  public TopologyStructure() {
  }

  public TopologyStructure(
      Vec3i center,
      Vec2i size2i,
      VillageTopologyStructureType type,
      Rotation rotation) {
    this(center, size2i, type, rotation, 0);
  }

  public TopologyStructure(
      Vec3i center,
      Vec2i size2i,
      VillageTopologyStructureType type,
      Rotation rotation,
      double distanceFromCenter) {
    super(createCenteredOffset(size2i), center, rotation);
    this.topologyType = type;
    this.distanceFromCenter = distanceFromCenter;
    this.orientation = rotation == null ? Rotation.None : rotation;
    rebuildEdges();
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

  public Vec3i getEdge(Vec3i normalizedDirection) {
    Vec3i edge = this.edges.get(normalizedDirection);
    return Vec3i.from(edge);
  }

  public Vec3i getForwardVector() {
    return Vec3i.from(getOrientation().getAxisDirection());
  }

  public Vec3i getBackVector() {
    return Vec3i.from(getOrientation().add(Rotation.OneEighty).getAxisDirection());
  }

  public Vec3i getLeftVector() {
    return Vec3i.from(getOrientation().add(Rotation.Ninety).getAxisDirection());
  }

  public Vec3i getRightVector() {
    return Vec3i.from(getOrientation().add(Rotation.TwoSeventy).getAxisDirection());
  }

  public Vec3i getFrontEdge() {
    return getEdge(getForwardVector());
  }

  public Vec3i getBackEdge() {
    return getEdge(getBackVector());
  }

  public Vec3i getLeftEdge() {
    return getEdge(getLeftVector());
  }

  public Vec3i getRightEdge() {
    return getEdge(getRightVector());
  }

  @Override
  public void setTopLeft(Vec2i topLeft) {
    super.setTopLeft(topLeft);
    rebuildEdges();
  }

  @Override
  public void setBotRight(Vec2i botRight) {
    super.setBotRight(botRight);
    rebuildEdges();
  }

  @Override
  public void setY(int y) {
    super.setY(y);
    rebuildEdges();
  }

  public Vec3i getCenter() {
    Vec2i center2D = getCenter2D();
    if (center2D == null) {
      return null;
    }

    return new Vec3i(center2D.x, getY(), center2D.y);
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

  private static AreaFootprint createCenteredOffset(Vec2i size2i) {
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
        new Vec2i(localMinX, localMinZ),
        new Vec2i(localMaxX, localMaxZ));
  }

  @Override
  public TopologyStructure clone() {
    TopologyStructure cloned = TopologyStructureCodec.clone(this);
    cloned.setY(this.getY());
    cloned.setTopLeft(Vec2i.from(this.getTopLeft()));
    cloned.setBotRight(Vec2i.from(this.getBotRight()));
    cloned.setPrefabAnchor(Vec3i.from(this.prefabAnchor));
    return cloned;
  }

  @Override
  public String toString() {
    return ("TopologyStructure: {\n\tStructureId: %s,\n\tTopologyType: %s,\n\tTopLeft: %s," +
        "\n\tBotRight: %s,\n\tY: %d,\n\tDistanceFromCenter: %f\n\t}")
        .formatted(this.structureId, this.topologyType, getTopLeft(), getBotRight(), getY(),
            this.distanceFromCenter);
  }

  private HashMap<Vec3i, Vec3i> buildEdges() {
    Vec2i axisAlignedTopLeft = getAxisAlignedTopLeft();
    Vec2i axisAlignedBotRight = getAxisAlignedBotRight();
    Vec2i center2D = getCenter2D();
    if (axisAlignedTopLeft == null || axisAlignedBotRight == null || center2D == null) {
      return new HashMap<>();
    }

    HashMap<Vec3i, Vec3i> computedEdges = new HashMap<>(4);
    int y = getY();
    int centerX = center2D.x;
    int centerZ = center2D.y;

    computedEdges.put(Vec3i.NORTH, new Vec3i(centerX, y, axisAlignedTopLeft.y));
    computedEdges.put(Vec3i.WEST, new Vec3i(axisAlignedTopLeft.x, y, centerZ));
    computedEdges.put(Vec3i.EAST, new Vec3i(axisAlignedBotRight.x, y, centerZ));
    computedEdges.put(Vec3i.SOUTH, new Vec3i(centerX, y, axisAlignedBotRight.y));
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
}
