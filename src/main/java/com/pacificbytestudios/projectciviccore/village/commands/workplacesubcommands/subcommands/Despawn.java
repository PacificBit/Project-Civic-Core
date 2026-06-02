// package
// com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands;
//
// import java.util.List;
//
// import javax.annotation.Nonnull;
//
// import com.hypixel.hytale.component.Ref;
// import com.hypixel.hytale.component.Store;
// import com.hypixel.hytale.math.util.ChunkUtil;
// import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
// import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
//
// import com.hypixel.hytale.server.core.Message;
// import com.hypixel.hytale.server.core.command.system.CommandContext;
// import
// com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
// import
// com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
// import
// com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
// import
// com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
// import com.hypixel.hytale.server.core.universe.world.World;
// import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
// import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
// import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
// import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
// import com.pacificbytestudios.projectciviccore.utils.DespawnFunction;
// import com.pacificbytestudios.projectciviccore.utils.Utils;
// import
// com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
// import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
// import
// com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
//
// @Deprecated(forRemoval = true)
// public class Despawn extends CommandBase {
// private static final VillageHeartRegistry villageStore =
// VillageHeartRegistry.get();
//
// @Nonnull
// private final RequiredArg<String> workplaceType = this.withRequiredArg(
// "type",
// "Type of workplace to despawn",
// ArgTypes.STRING);
//
// public Despawn() {
// super("despawn", "Despawn workplace");
// }
//
// @Override
// protected void executeSync(CommandContext context) {
// Ref<EntityStore> playerRef = context.senderAsPlayerRef();
// Store<EntityStore> entityStore = playerRef.getStore();
// World world = entityStore.getExternalData().getWorld();
//
// world.execute(() -> {
// TransformComponent transform = entityStore.getComponent(
// playerRef,
// TransformComponent.getComponentType());
//
// if (transform == null) {
// context.sendMessage(Message.raw("Could not read player position."));
// return;
// }
//
// Vec3i playerPos = Vec3i.from(transform.getPosition());
// EntityRef heartRef = villageStore.getVillageHeartIn(new Vec2i(playerPos.x,
// playerPos.z));
//
// if (heartRef == null) {
// context.sendMessage(Message.raw("No village found in range."));
// return;
// }
//
// Ref<ChunkStore> heart = heartRef.getRef();
// if (heart == null || !heart.isValid()) {
// context.sendMessage(Message.raw("No valid village found in range."));
// return;
// }
//
// VillageHeartComponent villageHeartComponent = heart.getStore()
// .getComponent(heart, VillageHeartComponent.getComponentType());
// if (villageHeartComponent == null) {
// context.sendMessage(Message.raw("Could not load village data."));
// return;
// }
//
// StructureType workplace =
// StructureType.valueOf(this.workplaceType.get(context).toUpperCase());
//
// List<WorkplaceComponent> workplaceComponents =
// villageHeartComponent.getWorkplaceFrom(workplace);
//
// if (workplaceComponents.size() == 0) {
// return;
// }
//
// WorkplaceComponent toRemove = workplaceComponents.get(0);
// double shortestDistance = toRemove.getBenchPos().distanceSquared(playerPos);
//
// for (int i = 1; i < workplaceComponents.size(); i++) {
// double distance =
// workplaceComponents.get(i).getBenchPos().distanceSquared(playerPos);
// if (distance < shortestDistance) {
// toRemove = workplaceComponents.get(i);
// shortestDistance = distance;
// }
// }
//
// Vec3i benchPosition = toRemove.getBenchPos();
//
// WorldChunk chunk =
// world.getChunk(ChunkUtil.indexChunkFromBlock(benchPosition.x,
// benchPosition.z));
//
// DespawnFunction despawnFunction = Utils.getDespawnFunction(workplace);
// boolean despawnResult = despawnFunction.apply(chunk, benchPosition);
//
// if (!despawnResult) {
// context.sendMessage(Message.raw("Could not despawn workplace"));
// return;
// }
// context.sendMessage(Message.raw("Workplace successfully despawned"));
// });
// }
//
// }
