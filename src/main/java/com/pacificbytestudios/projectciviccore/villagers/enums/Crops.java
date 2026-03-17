package com.pacificbytestudios.projectciviccore.villagers.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Crops {
  SUNFLOWER(
      "Plant_Seeds_Sunflower", "Plant_Sunflower_Block",
      null, null),

  WHEAT(
      "Plant_Seeds_Wheat", "Plant_Crop_Wheat_Block",
      "Plant_Seeds_Wheat_Eternal", "Plant_Crop_Wheat_Block_Eternal"),
  LETTUCE(
      "Plant_Seeds_Lettuce", "Plant_Crop_Lettuce_Block",
      "Plant_Seeds_Lettuce_Eternal", "Plant_Crop_Lettuce_Block_Eternal"),
  CARROT(
      "Plant_Seeds_Carrot", "Plant_Crop_Carrot_Block",
      "Plant_Seeds_Carrot_Eternal", "Plant_Crop_Carrot_Block_Eternal"),
  CORN(
      "Plant_Seeds_Corn", "Plant_Crop_Corn_Block",
      "Plant_Seeds_Corn_Eternal", "Plant_Crop_Corn_Block_Eternal"),
  CAULIFLOWER(
      "Plant_Seeds_Cauliflower", "Plant_Crop_Cauliflower_Block",
      "Plant_Seeds_Cauliflower_Eternal", "Plant_Crop_Cauliflower_Block_Eternal"),
  TURNIP(
      "Plant_Seeds_Turnip", "Plant_Crop_Turnip_Block",
      "Plant_Seeds_Turnip_Eternal", "Plant_Crop_Turnip_Block_Eternal"),
  AUBERGINE(
      "Plant_Seeds_Aubergine", "Plant_Crop_Aubergine_Block",
      "Plant_Seeds_Aubergine_Eternal", "Plant_Crop_Aubergine_Block_Eternal"),
  PUMPKIN(
      "Plant_Seeds_Pumpkin", "Plant_Crop_Pumpkin_Block",
      "Plant_Seeds_Pumpkin_Eternal", "Plant_Crop_Pumpkin_Block_Eternal"),
  TOMATO(
      "Plant_Seeds_Tomato", "Plant_Crop_Tomato_Block",
      "Plant_Seeds_Tomato_Eternal", "Plant_Crop_Tomato_Block_Eternal"),
  CHILI(
      "Plant_Seeds_Chilli", "Plant_Crop_Chilli_Block",
      "Plant_Seeds_Chilli_Eternal", "Plant_Crop_Chilli_Block_Eternal"),
  COTTON(
      "Plant_Seeds_Cotton", "Plant_Crop_Cotton_Block",
      "Plant_Seeds_Cotton_Eternal", "Plant_Crop_Cotton_Block_Eternal"),
  RICE(
      "Plant_Seeds_Rice", "Plant_Crop_Rice_Block",
      "Plant_Seeds_Rice_Eternal", "Plant_Crop_Rice_Block_Eternal"),
  ONION(
      "Plant_Seeds_Onion", "Plant_Crop_Onion_Block",
      "Plant_Seeds_Onion_Eternal", "Plant_Crop_Onion_Block_Eternal"),
  POTATO(
      "Plant_Seeds_Potato", "Plant_Crop_Potato_Block",
      "Plant_Seeds_Potato_Eternal", "Plant_Crop_Potato_Block_Eternal");

  private static final Map<String, String> SEED_TO_BLOCK;

  static {
    Map<String, String> map = new HashMap<>();

    for (Crops crop : values()) {
      if (crop.seedId != null && crop.blockId != null) {
        map.put(crop.seedId, crop.blockId);
      }
      if (crop.eternalSeedId != null && crop.eternalBlockId != null) {
        map.put(crop.eternalSeedId, crop.eternalBlockId);
      }
    }

    SEED_TO_BLOCK = Collections.unmodifiableMap(map);
  }

  private final String seedId;
  private final String blockId;
  private final String eternalSeedId;
  private final String eternalBlockId;

  Crops(String seedId, String blockId, String eternalSeedId, String eternalBlockId) {
    this.seedId = seedId;
    this.blockId = blockId;
    this.eternalSeedId = eternalSeedId;
    this.eternalBlockId = eternalBlockId;
  }

  public static String getBlockFromSeed(String seedId) {
    return SEED_TO_BLOCK.get(seedId);
  }

  public String getSeedId() {
    return this.seedId;
  }
}
