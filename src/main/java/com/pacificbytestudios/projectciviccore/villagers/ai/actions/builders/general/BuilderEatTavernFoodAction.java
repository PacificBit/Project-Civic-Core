package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.EatTavernFoodAction;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerMealType;

public class BuilderEatTavernFoodAction extends BuilderActionBase {
  private VillagerMealType mealType;

  @Override
  public Action build(BuilderSupport support) {
    return new EatTavernFoodAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Eat food on the plate";
  }

  @Override
  public String getShortDescription() {
    return "Eat food on the plate";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.getEnum(
        data,
        "MealType",
        this::setMealType,
        VillagerMealType.class,
        null,
        BuilderDescriptorState.Experimental,
        "Meal slot to mark as consumed",
        null);
    return this;
  }

  public VillagerMealType getMealType() {
    return this.mealType;
  }

  private void setMealType(VillagerMealType mealType) {
    this.mealType = mealType;
  }

}
