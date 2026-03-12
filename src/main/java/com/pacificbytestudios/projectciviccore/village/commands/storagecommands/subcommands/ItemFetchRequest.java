package com.pacificbytestudios.projectciviccore.village.commands.storagecommands.subcommands;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class ItemFetchRequest extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<String> workplaceType = this.withRequiredArg(
      "type",
      "Type of workplace to despawn",
      ArgTypes.STRING);

  @Nonnull
  private final RequiredArg<String> item = this.withRequiredArg(
      "item",
      "Item name",
      ArgTypes.STRING);

  @Nonnull
  private final RequiredArg<Integer> count = this.withRequiredArg(
      "quantity",
      "How many to item of that type to fetch",
      ArgTypes.INTEGER);

  public ItemFetchRequest() {
    super("fetch", "Item fetch request from village storage");
  }

  @Override
  protected void executeSync(CommandContext context) {
    Ref<EntityStore> playerRef = context.senderAsPlayerRef();
    Store<EntityStore> entityStore = playerRef.getStore();
    World world = entityStore.getExternalData().getWorld();

    world.execute(() -> {
      TransformComponent transform = entityStore.getComponent(
          playerRef,
          TransformComponent.getComponentType());

      if (transform == null) {
        context.sendMessage(Message.raw("Could not read player position."));
        return;
      }

      Vector3i playerPos = transform.getPosition().toVector3i();
      EntityRef heartRef = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));

      if (heartRef == null) {
        context.sendMessage(Message.raw("No village found in range."));
        return;
      }

      Ref<ChunkStore> heart = heartRef.getRef();
      if (heart == null || !heart.isValid()) {
        context.sendMessage(Message.raw("No valid village found in range."));
        return;
      }

      VillageHeartComponent villageHeartComponent = heart.getStore()
          .getComponent(heart, VillageHeartComponent.getComponentType());
      if (villageHeartComponent == null) {
        context.sendMessage(Message.raw("Could not load village data."));
        return;
      }

      WorkplaceType workplace = WorkplaceType.valueOf(this.workplaceType.get(context).toUpperCase());

      List<WorkplaceComponent> workplaceComponents = villageHeartComponent.getWorkplaceFrom(workplace);

      if (workplaceComponents.size() == 0) {
        context.sendMessage(
            Message.raw("No workplace of type '" + this.workplaceType.get(context).toUpperCase() + "' found"));
        return;
      }

      WorkplaceComponent closestWorkplace = workplaceComponents.get(0);
      double shortestDistance = closestWorkplace.getBenchPos().distanceSquaredTo(playerPos);

      for (int i = 1; i < workplaceComponents.size(); i++) {
        double distance = workplaceComponents.get(i).getBenchPos().distanceSquaredTo(playerPos);
        if (distance < shortestDistance) {
          closestWorkplace = workplaceComponents.get(i);
          shortestDistance = distance;
        }
      }

      VillageStorageRequest.ItemRequestCriterion criteria = new VillageStorageRequest.ItemRequestCriterion();
      criteria.setAttributeKey(this.item.get(context));
      criteria.setExactMatch(true);
      criteria.setRequiredAmount(this.count.get(context));

      VillageStorageRequest request = new VillageStorageRequest();
      request.setType(VillageStorageRequestType.DELIVER_TO_WORKPLACE);
      request.setRequesterWorkplaceId(closestWorkplace.getUuid());
      request.addItemCriteria(criteria);
      request.setTargetsWorkplaceStorage(closestWorkplace.getStorage().getAllContainerPositions());

      EnqueueVillageStorageRequestTask task = new EnqueueVillageStorageRequestTask(
          request);

      task.setOnError(
          (reason, msg) -> context.sendMessage(
              Message.raw("Could not proceed with the request. Reason: \n" + reason + "\n\t" + msg)));

      task.setOnSuccessCallback(
          (status) -> context.sendMessage(
              Message.raw("Request registered successfully")));

      VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
      taskComponent.registerTask(task);
      Utils.safeVillageTaskEnqueue(heart, taskComponent);
    });
  }

}
