// package
// com.pacificbytestudios.projectciviccore.village.commands.startercommands.subcommands;
//
// import java.util.ArrayList;
// import java.util.EnumMap;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
//
// import com.hypixel.hytale.component.Ref;
// import com.hypixel.hytale.component.RemoveReason;
// import com.hypixel.hytale.component.Store;
// import com.hypixel.hytale.math.util.ChunkUtil;
// import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
// import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
//
// import com.hypixel.hytale.server.core.Message;
// import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
// import com.hypixel.hytale.server.core.command.system.CommandContext;
// import
// com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
// import com.hypixel.hytale.server.core.universe.world.World;
// import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
// import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
// import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
// import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
// import
// com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
// import com.pacificbytestudios.projectciviccore.utils.DespawnFunction;
// import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
// import com.pacificbytestudios.projectciviccore.utils.Utils;
// import
// com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
// import
// com.pacificbytestudios.projectciviccore.village.core.spawner.HouseSpawner;
// import
// com.pacificbytestudios.projectciviccore.village.core.spawner.TownHeartSpawner;
// import
// com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnHouseTask;
// import
// com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnWorkplaceTask;
// import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
// import
// com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
// import
// com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
//
// @Deprecated(forRemoval = true)
// final class StarterVillageSupport {
// private static final VillageHeartRegistry villageStore =
// VillageHeartRegistry.get();
//
// private static final Map<StructureType, Vec3i> STARTER_WORKPLACE_OFFSETS =
// new EnumMap<>(StructureType.class);
// private static final Vec3i[] STARTER_HOUSE_OFFSETS = new Vec3i[] {
// new Vec3i(-12, 0, 12),
// new Vec3i(12, 0, 12)
// };
//
// static {
// STARTER_WORKPLACE_OFFSETS.put(StructureType.STORAGE, new Vec3i(12, 0, 0));
// STARTER_WORKPLACE_OFFSETS.put(StructureType.FARM, new Vec3i(-12, 0, 0));
// STARTER_WORKPLACE_OFFSETS.put(StructureType.TAVERN, new Vec3i(0, 0, -12));
// STARTER_WORKPLACE_OFFSETS.put(StructureType.TOWN_HALL, new Vec3i(0, 0,
// 12));
// }
//
// private StarterVillageSupport() {
// }
//
// static Vec3i resolveVillageAnchor(World world, Vec3i origin) {
// WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(origin.x,
// origin.z));
// Vec3i villageAnchor = Vec3i.from(origin);
//
// for (int y = origin.y; y > 0; y--) {
// if (chunk.getBlockType(villageAnchor).getId().equals("Empty")) {
// villageAnchor = new Vec3i(origin.x, y + 2, origin.z);
// }
// }
//
// return villageAnchor;
// }
//
// static boolean spawnStarterVillage(CommandContext context, World world,
// Vec3i villageAnchor) {
// if (villageStore.getVillageHeartIn(new Vec2i(villageAnchor.x,
// villageAnchor.z)) != null) {
// context.sendMessage(Message.raw("A village already exists at this
// position."));
// return false;
// }
//
// WorldChunk chunk =
// world.getChunk(ChunkUtil.indexChunkFromBlock(villageAnchor.x,
// villageAnchor.z));
//
// // if (!TownHeartSpawner.spawn(chunk, villageAnchor)) {
// // context.sendMessage(Message.raw("Could not spawn starter village."));
// // return false;
// // }
//
// Ref<ChunkStore> heartRef = chunk.getBlockComponentEntity(
// villageAnchor.x,
// villageAnchor.y + 1,
// villageAnchor.z);
//
// if (heartRef == null || !heartRef.isValid()) {
// TownHeartSpawner.despawn(chunk, villageAnchor);
// context.sendMessage(Message.raw("Starter village spawned without a valid
// heart reference."));
// return false;
// }
//
// VillageHeartComponent villageHeart = heartRef.getStore()
// .getComponent(heartRef, VillageHeartComponent.getComponentType());
// if (villageHeart == null) {
// TownHeartSpawner.despawn(chunk, villageAnchor);
// context.sendMessage(Message.raw("Could not initialize starter village heart
// data."));
// return false;
// }
//
// VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();
//
// // for (Map.Entry<StructureType, Vec3i> entry :
// // STARTER_WORKPLACE_OFFSETS.entrySet()) {
// // taskQueue.registerTask(new SpawnWorkplaceTask(
// // entry.getKey(),
// // Vec3i.from(villageAnchor).add(entry.getValue())));
// // }
// //
// // for (Vec3i houseOffset : STARTER_HOUSE_OFFSETS) {
// // taskQueue.registerTask(new
// // SpawnHouseTask(Vec3i.from(villageAnchor).add(houseOffset)));
// // }
//
// Utils.safeVillageTaskEnqueue(heartRef, taskQueue);
// return true;
// }
//
// static VillageContext findVillageFromPlayer(
// CommandContext context,
// Ref<EntityStore> playerRef,
// Store<EntityStore> entityStore) {
// TransformComponent transform = entityStore.getComponent(
// playerRef,
// TransformComponent.getComponentType());
//
// if (transform == null) {
// context.sendMessage(Message.raw("Could not read player position."));
// return null;
// }
//
// Vec3i playerPos = Vec3i.from(transform.getPosition());
// EntityRef heartWrapper = villageStore.getVillageHeartIn(new
// Vec2i(playerPos.x, playerPos.z));
//
// if (heartWrapper == null) {
// context.sendMessage(Message.raw("No village found in range."));
// return null;
// }
//
// Ref<ChunkStore> heartRef = heartWrapper.getRef();
// if (heartRef == null || !heartRef.isValid()) {
// context.sendMessage(Message.raw("No valid village found in range."));
// return null;
// }
//
// VillageHeartComponent villageHeart = heartRef.getStore()
// .getComponent(heartRef, VillageHeartComponent.getComponentType());
// if (villageHeart == null) {
// context.sendMessage(Message.raw("Could not load village data."));
// return null;
// }
//
// return new VillageContext(heartWrapper, heartRef, villageHeart);
// }
//
// static boolean despawnVillage(
// CommandContext context,
// Store<EntityStore> entityStore,
// World world,
// VillageContext villageContext) {
// removeVillagersAndBeds(entityStore, world, villageContext.villageHeart());
//
// if (!despawnHouses(world, villageContext.villageHeart())) {
// context.sendMessage(Message.raw("Could not despawn village houses."));
// return false;
// }
//
// if (!despawnWorkplaces(world, villageContext.villageHeart())) {
// context.sendMessage(Message.raw("Could not despawn village workplaces."));
// return false;
// }
//
// TrackedPrefabPlacementComponent trackedPlacement = villageContext.heartRef()
// .getStore()
// .getComponent(villageContext.heartRef(),
// TrackedPrefabPlacementComponent.getComponentType());
//
// Vec3i heartPosition = villageContext.heartWrapper().getPosition();
// WorldChunk chunk =
// world.getChunk(ChunkUtil.indexChunkFromBlock(heartPosition.x,
// heartPosition.z));
//
// chunk.breakBlock(heartPosition.x, heartPosition.y, heartPosition.z);
//
// Rotation heartRotation = trackedPlacement != null ? trackedPlacement.getYaw()
// : Rotation.None;
// boolean despawnResult = trackedPlacement != null &&
// trackedPlacement.hasRestoreSnapshot()
// ? PrefabManager.restoreTrackedPrefab(world, trackedPlacement)
// : TownHeartSpawner.despawn(chunk,
// Vec3i.from(heartPosition).add(Vec3i.safeDown()), heartRotation);
//
// if (!despawnResult) {
// context.sendMessage(Message.raw("Could not despawn village heart."));
// return false;
// }
//
// return true;
// }
//
// private static void removeVillagersAndBeds(
// Store<EntityStore> entityStore,
// World world,
// VillageHeartComponent villageHeart) {
// for (UUID villagerId : villageHeart.getVillagersRegistry()) {
// VillagerComponent villager = villageHeart.getVillager(villagerId);
// if (villager != null) {
// Vec3i bed = villager.getBedPosition();
// if (bed != null) {
// world.getChunk(ChunkUtil.indexChunkFromBlock(bed.x, bed.z))
// .breakBlock(bed.x, bed.y, bed.z);
// }
// }
//
// Ref<EntityStore> npc = world.getEntityStore().getRefFromUUID(villagerId);
// if (npc == null || !npc.isValid()) {
// continue;
// }
// entityStore.removeEntity(npc, RemoveReason.REMOVE);
// }
// }
//
// private static boolean despawnHouses(World world, VillageHeartComponent
// villageHeart) {
// List<HouseComponent> houses = new
// ArrayList<>(villageHeart.getHouseIdToHouse().values());
//
// for (HouseComponent house : houses) {
// breakHouseBeds(world, house);
//
// Vec3i coreBlockPosition = house.getCoreBlockPosition();
// if (coreBlockPosition == null) {
// continue;
// }
//
// WorldChunk chunk =
// world.getChunk(ChunkUtil.indexChunkFromBlock(coreBlockPosition.x,
// coreBlockPosition.z));
// if (!HouseSpawner.despawn(chunk, coreBlockPosition)) {
// return false;
// }
// }
//
// return true;
// }
//
// private static void breakHouseBeds(World world, HouseComponent house) {
// if (house.getAppartmentRegistry() == null) {
// return;
// }
//
// house.getAppartmentRegistry().getAppartments().values().forEach(appartment ->
// {
// if (appartment == null) {
// return;
// }
//
// appartment.getBeds().values().forEach(bed -> {
// if (bed == null || bed.getPosition() == null) {
// return;
// }
//
// Vec3i position = bed.getPosition();
// world.getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z))
// .breakBlock(position.x, position.y, position.z);
// });
// });
// }
//
// private static boolean despawnWorkplaces(World world, VillageHeartComponent
// villageHeart) {
// List<WorkplaceComponent> workplaces = new
// ArrayList<>(villageHeart.getWorkplaceIdToWorkplace().values());
//
// for (WorkplaceComponent workplace : workplaces) {
// Vec3i benchPosition = workplace.getBenchPos();
// if (benchPosition == null) {
// continue;
// }
//
// DespawnFunction despawnFunction =
// Utils.getDespawnFunction(workplace.getWorkplaceType());
// WorldChunk chunk =
// world.getChunk(ChunkUtil.indexChunkFromBlock(benchPosition.x,
// benchPosition.z));
//
// if (!despawnFunction.apply(chunk, benchPosition)) {
// return false;
// }
// }
//
// return true;
// }
//
// record VillageContext(
// EntityRef heartWrapper,
// Ref<ChunkStore> heartRef,
// VillageHeartComponent villageHeart) {
// }
// }
