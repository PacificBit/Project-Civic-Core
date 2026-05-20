package com.pacificbytestudios.projectciviccore.villagers.core.utils;

import org.joml.Vector3i;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public class ScanUtils {
  private static final String EMPTY_BLOCK_ID = "Empty";
  private static final String STATE_DEFINITIONS_MARKER = "_State_Definitions_";

  public static boolean isInsideWorkArea(
      Vector3i point,
      int minX,
      int maxX,
      int minZ,
      int maxZ,
      int soilLevel) {
    return point.x >= minX
        && point.x <= maxX
        && point.z >= minZ
        && point.z <= maxZ
        && point.y == soilLevel;
  }

  public static int clamp(int value, int min, int max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  public static boolean isEmptyBlock(BlockType blockType) {
    return matchesBaseId(getBlockId(blockType), EMPTY_BLOCK_ID);
  }

  public static String getBlockId(BlockType blockType) {
    if (blockType == null || blockType.getId() == null) {
      return "<null>";
    }
    return blockType.getId();
  }

  public static boolean matchesBaseId(String actualId, String expectedBaseId) {
    String normalized = normalizeBlockId(actualId);
    return normalized != null && normalized.equals(expectedBaseId);
  }

  private static String normalizeBlockId(String blockId) {
    if (blockId == null || "<null>".equals(blockId)) {
      return null;
    }

    String normalized = blockId;

    while (normalized.startsWith("*")) {
      normalized = normalized.substring(1);
    }

    while (normalized.endsWith("*")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }

    int stateIndex = normalized.indexOf(STATE_DEFINITIONS_MARKER);
    if (stateIndex >= 0) {
      normalized = normalized.substring(0, stateIndex);
    }

    return normalized;
  }

  public static Vector3i getNextPoint(
      Vector3i currentPoint,
      int minX,
      int maxX,
      int minZ,
      int maxZ,
      int soilLevel) {

    if (currentPoint.x < maxX) {
      return new Vector3i(currentPoint.x + 1, soilLevel, currentPoint.z);
    }

    if (currentPoint.z < maxZ) {
      return new Vector3i(minX, soilLevel, currentPoint.z + 1);
    }

    return null;
  }
}
