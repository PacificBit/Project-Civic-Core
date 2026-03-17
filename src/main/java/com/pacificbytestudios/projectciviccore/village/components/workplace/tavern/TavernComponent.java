package com.pacificbytestudios.projectciviccore.village.components.workplace.tavern;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.core.interfaces.IWorkplaceMissingResource;

public class TavernComponent implements Component<ChunkStore>, IWorkplaceMissingResource {
  public static final BuilderCodec<TavernComponent> CODEC = BuilderCodec
      .builder(TavernComponent.class, TavernComponent::new)

      .append(new KeyedCodec<>(
          "MealToPrepareMap",
          new MapCodec<Integer, HashMap<String, Integer>>(Codec.INTEGER, HashMap::new)),
          (obj, value) -> obj.mealToPrepare = value,
          obj -> obj.mealToPrepare)
      .add()

      .append(new KeyedCodec<>("MealToPrepareCount", Codec.INTEGER),
          (obj, value) -> obj.mealToPrepareCount = value,
          (obj) -> obj.mealToPrepareCount)
      .add()

      .append(
          new KeyedCodec<>(
              "AssignedRecipesToWorker",
              new MapCodec<String, HashMap<String, String>>(
                  Codec.STRING,
                  HashMap::new)),
          (obj, value) -> obj.assignedRecipesToWorker = decode(value),
          obj -> encode(obj.assignedRecipesToWorker))
      .add()

      .append(new KeyedCodec<Set<ItemRequestCriterion>>(
          "MissingResources",
          new SetCodec<ItemRequestCriterion, HashSet<ItemRequestCriterion>>(
              ItemRequestCriterion.CODEC,
              HashSet::new,
              false)),
          (obj, value) -> obj.missingResources = value,
          obj -> obj.missingResources)
      .add()

      .build();

  public TavernComponent() {
  }

  private Map<String, Integer> mealToPrepare = new HashMap<>();
  private Integer mealToPrepareCount = 0;
  private Map<UUID, String> assignedRecipesToWorker = new HashMap<>();
  private Set<ItemRequestCriterion> missingResources = new HashSet<>();

  public Map<String, Integer> getMealToPrepare() {
    return mealToPrepare;
  }

  public String getNextMealToPrepare() {
    var iterator = this.mealToPrepare.keySet().iterator();
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
    TavernComponent component = new TavernComponent();
    component.mealToPrepare = new HashMap<>();
    component.mealToPrepareCount = this.mealToPrepareCount;

    for (Entry<String, Integer> meal : this.mealToPrepare.entrySet()) {
      mealToPrepare.put(meal.getKey(), meal.getValue());
    }

    component.missingResources = new HashSet<>();

    for (ItemRequestCriterion criterion : this.missingResources) {
      component.missingResources.add(criterion != null ? criterion.clone() : null);
    }

    component.assignedRecipesToWorker = deepCopy(this.assignedRecipesToWorker);
    return component;
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

  private static HashMap<String, String> encode(Map<UUID, String> value) {
    HashMap<String, String> encoded = new HashMap<>();

    if (value == null) {
      return encoded;
    }

    value.forEach((k, v) -> encoded.put(k.toString(), v));
    return encoded;
  }

  private static Map<UUID, String> decode(Map<String, String> value) {
    Map<UUID, String> decoded = new HashMap<>();

    if (value == null) {
      return decoded;
    }

    value.forEach((k, v) -> decoded.put(UUID.fromString(k), v));
    return decoded;
  }

  private static Map<UUID, String> deepCopy(Map<UUID, String> source) {
    Map<UUID, String> copy = new HashMap<>();

    if (source == null) {
      return copy;
    }

    source.forEach((k, v) -> copy.put(k, v));
    return copy;
  }
}
