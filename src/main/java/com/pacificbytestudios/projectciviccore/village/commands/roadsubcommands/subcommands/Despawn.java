// package
// com.pacificbytestudios.projectciviccore.village.commands.roadsubcommands.subcommands;
//
// import com.hypixel.hytale.component.Ref;
// import com.hypixel.hytale.math.util.ChunkUtil;
// import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
//
// import com.hypixel.hytale.server.core.Message;
// import com.hypixel.hytale.server.core.command.system.CommandContext;
// import
// com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
// import
// com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
// import com.hypixel.hytale.server.core.universe.world.World;
// import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
// import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
// import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
// import
// com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
// import com.pacificbytestudios.projectciviccore.utils.Utils;
// import
// com.pacificbytestudios.projectciviccore.village.core.spawner.RoadSpawner;
//
// import io.sentry.util.Pair;
//
// @Deprecated(forRemoval = true)
// public class Despawn extends CommandBase {
// private static final int TRACKED_BLOCK_SEARCH_RADIUS = 4;
// private static final int TRACKED_BLOCK_VERTICAL_SEARCH_RADIUS = 6;
//
// public Despawn() {
// super("despawn", "Despawn a road");
// }
//
// @Override
// protected void executeSync(CommandContext context) {
// Ref<EntityStore> playerRef = context.senderAsPlayerRef();
// var entityStore = playerRef.getStore();
// World world = entityStore.getExternalData().getWorld();
//
// world.execute(() -> {
// TransformComponent transform = entityStore.getComponent(
// playerRef,
// TransformComponent.getComponentType());
// if (transform == null) {
// context.sendMessage(Message.raw("Could not read player position."));
// return;
// }
//
// Vec3i playerPos = Vec3i.from(transform.getPosition());
// WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(playerPos.x,
// playerPos.z));
// Utils.BlockContext roadAnchor = resolveTrackedRoadAnchor(chunk, playerPos);
// if (roadAnchor == null) {
// context.sendMessage(Message.raw("Could not despawn road near player."));
// return;
// }
//
// boolean despawnResult = RoadSpawner.despawn(roadAnchor.chunk(),
// roadAnchor.position());
// if (!despawnResult) {
// context.sendMessage(Message.raw("Could not despawn road near player."));
// return;
// }
//
// context.sendMessage(Message.raw("Road successfully despawned."));
// });
// }
//
// private static Utils.BlockContext resolveTrackedRoadAnchor(WorldChunk chunk,
// Vec3i position) {
// Pair<WorldChunk, Ref<ChunkStore>> direct =
// Utils.getBlockComponentEntityChunkAware(chunk, position);
// if (hasTrackedRoadMetadata(direct)) {
// return new Utils.BlockContext(direct.getFirst(), Vec3i.from(position));
// }
//
// for (int verticalOffset : buildVerticalSearchOffsets()) {
// for (int radius = 0; radius <= TRACKED_BLOCK_SEARCH_RADIUS; radius++) {
// for (int x = -radius; x <= radius; x++) {
// for (int z = -radius; z <= radius; z++) {
// if (radius > 0 && Math.max(Math.abs(x), Math.abs(z)) != radius) {
// continue;
// }
//
// Vec3i candidatePosition = Vec3i.from(position).add(x,
// verticalOffset, z);
// Pair<WorldChunk, Ref<ChunkStore>> candidate =
// Utils.getBlockComponentEntityChunkAware(
// chunk,
// candidatePosition);
//
// if (hasTrackedRoadMetadata(candidate)) {
// return new Utils.BlockContext(candidate.getFirst(), candidatePosition);
// }
// }
// }
// }
// }
//
// return null;
// }
//
// private static int[] buildVerticalSearchOffsets() {
// int[] offsets = new int[TRACKED_BLOCK_VERTICAL_SEARCH_RADIUS * 2 + 1];
// offsets[0] = 0;
//
// int index = 1;
// for (int offset = 1; offset <= TRACKED_BLOCK_VERTICAL_SEARCH_RADIUS;
// offset++) {
// offsets[index++] = -offset;
// offsets[index++] = offset;
// }
//
// return offsets;
// }
//
// private static boolean hasTrackedRoadMetadata(Pair<WorldChunk,
// Ref<ChunkStore>> result) {
// Ref<ChunkStore> candidateRef = result.getSecond();
// if (candidateRef == null || !candidateRef.isValid()) {
// return false;
// }
//
// return candidateRef.getStore().getComponent(
// candidateRef,
// TrackedPrefabPlacementComponent.getComponentType()) != null;
// }
// }
