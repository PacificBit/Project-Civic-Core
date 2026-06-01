package com.pacificbytestudios.projectciviccore.triggervolumeeffects;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TeleportVillagerToTownHeartTask;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class TeleportVillagerToTownHeartEffect extends TriggerEffect {
  @Nonnull
  public static final BuilderCodec<TeleportVillagerToTownHeartEffect> CODEC = BuilderCodec
      .builder(TeleportVillagerToTownHeartEffect.class,
          TeleportVillagerToTownHeartEffect::new, BASE_CODEC)

      .append(new KeyedCodec<EntityRef>("VillageRef", EntityRef.CODEC),
          (entity, value) -> entity.villageRef = value,
          entity -> entity.villageRef)
      .add()

      .build();

  private EntityRef villageRef;

  public EntityRef getVillageRef() {
    return villageRef;
  }

  public void setVillageRef(EntityRef villageRef) {
    this.villageRef = villageRef;
  }

  @Nonnull
  public static TeleportVillagerToTownHeartEffect create(@Nonnull TriggerEventType eventType,
      @Nonnull EntityRef villageRef) {
    TeleportVillagerToTownHeartEffect effect = new TeleportVillagerToTownHeartEffect();
    effect.setEventType(eventType);
    effect.villageRef = villageRef;
    return effect;
  }

  @Override
  public void execute(@Nonnull TriggerContext context) {
    // Check if entity is a valid villager
    Store<EntityStore> entityStore = context.getStore();
    VillagerComponent villager = entityStore.getComponent(
        context.getEntityRef(), VillagerComponent.getComponentType());
    if (villager == null) {
      return;
    }

    // Try to get villager id
    UUIDComponent uuidComponent = entityStore.getComponent(context.getEntityRef(), UUIDComponent.getComponentType());
    if (uuidComponent == null) {
      return;
    }
    UUID targetVillagerId = uuidComponent.getUuid();

    // Add telport task to village queue
    VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();
    taskQueue.registerTask(new TeleportVillagerToTownHeartTask(targetVillagerId));
    Utils.safeVillageTaskEnqueue(villageRef.getRef(), taskQueue);
  }
}