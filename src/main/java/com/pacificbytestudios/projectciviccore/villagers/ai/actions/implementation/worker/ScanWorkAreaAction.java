package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartDebugComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderScanWorkAreaAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.BlockTarget;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.IWorkAreaScanner;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.ScanFactory;

public class ScanWorkAreaAction extends VillagerActionBase {

  public ScanWorkAreaAction(BuilderScanWorkAreaAction builderActionBase) {
    super(builderActionBase);
  }

  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    UUID villagerId = this.getVillagerId(ref, store);
    VillagerComponent villager = this.getVillagerComponent(ref, store);
    VillageHeartComponent village = this.getVillageHeartComponent(ref, store);
    if (villager == null || village == null || villagerId == null) {
      return false;
    }

    IWorkAreaScanner scanner = ScanFactory.newScanner(villager);

    if (scanner == null) {
      return false;
    }

    WorkplaceAreaSnapshot snapshot = scanner.execute(
        store,
        ref,
        villagerId,
        villager);

    villager.getWorkerData().setWorkareaSnapshot(snapshot);

    if (village.getConfig().isDebugActive()) {
      VillageHeartDebugComponent debug = village.getComponent(VillageHeartDebugComponent.getComponentType());
      WorkplaceComponent workplace = villager.getWorkerData().getWorkplace();
      if (debug != null && debug.isDisplayWorkerSnapshots() && workplace != null) {
        bindSnapshotDebug(store.getExternalData().getWorld(), village, debug, workplace.getUuid(), villagerId, snapshot);
      }
    }

    return snapshot.isCompleted();
  }

  /**
   * Publishes the snapshot markers and keeps them in sync as the worker consumes targets:
   * the snapshot notifies this listener whenever targets are popped, so they disappear from
   * the debug view. The listener re-checks the debug flags so it stays cheap once disabled.
   */
  private void bindSnapshotDebug(
      World world,
      VillageHeartComponent village,
      VillageHeartDebugComponent debug,
      UUID structureId,
      UUID villagerId,
      WorkplaceAreaSnapshot snapshot) {
    Runnable refresh = () -> {
      if (!village.getConfig().isDebugActive() || !debug.isDisplayWorkerSnapshots()) {
        return;
      }
      debug.setSnapshotWaypoints(structureId, villagerId, buildSnapshotMarkers(snapshot));
      debug.drawAll(world);
    };

    snapshot.setChangeListener(refresh);
    refresh.run();
  }

  /**
   * Turns the scanned targets into snapshot waypoints: each scan group gets its own color
   * (cycled through the snapshot marker types), every waypoint carries its group name, and
   * targets sharing a block are stacked into a vertical column (hovering above the block) so
   * each stays visible.
   */
  private static Map<MarkerType, List<Waypoint>> buildSnapshotMarkers(WorkplaceAreaSnapshot snapshot) {
    Map<MarkerType, List<Waypoint>> markers = new HashMap<>();
    Map<Vec3i, Integer> columnByBlock = new HashMap<>();
    int groupIndex = 0;

    for (Map.Entry<String, ArrayDeque<BlockTarget>> group : snapshot.getSnapshotTargets().entrySet()) {
      MarkerType type = MarkerType.snapshotMarker(groupIndex++);

      for (BlockTarget target : group.getValue()) {
        Vec3i block = target == null ? null : target.getTargetBlock();
        if (block == null) {
          continue;
        }

        int column = columnByBlock.merge(block, 1, Integer::sum) - 1;

        Vec3f position = new Vec3f(
            block.x + 0.5f,
            block.y + BASE_HOVER + column * COLUMN_SPACING,
            block.z + 0.5f);

        Waypoint waypoint = Waypoint.from(type, new BlockTransform3D(position), false);
        waypoint.putMetadata("groupName", group.getKey());
        markers.computeIfAbsent(type, ignored -> new ArrayList<>()).add(waypoint);
      }
    }

    return markers;
  }

  // Lift markers a block above the target so they are not hidden inside it, then stack.
  private static final float BASE_HOVER = 1.5f;
  private static final float COLUMN_SPACING = 0.4f;
}
