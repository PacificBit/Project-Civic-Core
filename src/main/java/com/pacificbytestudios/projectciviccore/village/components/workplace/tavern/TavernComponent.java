package com.pacificbytestudios.projectciviccore.village.components.workplace.tavern;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.core.interfaces.IWorkplaceMissingResource;

@SerializableClass
public class TavernComponent implements Component<ChunkStore>, IWorkplaceMissingResource {
  public static final BuilderCodec<TavernComponent> CODEC = TavernComponentCodec.CODEC;

  @SerialName("MealToPrepareMap")
  Map<String, Integer> mealToPrepare = new HashMap<>();
  Integer mealToPrepareCount = 0;
  Map<UUID, String> assignedRecipesToWorker = new HashMap<>();
  Set<ItemRequestCriterion> missingResources = new HashSet<>();

  public Map<String, Integer> getMealToPrepare() {
    return mealToPrepare;
  }

  public String getNextMealToPrepare() {
    Iterator<String> iterator = this.mealToPrepare.keySet().iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }

  public void setMealToPrepare(Map<String, Integer> mealToPrepare) {
    this.mealToPrepare = mealToPrepare;
    this.mealToPrepareCount = 0;
    this.mealToPrepare.entrySet().forEach(entry -> {
      this.mealToPrepareCount += entry.getValue();
    });
  }

  public void addMealToPrepare(String meal, Integer amount) {
    int count = this.mealToPrepare.getOrDefault(meal, 0);
    this.mealToPrepare.put(meal, count + amount);
    this.mealToPrepareCount += amount;
  }

  public void removeMealFromPlan(String meal) {
    Integer count = this.mealToPrepare.get(meal);
    if (count == null) {
      return;
    } else if (count <= 1) {
      this.mealToPrepareCount -= 1;
      this.mealToPrepare.remove(meal);
      return;
    }
    this.mealToPrepareCount -= 1;
    this.mealToPrepare.put(meal, count - 1);
  }

  public boolean hasMealInQueue() {
    return this.mealToPrepareCount > 0;
  }

  public void registerWorkerToRecipe(UUID workerId, String recipe) {
    if (workerId == null || recipe == null) {
      return;
    }
    this.assignedRecipesToWorker.put(workerId, recipe);
  }

  @Override
  public Set<ItemRequestCriterion> getMissingResources() {
    return missingResources;
  }

  @Override
  public void setMissingResource(List<ItemRequestCriterion> criterion) {
    this.missingResources.addAll(criterion);
  }

  @Override
  public void newMissingResouceBuffer() {
    this.missingResources = new HashSet<>();
  }

  @Override
  public void addMissingResource(ItemRequestCriterion criterion) {
    this.missingResources.add(criterion);
  }

  @Override
  public void resetMissingResources() {
    this.newMissingResouceBuffer();
  }

  public String getAssignedRecipe(UUID workerId) {
    return this.assignedRecipesToWorker.get(workerId);
  }

  public int getAssignedRecipeCount() {
    return this.assignedRecipesToWorker.size();
  }

  public void markWorkerAsFree(UUID workerId) {
    this.assignedRecipesToWorker.remove(workerId);
  }

  public Integer getMealToPrepareCount() {
    return this.mealToPrepareCount;
  }

  @Override
  public TavernComponent clone() {
    return TavernComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, TavernComponent> getComponentType() {
    return ProjectCivicCore.get().getTavernComponentType();
  }

  @Override
  public String toString() {
    return "TavernComponent{" +
        "\n mealToPrepare=" + mealToPrepare + "," +
        "\n mealToPrepareCount=" + mealToPrepareCount + "," +
        "\n assignedRecipesToWorker=" + assignedRecipesToWorker + "," +
        "\n missingResources=" + missingResources + "," +
        "\n}";
  }

}
