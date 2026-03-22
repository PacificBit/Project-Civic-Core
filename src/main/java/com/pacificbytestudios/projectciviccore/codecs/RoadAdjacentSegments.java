package com.pacificbytestudios.projectciviccore.codecs;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class RoadAdjacentSegments {
  public static final BuilderCodec<RoadAdjacentSegments> CODEC = BuilderCodec
      .builder(RoadAdjacentSegments.class, RoadAdjacentSegments::new)

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

      .build();

  private List<Segment> validSegments = new ArrayList<>();

  @Override
  public RoadAdjacentSegments clone() {
    RoadAdjacentSegments adjacentSegments = new RoadAdjacentSegments();
    adjacentSegments.validSegments = new ArrayList<>();
    if (this.validSegments != null) {
      for (Segment segment : this.validSegments) {
        adjacentSegments.validSegments.add(segment == null ? null : segment.clone());
      }
    }
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

        .build();

    private Vector3i start;
    private Vector3i end;

    @Override
    public Segment clone() {
      Segment segment = new Segment();
      segment.start = this.start == null ? null : this.start.clone();
      segment.end = this.end == null ? null : this.end.clone();
      return segment;
    }
  }
}
