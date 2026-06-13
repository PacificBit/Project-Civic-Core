package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.ui;

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
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsSessionUtil;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;

/**
 * Lightweight confirmation shown when ending an editing session that has no pending
 * changes: the player can exit the session or keep editing.
 */
public class WaypointExitConfirmPage extends BasicCustomUIPage {
  private static final String PAGE_DOCUMENT = "Pages/Debug/WaypointExitConfirmPage.ui";

  private static final String EXIT_ACTION = "exit";
  private static final String CANCEL_ACTION = "cancel";

  private final World world;

  public WaypointExitConfirmPage(PlayerRef playerRef, World world) {
    super(playerRef, CustomPageLifetime.CanDismiss);
    this.world = world;
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
  }

  private void bindEvents(UIEventBuilder eventBuilder) {
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ExitButton",
        EventData.of("action", EXIT_ACTION));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelButton",
        EventData.of("action", CANCEL_ACTION));
  }

  @Override
  public void handleDataEvent(Ref<EntityStore> player, Store<EntityStore> store, String data) {
    Document event = parse(data);
    String action = event == null ? null : event.getString("action");

    if (EXIT_ACTION.equals(action)) {
      WaypointsEditingComponent component = store.getComponent(
          player, WaypointsEditingComponent.getComponentType());
      WaypointsSessionUtil.exitSession(this.world, store, player, component);
      this.playerRef.sendMessage(Message.raw("Waypoint editing session ended"));
    }

    this.close();
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
}
