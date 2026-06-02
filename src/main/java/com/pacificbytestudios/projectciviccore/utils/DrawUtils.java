package com.pacificbytestudios.projectciviccore.utils;

import javax.annotation.Nonnull;

import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;

public class DrawUtils extends DebugUtils {
    public static final float FOREVER = Float.MAX_VALUE;

    public static void addCuboid(@Nonnull World world, @Nonnull Vector3d center, @Nonnull Vector3d size,
            @Nonnull Vector3f color, float opacity, float seconds) {
        addCuboid(world, center, size, color, opacity, seconds, DebugUtils.FLAG_NONE);
    }

    public static void addCuboid(@Nonnull World world, @Nonnull Vector3d center, @Nonnull Vector3d size,
            @Nonnull Vector3f color, float opacity,
            float seconds, int flags) {
        Vector3d halfSize = new Vector3d(size).mul(0.5);

        Vector3d min = new Vector3d(center).sub(halfSize);
        Vector3d max = new Vector3d(center).add(halfSize);

        // TODO: Understand the logic of the spawn prefab on the actual hytale grid
        // FIXME: Ceil doesn't work when the tavern spawns on the opposite side of the
        // street
        min.set(Math.ceil(min.x), Math.ceil(min.y), Math.ceil(min.z));
        max.set(Math.ceil(max.x), Math.ceil(max.y), Math.ceil(max.z));

        Vector3d alignedCenter = new Vector3d(min).add(max).mul(0.5);
        Vector3d alignedSize = new Vector3d(max).sub(min);

        Matrix4d matrix = new Matrix4d().translation(alignedCenter).scale(alignedSize.x, alignedSize.y, alignedSize.z);
        add(world, DebugShape.Cube, matrix, color, opacity, seconds, flags);
    }
}
