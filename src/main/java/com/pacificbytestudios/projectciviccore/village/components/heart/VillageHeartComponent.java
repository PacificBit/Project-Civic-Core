package com.pacificbytestudios.projectciviccore.village.components.heart;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.enums.VillageHeartState;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;

public class VillageHeartComponent implements Component<ChunkStore> {

  public static final BuilderCodec<VillageHeartComponent> CODEC = BuilderCodec
      .builder(VillageHeartComponent.class, VillageHeartComponent::new)

      .append(new KeyedCodec<>("VillageId", Codec.UUID_BINARY),
          (obj, value) -> obj.uuid = value,
          (obj) -> obj.uuid)
      .add()

      .append(new KeyedCodec<>("Config", VillageHeartConfigComponent.CODEC),
          (obj, value) -> obj.config = value,
          (obj) -> obj.config)
      .add()

      .append(new KeyedCodec<>("State", Codec.STRING),
          (obj, value) -> obj.state = VillageHeartState.valueOf(value.toUpperCase()),
          (obj) -> obj.state.getState())
      .add()

      .append(new KeyedCodec<>("Level", Codec.INTEGER),
          (obj, value) -> obj.level = value,
          (obj) -> obj.level)
      .add()

      .append(
          new KeyedCodec<>("ClaimedArea", ClaimedAreaComponent.CODEC),
          (obj, value) -> obj.claimedArea = value,
          (obj) -> obj.claimedArea)
      .add()

