package com.pacificbytestudios.projectciviccore.utils;

import org.joml.Vector3d;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;

/**
 * Spawns floating nameplate ("title") holograms as lightweight, intangible entities. Unlike
 * debug shapes these are real entities, so callers must keep the returned reference and pass
 * it to {@link #remove} when the hologram is no longer needed.
 */
public final class TitleHologramUtil {

  private TitleHologramUtil() {
  }

  public static Ref<EntityStore> spawn(World world, Vec3d position, String text) {
    Store<EntityStore> store = world.getEntityStore().getStore();

    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
    ProjectileComponent projectile = new ProjectileComponent("Projectile");
    holder.putComponent(ProjectileComponent.getComponentType(), projectile);
    holder.putComponent(
        TransformComponent.getComponentType(),
        new TransformComponent(new Vector3d(position), new Rotation3f()));
    holder.ensureComponent(UUIDComponent.getComponentType());

    if (projectile.getProjectile() == null) {
      projectile.initialize();
      if (projectile.getProjectile() == null) {
        return null;
      }
    }

    holder.addComponent(
        NetworkId.getComponentType(),
        new NetworkId(store.getExternalData().takeNextNetworkId()));
    holder.addComponent(Nameplate.getComponentType(), new Nameplate(text));

    return store.addEntity(holder, AddReason.SPAWN);
  }

  public static void remove(World world, Ref<EntityStore> ref) {
    if (ref != null && ref.isValid()) {
      world.getEntityStore().getStore().removeEntity(ref, RemoveReason.REMOVE);
    }
  }
}
