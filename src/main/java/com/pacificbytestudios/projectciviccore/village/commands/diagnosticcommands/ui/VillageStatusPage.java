package com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.ui;

import java.util.Map;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

// NOTE: This will be removed when Noesis will come out
@Deprecated
public class VillageStatusPage extends BasicCustomUIPage {
  private static final String PAGE_DOCUMENT = "Pages/Village/VillageStatusPage.ui";

  private static final String ICON_LEVEL = "Icons/ItemCategories/Natural-Fire.png";
  private static final String ICON_VILLAGERS = "Icons/ItemCategories/Items-Clothes.png";
  private static final String ICON_BEDS = "Icons/ItemCategories/Build-Beds.png";
  private static final String ICON_WORK = "Common/Icons/ItemCategories/Items-Tools.png";
  private static final String ICON_HEALTH_OK = "Common/Icons/AssetNotifications/IconCheckmark@2x.png";
  private static final String ICON_HEALTH_WARN = "Common/Icons/AssetNotifications/IconAlert@2x.png";

  private final boolean mayorValid;
  private final int invalidNpcReferenceCount;
  private final short level;
  private final short villagerCount;
  private final short homelessCount;
  private final short emptyBedCount;
  private final short totalBedsCount;
  private final short occupiedBeds;
  private final short unemployedCount;
  private final boolean anyWorkplaceEmpty;
  private final Map<StructureType, Short> structureTypeMap;

  public VillageStatusPage(
      PlayerRef playerRef,
      boolean mayorValid,
      int invalidNpcReferenceCount,
      short level,
      short villagerCount,
      short homelessCount,
      short emptyBedCount,
      short totalBedsCount,
      short occupiedBeds,
      short unemployedCount,
      boolean anyWorkplaceEmpty,
      Map<StructureType, Short> structureTypeMap) {
    super(playerRef, CustomPageLifetime.CanDismiss);
    this.mayorValid = mayorValid;
    this.invalidNpcReferenceCount = invalidNpcReferenceCount;
    this.level = level;
    this.villagerCount = villagerCount;
    this.homelessCount = homelessCount;
    this.emptyBedCount = emptyBedCount;
    this.totalBedsCount = totalBedsCount;
    this.occupiedBeds = occupiedBeds;
    this.unemployedCount = unemployedCount;
    this.anyWorkplaceEmpty = anyWorkplaceEmpty;
    this.structureTypeMap = structureTypeMap;
  }

  @Override
  public void build(UICommandBuilder builder) {
    builder.append(PAGE_DOCUMENT);

    builder.set("#LevelIcon.AssetPath", ICON_LEVEL);
    builder.set("#VillagersIcon.AssetPath", ICON_VILLAGERS);
    builder.set("#BedsIcon.AssetPath", ICON_BEDS);
    builder.set("#WorkIcon.AssetPath", ICON_WORK);
    builder.set("#MayorIcon.AssetPath", this.mayorValid ? ICON_HEALTH_OK : ICON_HEALTH_WARN);
    builder.set("#NpcRefsIcon.AssetPath", this.invalidNpcReferenceCount == 0 ? ICON_HEALTH_OK : ICON_HEALTH_WARN);

    this.setText(builder, "LevelValue", Short.toString(this.level));
    this.setText(builder, "VillagersValue", Short.toString(this.villagerCount));
    this.setText(builder, "BedsValue", this.occupiedBeds + " / " + this.totalBedsCount);
    this.setText(builder, "EmptyBedsValue", Short.toString(this.emptyBedCount));
    this.setText(builder, "HomelessValue", Short.toString(this.homelessCount));
    this.setText(builder, "UnemployedValue", Short.toString(this.unemployedCount));
    this.setText(builder, "WorkplaceVacancyValue", this.anyWorkplaceEmpty ? "Needs assignment" : "Fully assigned");

    this.setStatus(
        builder,
        "MayorStatusValue",
        this.mayorValid ? "valid" : "invalid",
        this.mayorValid);
    this.setStatus(
        builder,
        "NpcRefsValue",
        this.invalidNpcReferenceCount == 0 ? "All valid" : this.invalidNpcReferenceCount + " invalid",
        this.invalidNpcReferenceCount == 0);

    this.setStructure(builder, StructureType.HOUSE);
    this.setStructure(builder, StructureType.TOWN_HALL);
    this.setStructure(builder, StructureType.FARM);
    this.setStructure(builder, StructureType.MINE);
    this.setStructure(builder, StructureType.SAWMILL);
    this.setStructure(builder, StructureType.STORAGE);
    this.setStructure(builder, StructureType.TAVERN);
  }

  private void setStructure(UICommandBuilder builder, StructureType type) {
    this.setText(builder, this.toStructureValueId(type),
        Short.toString(this.structureTypeMap.getOrDefault(type, (short) 0)));
  }

  private String toStructureValueId(StructureType type) {
    return switch (type) {
      case HOUSE -> "HouseValue";
      case TOWN_HALL -> "TownHallValue";
      case FARM -> "FarmValue";
      case MINE -> "MineValue";
      case SAWMILL -> "SawmillValue";
      case STORAGE -> "StorageValue";
      case TAVERN -> "TavernValue";
    };
  }

  private void setStatus(UICommandBuilder builder, String id, String text, boolean ok) {
    builder.set("#" + id + ".TextSpans", Message.raw(text).color(ok ? "#73d07d" : "#f0b85a"));
  }

  private void setText(UICommandBuilder builder, String id, String text) {
    builder.set("#" + id + ".TextSpans", Message.raw(text));
  }
}
