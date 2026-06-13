package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bson.Document;
import org.bson.json.JsonParseException;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsConfigSaver;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsSessionUtil;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent.WaypointChange;

/**
 * Review page for an editing session: lists the pending waypoint changes and lets the
 * player save them to the Village_Core config, roll back everything, or roll back a
 * single waypoint. When a switch target is set (the player clicked another structure
 * while having unsaved changes), saving or rolling back also switches the session to
 * the new structure, while cancelling keeps the current one.
 */
public class WaypointSessionPage extends BasicCustomUIPage {
  private static final String PAGE_DOCUMENT = "Pages/Debug/WaypointSessionPage.ui";
  private static final int MAX_ROWS = 12;

  private static final String SAVE_ACTION = "save";
  private static final String SAVE_EXIT_ACTION = "saveExit";
  private static final String ROLLBACK_ALL_ACTION = "rollbackAll";
  private static final String ROLLBACK_ONE_ACTION = "rollbackOne";
  private static final String CANCEL_ACTION = "cancel";

  public record SwitchTarget(VillageHeartComponent village, TopologyStructure structure) {
  }

  private final World world;
  private final VillageHeartComponent village;
  private final TopologyStructure structure;
  private final WaypointsEditingComponent editingComponent;
  private final SwitchTarget switchTarget;
  private final List<UUID> rowWaypointIds = new ArrayList<>();

  public WaypointSessionPage(
      PlayerRef playerRef,
      World world,
      VillageHeartComponent village,
      TopologyStructure structure,
      WaypointsEditingComponent editingComponent,
      SwitchTarget switchTarget) {
    super(playerRef, CustomPageLifetime.CanDismiss);
    this.world = world;
    this.village = village;
    this.structure = structure;
    this.editingComponent = editingComponent;
    this.switchTarget = switchTarget;
  }

  @Override
  public void build(
      Ref<EntityStore> player,
      UICommandBuilder builder,
      UIEventBuilder eventBuilder,
      Store<EntityStore> store) {
    this.build(builder);
    this.bindEvents(eventBuilder);
  }

  @Override
  public void build(UICommandBuilder builder) {
    builder.append(PAGE_DOCUMENT);

    this.setText(builder, "StructureIdValue", String.valueOf(this.editingComponent.getStructureId()));
    builder.set("#SwitchNotice.Visible", this.switchTarget != null);

    List<WaypointChange> changes = this.editingComponent.getNetChanges();
    this.rowWaypointIds.clear();

    builder.set("#NoChangesLabel.Visible", changes.isEmpty());

    for (int i = 0; i < MAX_ROWS; i++) {
      boolean used = i < changes.size();
      builder.set("#ChangeRow" + i + ".Visible", used);
      if (!used) {
        continue;
      }

      WaypointChange change = changes.get(i);
      this.rowWaypointIds.add(change.waypointId());
      this.setText(builder, "ChangeLabel" + i, describe(change));
    }

    int extra = changes.size() - MAX_ROWS;
    builder.set("#MoreChangesLabel.Visible", extra > 0);
    if (extra > 0) {
      this.setText(builder, "MoreChangesLabel", "+" + extra + " more change(s)");
    }
  }