      .append(
          new KeyedCodec<>(
              "VillagersRegistry",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false)),
          (obj, value) -> obj.villagersRegistry = value,
          obj -> obj.villagersRegistry)
      .add()

      .append(
          new KeyedCodec<>(
              "UnemployedRegistry",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false)),
          (obj, value) -> obj.unemployedRegistry = value,
          obj -> obj.unemployedRegistry)
      .add()

      .append(
          new KeyedCodec<>(
              "VillagerIdToComponent",
              new MapCodec<VillagerComponent, HashMap<String, VillagerComponent>>(
                  VillagerComponent.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.villagerIdToComponent = new HashMap<>();
            value.forEach((k, v) -> obj.villagerIdToComponent.put(UUID.fromString(k), v));
          },
          obj -> {
            HashMap<String, VillagerComponent> encoded = new HashMap<>();
            obj.villagerIdToComponent.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .append(
          new KeyedCodec<>(
              "WorkplaceIdToWorkplace",
              new MapCodec<WorkplaceComponent, HashMap<String, WorkplaceComponent>>(
                  WorkplaceComponent.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.workplaceIdToWorkplace = new HashMap<>();
            value.forEach((k, v) -> obj.workplaceIdToWorkplace.put(UUID.fromString(k), v));
          },
          obj -> {
            HashMap<String, WorkplaceComponent> encoded = new HashMap<>();
            obj.workplaceIdToWorkplace.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .append(
          new KeyedCodec<>(
              "VillagerToWorkplaceMap",
              new MapCodec<UUID, HashMap<String, UUID>>(
                  Codec.UUID_BINARY,
                  HashMap::new)),
          (obj, value) -> {
            obj.villagerToWorkplace = new HashMap<>();
            value.forEach((k, v) -> obj.villagerToWorkplace.put(UUID.fromString(k), v));
          },
          obj -> {
            HashMap<String, UUID> encoded = new HashMap<>();
            obj.villagerToWorkplace.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .append(
          new KeyedCodec<>(
              "VillageStorageWorkplaces",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false)),
          (obj, value) -> obj.villageStorageWorkplaces = value,
          obj -> obj.villageStorageWorkplaces)
      .add()

      .append(
          new KeyedCodec<>(
              "VillageTavernWorkplaces",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false)),
          (obj, value) -> obj.villageTavernWorkplaces = value,
          obj -> obj.villageTavernWorkplaces)
      .add()

      .build();

  private UUID uuid;
  private VillageHeartConfigComponent config;
  private VillageHeartState state = VillageHeartState.NORMAL;
  private int level;
  private ClaimedAreaComponent claimedArea;

  private Set<UUID> villagersRegistry = new HashSet<>();
  private Set<UUID> unemployedRegistry = new HashSet<>();
  private Map<UUID, VillagerComponent> villagerIdToComponent = new HashMap<>();

  private Map<UUID, WorkplaceComponent> workplaceIdToWorkplace = new HashMap<>();
  private Map<UUID, UUID> villagerToWorkplace = new HashMap<>();
  private Set<UUID> villageStorageWorkplaces = new HashSet<>();
  private Set<UUID> villageTavernWorkplaces = new HashSet<>();

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public VillageHeartConfigComponent getConfig() {
    // INFO: Read only
    return config.clone();
  }

  public Set<UUID> getVillagersRegistry() {
    return Collections.unmodifiableSet(this.villagersRegistry);
  }

  public short getVillagerCount() {
    return (short) this.villagersRegistry.size();
  }

  public void registerVillager(UUID uuid, VillagerComponent villagerComponent) {
    this.villagersRegistry.add(uuid);
    this.villagerIdToComponent.put(uuid, villagerComponent);
  }

  public void unregisterVillager(UUID uuid) {
    this.villagersRegistry.remove(uuid);
    this.villagerIdToComponent.remove(uuid);
  }

  public Set<UUID> getUnemployedRegistry() {
    return Collections.unmodifiableSet(unemployedRegistry);
  }

  public void registerAsUnemployed(UUID uuid) {
    this.unemployedRegistry.add(uuid);
  }

  public void unregisterAsUnemployed(UUID uuid) {
    this.unemployedRegistry.remove(uuid);
  }

  public VillagerComponent getVillager(UUID uuid) {
    return this.villagerIdToComponent.get(uuid);
  }

  public VillageHeartState getState() {
    return this.state;
  }

  public void setState(VillageHeartState state) {
    this.state = state;
  }

  public ClaimedAreaComponent getClaimedArea() {
    return claimedArea;
  }

  public void setClaimedArea(ClaimedAreaComponent claimedChunks) {
    this.claimedArea = claimedChunks;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public void registerWorkplace(WorkplaceComponent workplace) {
    this.workplaceIdToWorkplace.put(workplace.getUuid(), workplace);

    WorkplaceType workplaceType = workplace.getWorkplaceType();
    if (workplaceType.equals(WorkplaceType.STORAGE)) {
      this.villageStorageWorkplaces.add(workplace.getUuid());
    } else if (workplaceType.equals(WorkplaceType.TAVERN)) {
      this.villageTavernWorkplaces.add(workplace.getUuid());
    }
  }

  public void unregisterWorkplace(UUID workplace) {
    if (!this.workplaceIdToWorkplace.containsKey(workplace)) {
      return;
    }
    this.workplaceIdToWorkplace.remove(workplace);

    if (this.villageStorageWorkplaces.contains(workplace)) {
      this.villageStorageWorkplaces.remove(workplace);
    } else if (this.villageTavernWorkplaces.contains(workplace)) {
      this.villageTavernWorkplaces.remove(workplace);
    }

    Iterator<Map.Entry<UUID, UUID>> workers = this.villagerToWorkplace.entrySet().iterator();
    while (workers.hasNext()) {
      Map.Entry<UUID, UUID> worker = workers.next();
      if (worker.getValue().equals(workplace)) {
        workers.remove();
        this.unemployedRegistry.add(worker.getKey());
        this.villagerIdToComponent.get(worker.getKey()).setWorkerData(null);
      }
    }
  }

  public void registerVillagerAsWorkerAt(UUID villager, UUID workplace) {
    this.unemployedRegistry.remove(villager);
    this.villagerToWorkplace.put(villager, workplace);
    WorkplaceComponent workplaceComponent = this.workplaceIdToWorkplace.get(workplace);
    workplaceComponent.registerWorker(villager);

    VillagerWorkerComponent workerData = new VillagerWorkerComponent();
    workerData.setWorkplace(workplaceComponent);
    // TODO: this is not flexible enough for the future
    workerData.setTypeOfWorker(workplaceComponent.getWorkerType()[0]);
    this.villagerIdToComponent.get(villager).setWorkerData(workerData);
  }

  public void markAsUnemployed(UUID villager) {
    UUID workplace = this.villagerToWorkplace.get(villager);

    if (workplace == null) {
      return;
    }

    this.workplaceIdToWorkplace.get(workplace).unregisterWorker(villager);
    this.villagerToWorkplace.remove(villager);
    this.unemployedRegistry.add(villager);
    this.villagerIdToComponent.get(villager).setWorkerData(null);
  }

  public List<WorkplaceComponent> getWorkplaceFrom(WorkplaceType workplaceType) {
    return this.workplaceIdToWorkplace
        .values()
        .stream()
        .filter((workplace) -> workplace.getWorkplaceType().equals(workplaceType))
        .collect(Collectors.toList());
  }

  public Map<UUID, VillagerComponent> getVillagerIdToComponent() {
    return Collections.unmodifiableMap(this.villagerIdToComponent);
  }

  public Map<UUID, WorkplaceComponent> getWorkplaceIdToWorkplace() {
    return Collections.unmodifiableMap(this.workplaceIdToWorkplace);
  }

  public WorkplaceComponent getWorkplaceFromId(UUID uuid) {
    return this.workplaceIdToWorkplace.get(uuid);
  }

  public Map<UUID, UUID> getVillagerToWorkplace() {
    return Collections.unmodifiableMap(this.villagerToWorkplace);
  }

  public Set<UUID> getVillageStorageWorkplaces() {
    return Collections.unmodifiableSet(this.villageStorageWorkplaces);
  }

  public Set<UUID> getVillageTavernWorkplaces() {
    return Collections.unmodifiableSet(this.villageTavernWorkplaces);
  }

  @Override
  public VillageHeartComponent clone() {
    VillageHeartComponent component = new VillageHeartComponent();

    component.uuid = this.uuid;
    component.state = this.state;
    component.level = this.level;

    component.config = this.config != null ? this.config.clone() : null;
    component.claimedArea = this.claimedArea != null ? this.claimedArea.clone() : null;

    component.villagersRegistry = new HashSet<>(this.villagersRegistry);
    component.unemployedRegistry = new HashSet<>(this.unemployedRegistry);
    component.villagerToWorkplace = new HashMap<>(this.villagerToWorkplace);
    component.villageStorageWorkplaces = new HashSet<>(this.villageStorageWorkplaces);
    component.villageTavernWorkplaces = new HashSet<>(this.villageTavernWorkplaces);

    component.workplaceIdToWorkplace = new HashMap<>();
    for (Map.Entry<UUID, WorkplaceComponent> entry : this.workplaceIdToWorkplace.entrySet()) {
      WorkplaceComponent clonedWorkplace = entry.getValue() != null ? entry.getValue().clone() : null;
      component.workplaceIdToWorkplace.put(entry.getKey(), clonedWorkplace);
    }

    component.villagerIdToComponent = new HashMap<>();
    for (Map.Entry<UUID, VillagerComponent> entry : this.villagerIdToComponent.entrySet()) {
      VillagerComponent clonedVillager = entry.getValue() != null ? entry.getValue().clone() : null;
      component.villagerIdToComponent.put(entry.getKey(), clonedVillager);
    }

    for (Map.Entry<UUID, UUID> entry : component.villagerToWorkplace.entrySet()) {
      UUID villagerId = entry.getKey();
      VillagerComponent villager = component.villagerIdToComponent.get(villagerId);

      if (villager != null) {
        villager.setWorkerData(villager.getWorkerData());
      }
    }

    for (UUID unemployedVillagerId : component.unemployedRegistry) {
      VillagerComponent villager = component.villagerIdToComponent.get(unemployedVillagerId);
      if (villager != null) {
        villager.setWorkerData(null);
      }
    }

    return component;
  }

  public static ComponentType<ChunkStore, VillageHeartComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageCoreComponentType();
  }

}
