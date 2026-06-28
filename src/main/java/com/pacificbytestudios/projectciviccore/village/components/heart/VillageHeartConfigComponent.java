package com.pacificbytestudios.projectciviccore.village.components.heart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialDescription;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import lombok.Getter;
import lombok.Setter;

@SerializableClass
public class VillageHeartConfigComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageHeartConfigComponent> CODEC = VillageHeartConfigComponentCodec.CODEC;

  @Getter
  @SerialName("MaxVillageLevel")
  @SerialDescription("The maximum village reachable level")
  short maxLevel;

  @SerialDescription("Enables debug functionalities")
  Boolean debug = false;

  @SerialName("StructureLimitsPerLevel")
  @SerialDescription("Defines for each village level the max structure count")
  Map<Short, Map<StructureType, Short>> structureCountLimitPerLevel = new HashMap<>();

  @SerialDescription("The village claim area per village level")
  Map<Short, Short> claimRadiusPerLevel = new HashMap<>();

  @SerialDescription("The maximum villager count per village level")
  Map<Short, Short> villagerCapPerLevel = new HashMap<>();

  @Getter
  @SerialDescription("Chance of spawning a side street")
  short fallbackMainRoadTurnChance;

  @Getter
  @SerialDescription("Chance of a road extension spawning after one building operation")
  short postBuildSideRoadChance;

  @SerialDescription("For each village level defines the max structure level reachable")
  Map<Short, Map<StructureType, Short>> structureMaxLevelPerVillageLevel = new HashMap<>();

  @Getter
  @Setter
  @SerialDescription("Possible village first name")
  String[] firstVillageNameComponents;

  @Getter
  @Setter
  @SerialDescription("Possible village second name")
  String[] secondVillageNameComponents;

  @Getter
  @Setter
  @SerialDescription("Height of the village's triggervolume above the ground")
  short triggerVolumeHeightAboveGround;

  @Getter
  @Setter
  @SerialDescription("Height of the village's triggervolume below the ground")
  short triggerVolumeHeightBelowGround;

  @Getter
  @Setter
  @SerialDescription("Sets the cooldown between village's welcome message popup")
  short welcomeMessageCooldown;

  public Boolean isDebugActive() {
    return debug;
  }

  public short getClaimRadiusPerLevel(short level) {
    return this.claimRadiusPerLevel.getOrDefault(level, (short) 0);
  }

  public short getVillagerCapPerLevel(short level) {
    return this.villagerCapPerLevel.getOrDefault(level, (short) 0);
  }

  public Map<StructureType, Short> getStructureLimitsPerLevel(short level) {
    Map<StructureType, Short> structureLimits = this.structureCountLimitPerLevel.get(level);
    if (structureLimits == null || structureLimits.isEmpty()) {
      return Collections.emptyMap();
    }

    return Collections.unmodifiableMap(structureLimits);
  }

  public Map<StructureType, Short> getStructureMaxLevelPerLevel(short level) {
    Map<StructureType, Short> structureLimits = this.structureMaxLevelPerVillageLevel.get(level);
    if (structureLimits == null || structureLimits.isEmpty()) {
      return Collections.emptyMap();
    }

    return Collections.unmodifiableMap(structureLimits);
  }

  public int getMaxRadius() {
    return getClaimRadiusPerLevel(getMaxLevel());
  }

  @Override
  public VillageHeartConfigComponent clone() {
    return VillageHeartConfigComponentCodec.clone(this);
  }
}