  private void bindEvents(UIEventBuilder eventBuilder) {
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveButton",
        EventData.of("action", SAVE_ACTION));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveExitButton",
        EventData.of("action", SAVE_EXIT_ACTION));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#RollbackAllButton",
        EventData.of("action", ROLLBACK_ALL_ACTION));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelButton",
        EventData.of("action", CANCEL_ACTION));

    for (int i = 0; i < MAX_ROWS; i++) {
      eventBuilder.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#UndoButton" + i,
          EventData.of("action", ROLLBACK_ONE_ACTION).append("index", String.valueOf(i)));
    }
  }

  @Override
  public void handleDataEvent(Ref<EntityStore> player, Store<EntityStore> store, String data) {
    Document event = parse(data);
    if (event == null) {
      return;
    }

    String action = event.getString("action");
    if (action == null) {
      this.sendUpdate();
      return;
    }

    switch (action) {
      case SAVE_ACTION -> handleSave(player, store);
      case SAVE_EXIT_ACTION -> handleSaveExit(player, store);
      case ROLLBACK_ALL_ACTION -> handleRollbackAll(player, store);
      case ROLLBACK_ONE_ACTION -> handleRollbackOne(player, store, event);
      default -> this.close();
    }
  }

  private void handleSave(Ref<EntityStore> player, Store<EntityStore> store) {
    WaypointsEditingComponent component = liveComponent(player, store);
    if (component == null) {
      this.close();
      return;
    }

    component.commitCurrentOperation();
    WaypointsConfigSaver.SaveResult result = WaypointsConfigSaver.save(
        this.world,
        this.village,
        this.structure,
        component);
    this.playerRef.sendMessage(Message.raw(result.message()));

    if (!result.success()) {
      // Keep the session (and its pending changes) so nothing gets lost.
      this.close();
      return;
    }

    component.clearOperations();
    this.finish(player, store);
  }

  /** Saves any pending changes and then ends the editing session entirely. */
  private void handleSaveExit(Ref<EntityStore> player, Store<EntityStore> store) {
    WaypointsEditingComponent component = liveComponent(player, store);
    if (component == null) {
      WaypointsSessionUtil.exitSession(this.world, store, player, null);
      this.close();
      return;
    }

    component.commitCurrentOperation();
    WaypointsConfigSaver.SaveResult result = WaypointsConfigSaver.save(
        this.world,
        this.village,
        this.structure,
        component);
    this.playerRef.sendMessage(Message.raw(result.message()));

    if (!result.success()) {
      // Keep the session (and its pending changes) so nothing gets lost.
      this.close();
      return;
    }

    component.clearOperations();
    WaypointsSessionUtil.exitSession(this.world, store, player, component);
    this.playerRef.sendMessage(Message.raw("Waypoint editing session ended"));
    this.close();
  }

  private void handleRollbackAll(Ref<EntityStore> player, Store<EntityStore> store) {
    WaypointsEditingComponent component = liveComponent(player, store);
    if (component == null) {
      this.close();
      return;
    }

    component.rollbackAll();
    WaypointsSessionUtil.redraw(this.world, component);
    this.playerRef.sendMessage(Message.raw("Waypoint changes rolled back"));
    this.finish(player, store);
  }

  private void handleRollbackOne(Ref<EntityStore> player, Store<EntityStore> store, Document event) {
    WaypointsEditingComponent component = liveComponent(player, store);
    if (component == null) {
      this.close();
      return;
    }

    int index = parseIndex(event.getString("index"));
    if (index < 0 || index >= this.rowWaypointIds.size()) {
      this.sendUpdate();
      return;
    }

    component.rollbackWaypoint(this.rowWaypointIds.get(index));
    WaypointsSessionUtil.redraw(this.world, component);
    this.rebuild();
  }

  /** Closes the page and, when a switch target is set, moves the session to it. */
  private void finish(Ref<EntityStore> player, Store<EntityStore> store) {
    if (this.switchTarget == null) {
      this.close();
      return;
    }

    WaypointsEditingComponent fresh = new WaypointsEditingComponent();
    fresh.setStructureId(this.switchTarget.structure().getStructureId());
    if (!WaypointsSessionUtil.populateWaypoints(
        fresh,
        this.switchTarget.village(),
        this.switchTarget.structure())) {
      this.playerRef.sendMessage(Message.raw("Could not start editing the clicked structure"));
      store.tryRemoveComponent(player, WaypointsEditingComponent.getComponentType());
      DrawUtils.clear(this.world);
      this.close();
      return;
    }

    store.putComponent(player, WaypointsEditingComponent.getComponentType(), fresh);
    WaypointsSessionUtil.redraw(this.world, fresh);
    this.playerRef.sendMessage(Message.raw(
        "Now editing " + this.switchTarget.structure().getStructureType()
            + " " + this.switchTarget.structure().getStructureId()));
    this.close();
  }

  private WaypointsEditingComponent liveComponent(Ref<EntityStore> player, Store<EntityStore> store) {
    return store.getComponent(player, WaypointsEditingComponent.getComponentType());
  }

  private static String describe(WaypointChange change) {
    return switch (change.operation()) {
      case ADD -> "[ADDED] " + change.markerType() + " at " + position(change.current());
      case DELETE -> "[DELETED] " + change.markerType() + " was " + position(change.original());
      case MOVE -> describeMove(change);
    };
  }

  private static String describeMove(WaypointChange change) {
    String from = position(change.original());
    String to = position(change.current());
    String rotationChange = rotationChange(change.original(), change.current());

    if (from.equals(to) && rotationChange != null) {
      return "[ROTATED] " + change.markerType() + " " + rotationChange;
    }

    String description = "[MOVED] " + change.markerType() + " " + from + " -> " + to;
    return rotationChange == null ? description : description + " (" + rotationChange + ")";
  }

  private static String rotationChange(Waypoint original, Waypoint current) {
    if (original == null || current == null) {
      return null;
    }

    var oldRotation = original.getTransform() == null ? null : original.getTransform().getRotation();
    var newRotation = current.getTransform() == null ? null : current.getTransform().getRotation();
    if (oldRotation == newRotation) {
      return null;
    }

    return (oldRotation == null ? "none" : oldRotation.name())
        + " -> " + (newRotation == null ? "none" : newRotation.name());
  }

  private static String position(Waypoint waypoint) {
    if (waypoint == null) {
      return "?";
    }

    Vec3d position = waypoint.getWaypointPosition();
    return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", position.x, position.y, position.z);
  }

  private static int parseIndex(String value) {
    if (value == null) {
      return -1;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      return -1;
    }
  }

  private static Document parse(String data) {
    if (data == null || data.isBlank()) {
      return null;
    }

    try {
      return Document.parse(data);
    } catch (JsonParseException exception) {
      return null;
    }
  }

  private void setText(UICommandBuilder builder, String id, String text) {
    builder.set("#" + id + ".TextSpans", Message.raw(text));
  }
}
