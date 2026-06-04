package com.pacificbytestudios.projectciviccore.village.components.heart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.hypixel.hytale.builtin.triggervolumes.EntityTargetType;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.builtin.triggervolumes.effect.builtin.PlaySoundEffect;
import com.hypixel.hytale.builtin.triggervolumes.effect.builtin.ShowEventTitleEffect;
import com.hypixel.hytale.builtin.triggervolumes.effect.builtin.conditions.CooldownCondition;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.builtin.triggervolumes.shape.BoxShape;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.BlockContext;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.triggervolumeeffects.TeleportVillagerToTownHeartEffect;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageHeartState;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerRole;

public class VillageHeartComponent implements Component<ChunkStore> {

  public static final BuilderCodec<VillageHeartComponent> CODEC = BuilderCodec
      .builder(VillageHeartComponent.class, VillageHeartComponent::new)

      .append(new KeyedCodec<>("VillageId", Codec.UUID_BINARY),
          (obj, value) -> obj.uuid = value,
          (obj) -> obj.uuid)
      .add()

      .append(new KeyedCodec<>("VillageRef", EntityRef.CODEC),
          (obj, value) -> obj.villageRef = value,
          (obj) -> obj.villageRef)
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
          new KeyedCodec<>("ClaimedArea", AreaFootprint.CODEC),
          (obj, value) -> obj.claimedArea = value,
          (obj) -> obj.claimedArea)
      .add()

      .append(
          new KeyedCodec<>("VillageHeartBlockContext", BlockContext.CODEC),
          (obj, value) -> obj.blockContext = value,
          (obj) -> obj.blockContext)
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
              "HomelessRegistry",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false)),
          (obj, value) -> obj.homelessRegistry = value,
          obj -> obj.homelessRegistry)
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
              "HouseIdToHouse",
              new MapCodec<HouseComponent, HashMap<String, HouseComponent>>(
                  HouseComponent.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.houseIdToHouse = new HashMap<>();
            value.forEach((k, v) -> obj.houseIdToHouse.put(UUID.fromString(k), v));
          },
          obj -> {
            HashMap<String, HouseComponent> encoded = new HashMap<>();
            obj.houseIdToHouse.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .append(
          new KeyedCodec<>(
              "VillagerToHouseMap",
              new MapCodec<UUID, HashMap<String, UUID>>(
                  Codec.UUID_BINARY,
                  HashMap::new)),
          (obj, value) -> {
            obj.villagerToHouse = new HashMap<>();
            value.forEach((k, v) -> obj.villagerToHouse.put(UUID.fromString(k), v));
          },
          obj -> {
            HashMap<String, UUID> encoded = new HashMap<>();
            obj.villagerToHouse.forEach((k, v) -> encoded.put(k.toString(), v));
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

      .append(
          new KeyedCodec<>("TownHallId", Codec.UUID_BINARY),
          (obj, value) -> obj.townHallId = value,
          (obj) -> obj.townHallId)
      .add()

      .build();

  private UUID uuid;
  private String name;
  private EntityRef villageRef;
  private VillageHeartConfigComponent config;
  private VillageHeartState state = VillageHeartState.NORMAL;
  private int level = 1;
  private AreaFootprint claimedArea;
  private VolumeEntry volumeEntry;
  private BlockContext blockContext;

  private Set<UUID> villagersRegistry = new HashSet<>();
  private Set<UUID> unemployedRegistry = new HashSet<>();
  private Set<UUID> homelessRegistry = new HashSet<>();

  private Map<UUID, VillagerComponent> villagerIdToComponent = new HashMap<>();

  private Map<UUID, WorkplaceComponent> workplaceIdToWorkplace = new HashMap<>();
  private Map<UUID, UUID> villagerToWorkplace = new HashMap<>();

  private Map<UUID, HouseComponent> houseIdToHouse = new HashMap<>();
  private Map<UUID, UUID> villagerToHouse = new HashMap<>();

  private Set<UUID> villageStorageWorkplaces = new HashSet<>();
  private Set<UUID> villageTavernWorkplaces = new HashSet<>();
  private UUID townHallId;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public Ref<ChunkStore> getVillageRef() {
    return villageRef.getRef();
  }

  public EntityRef getEntityRef() {
    return this.villageRef;
  }

  public void setVillageRef(EntityRef ref) {
    this.villageRef = ref;
  }

  public WorkplaceComponent getTownHall() {
    if (this.townHallId == null) {
      return null;
    }
    WorkplaceComponent workplace = this.getWorkplaceFromId(this.townHallId);
    if (workplace == null) {
      throw new IllegalArgumentException("[getTownHallRef] TownHallId points to dead workplace component");
    }

    return workplace;
  }

  public UUID getTownHallId() {
    return this.townHallId;
  }

  public Ref<ChunkStore> getTownHallRef() {
    return this.getTownHall().getWorkplaceRef().getRef();
  }

  public VillageHeartConfigComponent getConfig() {
    return config.clone();
  }

  public Set<UUID> getVillagersRegistry() {
    return Collections.unmodifiableSet(this.villagersRegistry);
  }

  public Set<UUID> getHomelessRegistry() {
    return Collections.unmodifiableSet(this.homelessRegistry);
  }

  public Map<UUID, HouseComponent> getHouseIdToHouse() {
    return Collections.unmodifiableMap(this.houseIdToHouse);
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

  public void removeVillagerLinks(UUID villagerId) {
    if (villagerId == null) {
      return;
    }

    this.removeVillagerWorkplaceLink(villagerId);
    this.removeVillagerHousingLink(villagerId);
    this.unemployedRegistry.remove(villagerId);
    this.homelessRegistry.remove(villagerId);
    this.unregisterVillager(villagerId);
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

  public void registerAsHomeless(UUID uuid) {
    this.homelessRegistry.add(uuid);
    this.villagerToHouse.remove(uuid);
    VillagerComponent villagerComponent = this.villagerIdToComponent.get(uuid);
    if (villagerComponent != null) {
      villagerComponent.setHousingData(null);
    }
  }

  public boolean assignHouse(UUID villager, UUID house) {
    HouseComponent houseComponent = this.houseIdToHouse.get(house);
    assert houseComponent != null : "Assigned a house before registering the house";

    UUID bed = houseComponent.assignAppartment(villager);
    if (bed == null) {
      return false;
    }

    this.homelessRegistry.remove(villager);
    Vec3i bedPosition = houseComponent.getBedPosition(bed);
    VillagerComponent villagerComponent = this.villagerIdToComponent.get(villager);

    this.villagerToHouse.put(villager, house);
    villagerComponent.setHouse(houseComponent);
    villagerComponent.getHousingData().setBedPosition(bedPosition);

    return true;
  }

  public HouseComponent findHouseWithVacancy() {
    for (Map.Entry<UUID, HouseComponent> house : this.houseIdToHouse.entrySet()) {
      if (house.getValue().hasVacancy()) {
        return house.getValue();
      }
    }
    return null;
  }

  public UUID findHouseIdWithVacancy() {
    for (Map.Entry<UUID, HouseComponent> house : this.houseIdToHouse.entrySet()) {
      if (house.getValue().hasVacancy()) {
        return house.getKey();
      }
    }
    return null;
  }

  public void registerHouse(HouseComponent house) {
    this.houseIdToHouse.put(house.getUuid(), house);
  }

  public void unregisterHouse(UUID house) {
    HouseComponent houseComponent = this.houseIdToHouse.remove(house);
    if (houseComponent == null || houseComponent.getAppartmentRegistry() == null) {
      return;
    }

    houseComponent.getAppartmentRegistry().getAppartments().values().forEach(appartment -> {
      appartment.getBeds().values().forEach(bed -> {
        UUID owner = bed.getOwner();
        if (owner != null) {
          this.registerAsHomeless(owner);
        }
      });
    });
  }

  public HouseComponent getHouseFromId(UUID house) {
    return this.houseIdToHouse.get(house);
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

  public AreaFootprint getClaimedArea() {
    return claimedArea;
  }

  public void setClaimedArea(AreaFootprint claimedChunks) {
    this.claimedArea = claimedChunks;
    setTriggerVolumeArea(this.claimedArea);
  }

  public VolumeEntry getTriggerVolumeEntry() {
    return volumeEntry;
  }

  public void setTriggerVolumeEntry(VolumeEntry volumeEntry) {
    this.volumeEntry = volumeEntry;
  }

  private void setTriggerVolumeArea(AreaFootprint area) {
    if (volumeEntry == null) {
      // If the village is being spawned for the first time
      // Generate a trigger volume to register enter/exit village events
      volumeEntry = generateTriggerVolumeEntry(getBlockContext().getWorld(), area);

      // Add condition for cooldown to not always show the welcome message
      volumeEntry.getConditions().add(
          CooldownCondition.create(TriggerEventType.ENTER, config.getWelcomeMessageCooldown()));

      // Add effects
      Logger.logInfo("Registering village events for volume: " + name);
      List<TriggerEffect> triggerEffects = volumeEntry.getEffects();
      String welcomeMessage = "server.system.villageheartplacementsystem.welcomeToVillage";
      triggerEffects.add(ShowEventTitleEffect.create(TriggerEventType.ENTER, name, welcomeMessage, false));
      triggerEffects.add(PlaySoundEffect.create(TriggerEventType.ENTER, "SFX_AVATAR_POWERS_ENABLE"));

      // Add teleport village to core when exiting radius
      triggerEffects.add(TeleportVillagerToTownHeartEffect.create(TriggerEventType.EXIT, this.getEntityRef()));
    } else {
      // Trigger volume already exist, so change its shape according to the area
      BoxShape newShape = generateVolumeEntryShapeFromArea(area);
      volumeEntry.setShape(newShape);
      TriggerVolumeManager manager = Utils.getTriggerVolumeManager(getBlockContext().getWorld());
      manager.markSpatialDirty();
      manager.notifyViewersAdd(volumeEntry);
    }
  }

  private BoxShape generateVolumeEntryShapeFromArea(AreaFootprint area) {
    double halfX = (area.getMaxX() - area.getMinX()) / 2.0;
    double halfZ = (area.getMaxZ() - area.getMinZ()) / 2.0;
    BoxShape shape = new BoxShape(
        new Vec3d(-halfX, -config.getTriggerVolumeHeightBelowGround(), -halfZ), // Min bound
        new Vec3d(halfX, config.getTriggerVolumeHeightAboveGround(), halfZ)); // Max bound

    return shape;
  }

  private VolumeEntry generateTriggerVolumeEntry(World world, AreaFootprint villageArea) {
    BoxShape shape = generateVolumeEntryShapeFromArea(villageArea);

    // Generate position
    Vec3d position = villageArea.getCenter3D().to3d();
    position.y = getBlockContext().getPosition().y;

    Logger.logInfo("Generating TriggerVolume (Shape: %s, Position: %s)", shape, position);

    // Retrieve TriggerVolume manager
    TriggerVolumeManager manager = Utils.getTriggerVolumeManager(world);
    if (manager == null) {
      throw new IllegalStateException("Trigger volume manager not found");
    }

    // Create Volume
    String name = "village_" + getUuid().toString().replace("-", "_");
    VolumeEntry entry = new VolumeEntry(
        name, world.getName(), position, shape, new ArrayList<TriggerEffect>(),
        Set.of(EntityTargetType.PLAYER, EntityTargetType.NPC), true);

    // Register volume
    manager.register(name, entry);
    manager.notifyViewersAdd(entry);

    return entry;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public VillagerRole getNextRequiredRole() {
    if (this.villagerIdToComponent.size() == 0) {
      return VillagerRole.values()[0];
    }

    Map<VillagerRole, Integer> roleCounts = new EnumMap<>(VillagerRole.class);
    for (VillagerComponent villager : this.villagerIdToComponent.values()) {
      VillagerRole role = villager.getRole();
      if (role == null) {
        continue;
      }

      int count = roleCounts.getOrDefault(role, 0);
      roleCounts.put(role, count + 1);
    }

    int femaleCount = roleCounts.getOrDefault(VillagerRole.FEMALE, 0);
    int maleCount = roleCounts.getOrDefault(VillagerRole.MALE, 0);
    if (femaleCount < maleCount) {
      return VillagerRole.FEMALE;
    }
    return VillagerRole.MALE;
  }

  public void registerWorkplace(WorkplaceComponent workplace) {
    this.workplaceIdToWorkplace.put(workplace.getUuid(), workplace);

    StructureType workplaceType = workplace.getWorkplaceType();
    if (workplaceType.equals(StructureType.STORAGE)) {
      this.villageStorageWorkplaces.add(workplace.getUuid());
    } else if (workplaceType.equals(StructureType.TAVERN)) {
      this.villageTavernWorkplaces.add(workplace.getUuid());
    } else if (workplaceType.equals(StructureType.TOWN_HALL)) {
      this.townHallId = workplace.getUuid();
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
    } else if (workplace.equals(this.townHallId)) {
      this.townHallId = null;
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
    WorkplaceComponent workplaceComponent = this.workplaceIdToWorkplace.get(workplace);
    if (workplaceComponent == null || !workplaceComponent.needWorker()) {
      return;
    }

    this.unemployedRegistry.remove(villager);
    this.villagerToWorkplace.put(villager, workplace);
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

  private void removeVillagerWorkplaceLink(UUID villagerId) {
    UUID workplaceId = this.villagerToWorkplace.remove(villagerId);
    if (workplaceId == null) {
      return;
    }

    WorkplaceComponent workplace = this.workplaceIdToWorkplace.get(workplaceId);
    if (workplace == null) {
      return;
    }

    workplace.unregisterWorker(villagerId);
    if (villagerId.equals(workplace.getBossId())) {
      workplace.registerAsBoss(null);
    }
  }

  private void removeVillagerHousingLink(UUID villagerId) {
    UUID houseId = this.villagerToHouse.remove(villagerId);
    if (houseId == null) {
      return;
    }

    HouseComponent house = this.houseIdToHouse.get(houseId);
    if (house == null || house.getAppartmentRegistry() == null) {
      return;
    }

    house.getAppartmentRegistry().getAppartments().values().forEach(appartment -> {
      if (appartment == null) {
        return;
      }

      appartment.getBeds().values().forEach(bed -> {
        if (bed == null || !villagerId.equals(bed.getOwner())) {
          return;
        }

        bed.setOwner(null);
        bed.setTaken(false);
      });
    });
  }

  public List<WorkplaceComponent> getWorkplaceFrom(StructureType workplaceType) {
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

  public List<WorkplaceComponent> getWorkplaces() {
    return Collections.unmodifiableList(new ArrayList<>(this.workplaceIdToWorkplace.values()));
  }

  public List<HouseComponent> getHouses() {
    return Collections.unmodifiableList(new ArrayList<>(this.houseIdToHouse.values()));
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

  public short getStorageWorkplaceCount() {
    return (short) this.villageStorageWorkplaces.size();
  }

  public short getTavernWorkplaceCount() {
    return (short) this.villageTavernWorkplaces.size();
  }

  public short getHomelessCount() {
    return (short) this.homelessRegistry.size();
  }

  public short getUnemployedCount() {
    return (short) this.unemployedRegistry.size();
  }

  public short getHouseCount() {
    return (short) this.houseIdToHouse.size();
  }

  public short getAvailableBedsCount() {
    return (short) this.houseIdToHouse.values().stream()
        .mapToInt(HouseComponent::getFreeBedCount)
        .sum();
  }

  public short getTotalBedsCount() {
    return (short) this.houseIdToHouse.values().stream()
        .mapToInt(HouseComponent::getTotalBedsCount)
        .sum();
  }

  public BlockContext getBlockContext() {
    return blockContext;
  }

  public void setBlockContext(BlockContext blockContext) {
    this.blockContext = blockContext;
  }

  public UUID getFirstWorkplaceWithMissingEmployee() {
    for (Map.Entry<UUID, WorkplaceComponent> entry : this.workplaceIdToWorkplace.entrySet()) {
      if (entry.getValue().needWorker()) {
        return entry.getKey();
      }
    }
    return null;
  }

  public Map<StructureType, Short> getStructureTypeCount() {
    Map<StructureType, Short> structureCount = new HashMap<>();
    for (WorkplaceComponent workplace : this.workplaceIdToWorkplace.values()) {
      short count = structureCount.getOrDefault(workplace.getWorkplaceType(), (short) 0);
      structureCount.put(workplace.getWorkplaceType(), (short) (count + 1));
    }

    structureCount.put(StructureType.HOUSE, this.getHouseCount());
    return structureCount;
  }

  public List<WorkplaceComponent> getWorkplaceOfType(StructureType structureType) {
    return this.workplaceIdToWorkplace.values()
        .stream()
        .filter(entry -> entry.getWorkplaceType().equals(structureType))
        .toList();
  }

  @Override
  public VillageHeartComponent clone() {
    VillageHeartComponent component = new VillageHeartComponent();

    component.uuid = this.uuid;
    component.villageRef = this.villageRef == null ? null : this.villageRef.clone();

    component.state = this.state;
    component.level = this.level;

    component.config = this.config != null ? this.config.clone() : null;
    component.claimedArea = this.claimedArea != null ? this.claimedArea.clone() : null;
    component.blockContext = this.blockContext != null ? this.blockContext.clone() : null;
    component.townHallId = this.townHallId;

    component.villagersRegistry = new HashSet<>(this.villagersRegistry);
    component.unemployedRegistry = new HashSet<>(this.unemployedRegistry);
    component.homelessRegistry = new HashSet<>(this.homelessRegistry);
    component.villagerToWorkplace = new HashMap<>(this.villagerToWorkplace);
    component.villagerToHouse = new HashMap<>(this.villagerToHouse);
    component.villageStorageWorkplaces = new HashSet<>(this.villageStorageWorkplaces);
    component.villageTavernWorkplaces = new HashSet<>(this.villageTavernWorkplaces);

    component.workplaceIdToWorkplace = new HashMap<>();
    for (Map.Entry<UUID, WorkplaceComponent> entry : this.workplaceIdToWorkplace.entrySet()) {
      WorkplaceComponent clonedWorkplace = entry.getValue() != null ? entry.getValue().clone() : null;
      component.workplaceIdToWorkplace.put(entry.getKey(), clonedWorkplace);
    }

    component.houseIdToHouse = new HashMap<>();
    for (Map.Entry<UUID, HouseComponent> entry : this.houseIdToHouse.entrySet()) {
      HouseComponent clonedHouse = entry.getValue() != null ? entry.getValue().clone() : null;
      component.houseIdToHouse.put(entry.getKey(), clonedHouse);
    }

    component.villagerIdToComponent = new HashMap<>();
    for (Map.Entry<UUID, VillagerComponent> entry : this.villagerIdToComponent.entrySet()) {
      VillagerComponent clonedVillager = entry.getValue() != null ? entry.getValue().clone() : null;
      component.villagerIdToComponent.put(entry.getKey(), clonedVillager);
    }

    for (Map.Entry<UUID, UUID> entry : component.villagerToWorkplace.entrySet()) {
      UUID villagerId = entry.getKey();
      VillagerComponent villager = component.villagerIdToComponent.get(villagerId);
      WorkplaceComponent workplace = component.workplaceIdToWorkplace.get(entry.getValue());

      if (villager != null && villager.getWorkerData() != null) {
        villager.getWorkerData().setWorkplace(workplace);
      }
    }

    for (UUID unemployedVillagerId : component.unemployedRegistry) {
      VillagerComponent villager = component.villagerIdToComponent.get(unemployedVillagerId);
      if (villager != null) {
        villager.setWorkerData(null);
      }
    }

    for (Map.Entry<UUID, UUID> entry : component.villagerToHouse.entrySet()) {
      UUID villagerId = entry.getKey();
      VillagerComponent villager = component.villagerIdToComponent.get(villagerId);
      HouseComponent house = component.houseIdToHouse.get(entry.getValue());

      if (villager != null && villager.getHousingData() != null) {
        villager.getHousingData().setHouse(house);
      }
    }

    for (UUID homelessVillagerId : component.homelessRegistry) {
      VillagerComponent villager = component.villagerIdToComponent.get(homelessVillagerId);
      if (villager != null) {
        villager.setHousingData(null);
      }
    }

    return component;
  }

  @Override
  public String toString() {
    return "VillageHeartComponent{" +
        "\n uuid=" + Utils.safeToString(this.uuid) + "," +
        "\n villageRef=" + Utils.safeToString(this.villageRef) + "," +
        "\n config=" + Utils.safeToString(this.config) + "," +
        "\n state=" + Utils.safeToString(this.state) + "," +
        "\n level=" + this.level + "," +
        "\n claimedArea=" + Utils.safeToString(this.claimedArea) + "," +
        "\n villagersRegistry=" + Utils.safeToString(this.villagersRegistry) + "," +
        "\n unemployedRegistry=" + Utils.safeToString(this.unemployedRegistry) + "," +
        "\n homelessRegistry=" + Utils.safeToString(this.homelessRegistry) + "," +
        "\n villagerIdToComponent=" + Utils.safeToString(this.villagerIdToComponent) + "," +
        "\n workplaceIdToWorkplace=" + Utils.safeToString(this.workplaceIdToWorkplace) + "," +
        "\n villagerToWorkplace=" + Utils.safeToString(this.villagerToWorkplace) + "," +
        "\n houseIdToHouse=" + Utils.safeToString(this.houseIdToHouse) + "," +
        "\n villagerToHouse=" + Utils.safeToString(this.villagerToHouse) + "," +
        "\n villageStorageWorkplaces=" + Utils.safeToString(this.villageStorageWorkplaces) + "," +
        "\n villageTavernWorkplaces=" + Utils.safeToString(this.villageTavernWorkplaces) + "," +
        '}';
  }

  public static ComponentType<ChunkStore, VillageHeartComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageCoreComponentType();
  }

}
