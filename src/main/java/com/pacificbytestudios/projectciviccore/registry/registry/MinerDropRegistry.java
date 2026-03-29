package com.pacificbytestudios.projectciviccore.registry.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.pacificbytestudios.projectciviccore.registry.enums.MidQualityMinerDrop;
import com.pacificbytestudios.projectciviccore.registry.enums.TopQualityMinerDrop;

public class MinerDropRegistry {
  private static final List<WeightedDrop> topQualityDrops = new ArrayList<>();
  private static final List<WeightedDrop> midQualityDrops = new ArrayList<>();

  private static boolean alreadyInizialized = false;

  public static void initizalize() {
    if (alreadyInizialized) {
      return;
    }
    alreadyInizialized = true;

    initializeDropTier(midQualityDrops, MidQualityMinerDrop.values());
    initializeDropTier(topQualityDrops, TopQualityMinerDrop.values());
  }

  public static String getDropByVillageLevel(int villageLevel) {
    initizalize();

    // Miner drops currently only define mid and top tiers, so early villages use
    // the mid pool.
    List<WeightedDrop> drops = villageLevel >= 3 ? topQualityDrops : midQualityDrops;
    return rollDrop(drops);
  }

  private static void initializeDropTier(List<WeightedDrop> tier, MidQualityMinerDrop[] drops) {
    tier.clear();
    for (MidQualityMinerDrop drop : drops) {
      tier.add(new WeightedDrop(drop.get(), drop.getProbability()));
    }
  }

  private static void initializeDropTier(List<WeightedDrop> tier, TopQualityMinerDrop[] drops) {
    tier.clear();
    for (TopQualityMinerDrop drop : drops) {
      tier.add(new WeightedDrop(drop.get(), drop.getProbability()));
    }
  }

  private static String rollDrop(List<WeightedDrop> drops) {
    if (drops == null || drops.isEmpty()) {
      return null;
    }

    int totalWeight = 0;
    for (WeightedDrop drop : drops) {
      totalWeight += Math.max(drop.probability, 0);
    }

    if (totalWeight <= 0) {
      return null;
    }

    int roll = ThreadLocalRandom.current().nextInt(totalWeight);
    for (WeightedDrop drop : drops) {
      roll -= Math.max(drop.probability, 0);
      if (roll < 0) {
        return drop.itemId;
      }
    }

    return drops.get(drops.size() - 1).itemId;
  }

  private static final class WeightedDrop {
    private final String itemId;
    private final int probability;

    private WeightedDrop(String itemId, int probability) {
      this.itemId = itemId;
      this.probability = probability;
    }
  }
}
