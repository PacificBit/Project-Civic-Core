package com.pacificbytestudios.projectciviccore.codecs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class LateralVacancySegments {
  public static final BuilderCodec<LateralVacancySegments> CODEC = LateralVacancySegmentsCodec.CODEC;

  @Setter(lombok.AccessLevel.NONE)
  List<Segment> validSegments = new ArrayList<>();
  Vec3i roadDirectionVec;

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

    Vec3i end = Vec3i.from(segment.end);
    Vec3i start = Vec3i.from(end).add(
        Vec3i.from(this.roadDirectionVec).mul(-(size - 1)));
    return new Segment(start, end);
  }

  public Segment buildSegmentAt(Vec3i start, Vec3i end) {
    int requestedStart = Math.min(project(start), project(end));
    int requestedEnd = Math.max(project(start), project(end));

    for (Segment validSegment : this.validSegments) {
      if (validSegment == null || validSegment.start == null || validSegment.end == null) {
        continue;
      }

      int validStart = Math.min(project(validSegment.start), project(validSegment.end));
      int validEnd = Math.max(project(validSegment.start), project(validSegment.end));

      if (requestedStart >= validStart && requestedEnd <= validEnd) {
        return new Segment(Vec3i.from(start), Vec3i.from(end));
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

    Vec3i start = Vec3i.from(segment.start);
    Vec3i end = Vec3i.from(start).add(
        Vec3i.from(this.roadDirectionVec).mul(size - 1));
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

  private int project(Vec3i point) {
    return this.roadDirectionVec.x != 0
        ? point.x * this.roadDirectionVec.x
        : point.z * this.roadDirectionVec.z;
  }

  private Segment createSegment(Segment template, int rangeStart, int rangeEnd) {
    boolean preserveOrder = project(template.start) <= project(template.end);
    Vec3i start = preserveOrder
        ? fromProjection(template.start, rangeStart)
        : fromProjection(template.start, rangeEnd);
    Vec3i end = preserveOrder
        ? fromProjection(template.end, rangeEnd)
        : fromProjection(template.end, rangeStart);
    return new Segment(start, end);
  }

  private int getSegmentMinProjection(Segment segment) {
    return Math.min(project(segment.start), project(segment.end));
  }

  private Vec3i fromProjection(Vec3i template, int projection) {
    Vec3i result = Vec3i.from(template);
    if (this.roadDirectionVec.x != 0) {
      result.x = projection * this.roadDirectionVec.x;
    } else {
      result.z = projection * this.roadDirectionVec.z;
    }
    return result;
  }

  @Override
  public LateralVacancySegments clone() {
    return LateralVacancySegmentsCodec.clone(this);
  }

  @Getter
  @SerializableClass
  public static class Segment {
    public static final BuilderCodec<Segment> CODEC = SegmentCodec.CODEC;

    protected Vec3i start;
    protected Vec3i end;
    protected Short size;

    public Segment() {
    }

    public Segment(Vec3i start, Vec3i end) {
      this.start = start;
      this.end = end;
      this.size = calculateSize(start, end);
    }

    @Override
    public Segment clone() {
      return SegmentCodec.clone(this);
    }

    public void setStart(Vec3i start) {
      this.start = start;
      recalculateSize();
    }

    public void setEnd(Vec3i end) {
      this.end = end;
      recalculateSize();
    }

    public boolean isDotInside(Vec3i dot) {
      int minX = Math.min(start.x, end.x);
      int maxX = Math.max(start.x, end.x);
      int minZ = Math.min(start.z, end.z);
      int maxZ = Math.max(start.z, end.z);
      return minX <= dot.x &&
          minZ <= dot.z &&
          maxX >= dot.x &&
          maxZ >= dot.z;
    }

    public void recalculateSize() {
      this.size = calculateSize(this.start, this.end);
    }

    private static Short calculateSize(Vec3i start, Vec3i end) {
      if (start == null || end == null) {
        return null;
      }

      int size = end.x - start.x;
      if (size == 0) {
        size = end.z - start.z;
      }
      return (short) (Math.abs(size) + 1);
    }

    @Override
    public String toString() {
      return "Segment: { Start: %s, End: %s, Size: %d }".formatted(this.start, this.end, this.size);
    }
  }
}
