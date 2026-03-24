package com.pacificbytestudios.projectciviccore.codecs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class LateralVacancySegments {
  public static final BuilderCodec<LateralVacancySegments> CODEC = BuilderCodec
      .builder(LateralVacancySegments.class, LateralVacancySegments::new)

      .append(new KeyedCodec<>("ValidSegments",
          new ArrayCodec<>(
              Segment.CODEC,
              Segment[]::new)),
          (obj, value) -> {
            obj.validSegments = new ArrayList<>();
            if (value != null) {
              for (Segment segment : value) {
                obj.validSegments.add(segment);
              }
            }
          },
          (obj) -> obj.validSegments == null ? new Segment[0] : obj.validSegments.toArray(new Segment[0]))
      .add()

      .append(new KeyedCodec<>("OrientationVec", Vector3i.CODEC),
          (obj, value) -> obj.roadDirectionVec = value,
          (obj) -> obj.roadDirectionVec)
      .add()

      .build();

  private List<Segment> validSegments = new ArrayList<>();
  private Vector3i roadDirectionVec;

  public List<Segment> getValidSegments() {
    return validSegments;
  }

  public void appendSegment(Segment segment) {
    if (this.validSegments == null) {
      this.validSegments = new ArrayList<>();
    }

    if (segment.size == null) {
      segment.recalculateSize();
    }

    if (this.roadDirectionVec == null) {
      this.validSegments.add(segment);
      return;
    }

    int mergedStart = Math.min(project(segment.start), project(segment.end));
    int mergedEnd = Math.max(project(segment.start), project(segment.end));
    Segment mergedTemplate = segment;
    List<Segment> updatedSegments = new ArrayList<>();

    for (Segment validSegment : this.validSegments) {
      if (validSegment == null || validSegment.start == null || validSegment.end == null) {
        continue;
      }

      int validStart = Math.min(project(validSegment.start), project(validSegment.end));
      int validEnd = Math.max(project(validSegment.start), project(validSegment.end));

      if (validEnd + 1 < mergedStart || validStart - 1 > mergedEnd) {
        updatedSegments.add(validSegment);
        continue;
      }

      mergedStart = Math.min(mergedStart, validStart);
      mergedEnd = Math.max(mergedEnd, validEnd);
      mergedTemplate = validSegment;
    }

    updatedSegments.add(createSegment(mergedTemplate, mergedStart, mergedEnd));
    updatedSegments.sort(Comparator.comparingInt(this::getSegmentMinProjection));
    this.validSegments = updatedSegments;
  }

  public Segment buildSegmentFromEnd(short size) {
    if (size <= 0) {
      return null;
    }

    Segment segment = getLastValidSegment(size);
    if (segment == null) {
      return null;
    }

    Vector3i end = segment.end.clone();
    Vector3i start = end.clone().add(this.roadDirectionVec.clone().scale(-(size - 1)));
    return new Segment(start, end);
  }

  public Segment buildSegmentAt(Vector3i start, Vector3i end) {
    int requestedStart = Math.min(project(start), project(end));
    int requestedEnd = Math.max(project(start), project(end));

    for (Segment validSegment : this.validSegments) {
      if (validSegment == null || validSegment.start == null || validSegment.end == null) {
        continue;
      }

      int validStart = Math.min(project(validSegment.start), project(validSegment.end));
      int validEnd = Math.max(project(validSegment.start), project(validSegment.end));

      if (requestedStart >= validStart && requestedEnd <= validEnd) {
        return new Segment(start.clone(), end.clone());
      }
    }

    return null;
  }

  public Segment buildSegmentFromStart(short size) {
    if (size <= 0) {
      return null;
    }

    Segment segment = getFirstValidSegment(size);
    if (segment == null) {
      return null;
    }

    Vector3i start = segment.start.clone();
    Vector3i end = start.clone().add(this.roadDirectionVec.clone().scale(size - 1));
    return new Segment(start, end);
  }

  public Segment getFirstValidSegment(short size) {
    for (Segment segment : this.validSegments) {
      if (segment.size == null && segment.start != null && segment.end != null) {
        segment.recalculateSize();
      }
      if (segment.size == null) {
        continue;
      }
      if (segment.size >= size) {
        return segment;
      }
    }
    return null;
  }

  public Segment getLastValidSegment(short size) {
    for (int i = this.validSegments.size() - 1; i >= 0; i--) {
      Segment segment = this.validSegments.get(i);
      if (segment == null) {
        continue;
      }
      if (segment.size == null && segment.start != null && segment.end != null) {
        segment.recalculateSize();
      }
      if (segment.size == null) {
        continue;
      }
      if (segment.size >= size) {
        return segment;
      }
    }
    return null;
  }

  public boolean hasFittinSegment(short size) {
    for (Segment segment : this.validSegments) {
      if (segment == null) {
        continue;
      }
      if (segment.size == null && segment.start != null && segment.end != null) {
        segment.recalculateSize();
      }
      if (segment.size == null) {
        continue;
      }
      if (segment.size >= size) {
        return true;
      }
    }
    return false;
  }

  public void removeSegment(Segment segment) {
    List<Segment> updatedSegments = new ArrayList<>();
    int removeStart = Math.min(project(segment.start), project(segment.end));
    int removeEnd = Math.max(project(segment.start), project(segment.end));

    for (Segment validSegment : this.validSegments) {
      if (validSegment == null || validSegment.start == null || validSegment.end == null) {
        continue;
      }

      int validStart = Math.min(project(validSegment.start), project(validSegment.end));
      int validEnd = Math.max(project(validSegment.start), project(validSegment.end));

      if (removeEnd < validStart || removeStart > validEnd) {
        updatedSegments.add(validSegment);
        continue;
      }

      int leftStart = validStart;
      int leftEnd = removeStart - 1;
      if (leftStart <= leftEnd) {
        updatedSegments.add(createSegment(validSegment, leftStart, leftEnd));
      }

      int rightStart = removeEnd + 1;
      int rightEnd = validEnd;
      if (rightStart <= rightEnd) {
        updatedSegments.add(createSegment(validSegment, rightStart, rightEnd));
      }
    }

    this.validSegments = updatedSegments;
  }

  private int project(Vector3i point) {
    return this.roadDirectionVec.x != 0
        ? point.x * this.roadDirectionVec.x
        : point.z * this.roadDirectionVec.z;
  }

  private Segment createSegment(Segment template, int rangeStart, int rangeEnd) {
    boolean preserveOrder = project(template.start) <= project(template.end);
    Vector3i start = preserveOrder
        ? fromProjection(template.start, rangeStart)
        : fromProjection(template.start, rangeEnd);
    Vector3i end = preserveOrder
        ? fromProjection(template.end, rangeEnd)
        : fromProjection(template.end, rangeStart);
    return new Segment(start, end);
  }

  private int getSegmentMinProjection(Segment segment) {
    return Math.min(project(segment.start), project(segment.end));
  }

  private Vector3i fromProjection(Vector3i template, int projection) {
    Vector3i result = template.clone();
    if (this.roadDirectionVec.x != 0) {
      result.x = projection * this.roadDirectionVec.x;
    } else {
      result.z = projection * this.roadDirectionVec.z;
    }
    return result;
  }

  public Vector3i getRoadDirectionVec() {
    return roadDirectionVec;
  }

  public void setRoadDirectionVec(Vector3i roadDirectionVec) {
    this.roadDirectionVec = roadDirectionVec;
  }

  @Override
  public LateralVacancySegments clone() {
    LateralVacancySegments adjacentSegments = new LateralVacancySegments();
    adjacentSegments.validSegments = new ArrayList<>();
    if (this.validSegments != null) {
      for (Segment segment : this.validSegments) {
        adjacentSegments.validSegments.add(segment == null ? null : segment.clone());
      }
    }

    adjacentSegments.roadDirectionVec = this.roadDirectionVec == null ? null : this.roadDirectionVec.clone();
    return adjacentSegments;
  }

  public static class Segment {
    public static final BuilderCodec<Segment> CODEC = BuilderCodec
        .builder(Segment.class, Segment::new)

        .append(new KeyedCodec<>("Start", Vector3i.CODEC),
            (obj, value) -> obj.start = value,
            (obj) -> obj.start)
        .add()

        .append(new KeyedCodec<>("End", Vector3i.CODEC),
            (obj, value) -> obj.end = value,
            (obj) -> obj.end)
        .add()

        .append(new KeyedCodec<>("Size", Codec.SHORT),
            (obj, value) -> obj.size = value,
            (obj) -> obj.size)
        .add()

        .build();

    protected Vector3i start;
    protected Vector3i end;
    protected Short size;

    public Segment() {
    }

    public Segment(Vector3i start, Vector3i end) {
      this.start = start;
      this.end = end;
      recalculateSize();
    }

    @Override
    public Segment clone() {
      Segment segment = new Segment();
      segment.start = this.start == null ? null : this.start.clone();
      segment.end = this.end == null ? null : this.end.clone();
      segment.size = this.size;
      return segment;
    }

    public Vector3i getStart() {
      return start;
    }

    public void setStart(Vector3i start) {
      this.start = start;
      recalculateSize();
    }

    public Vector3i getEnd() {
      return end;
    }

    public void setEnd(Vector3i end) {
      this.end = end;
      recalculateSize();
    }

    public boolean isDotInside(Vector3i dot) {
      int minX = Math.min(start.x, end.x);
      int maxX = Math.max(start.x, end.x);
      int minZ = Math.min(start.z, end.z);
      int maxZ = Math.max(start.z, end.z);
      return minX <= dot.x &&
          minZ <= dot.z &&
          maxX >= dot.x &&
          maxZ >= dot.z;
    }

    public Short getSize() {
      return size;
    }

    public void recalculateSize() {
      if (this.start == null || this.end == null) {
        this.size = null;
        return;
      }

      int size = this.end.x - this.start.x;
      if (size == 0) {
        size = this.end.z - this.start.z;
      }
      this.size = (short) (Math.abs(size) + 1);
    }

    @Override
    public String toString() {
      return "Segment [start=" + start + ", end=" + end + ", size=" + size + "]";
    }
  }
}
