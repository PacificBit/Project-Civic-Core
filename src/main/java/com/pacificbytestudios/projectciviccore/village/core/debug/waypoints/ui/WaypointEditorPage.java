package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;

import org.bson.Document;
import org.bson.json.JsonParseException;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsSessionUtil;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointOperation;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointOperation.Operation;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;

public class WaypointEditorPage extends BasicCustomUIPage {
  private static final String PAGE_DOCUMENT = "Pages/Debug/WaypointEditorPage.ui";

  private static final String DELETE_ACTION = "delete";
  private static final String MOVE_ACTION = "move";
  private static final String TP_ACTION = "tp";
  private static final String ADD_ACTION = "add";
  private static final String CHANGE_REFERENCE_ACTION = "changeReference";
  private static final String SET_POSITION_ACTION = "setPosition";
  private static final String ROTATE_ACTION = "rotate";

  private final World world;
  private final WaypointsEditingComponent editingComponent;
  private Waypoint selectedWaypoint;
  private final Vec3d hitLocation;

  public WaypointEditorPage(
      PlayerRef playerRef,
      World world,
      WaypointsEditingComponent editingComponent,
      Waypoint selectedWaypoint,
      Vec3d hitLocation) {
    super(playerRef, CustomPageLifetime.CanDismiss);
    this.world = world;
    this.editingComponent = editingComponent;
    this.selectedWaypoint = selectedWaypoint;
    this.hitLocation = hitLocation;
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

    boolean hasSelectedWaypoint = this.selectedWaypoint != null;
    builder.set("#SelectedPanel.Visible", hasSelectedWaypoint);
    builder.set("#AddPanel.Visible", !hasSelectedWaypoint);
    this.setText(builder, "StructureIdValue", this.editingComponent.getStructureId().toString());

    if (hasSelectedWaypoint) {
      this.buildSelectedWaypoint(builder);
      return;
    }

    this.buildAddWaypoint(builder);
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
      case DELETE_ACTION -> handleDeleteEvent(player, store);
      case MOVE_ACTION -> handleMoveEvent(player, store);
      case TP_ACTION -> handleTpEvent(player, store);
      case ADD_ACTION -> handleAddEvent(player, store, event);
      case CHANGE_REFERENCE_ACTION -> handleChangeReferenceEvent(player, store, event);
      case SET_POSITION_ACTION -> handleSetPositionEvent(player, store, event);
      case ROTATE_ACTION -> handleRotateEvent(player, store, event);
      default -> this.sendUpdate();
    }
  }

  private void handleRotateEvent(Ref<EntityStore> player, Store<EntityStore> store, Document event) {
    WaypointsEditingComponent editingComponent = liveComponent(player, store);
    if (editingComponent == null
        || this.selectedWaypoint == null
        || this.selectedWaypoint.isAccess()
        || this.selectedWaypoint.isFloatTransform()) {
      this.sendUpdate();
      return;
    }

    Rotation rotation = parseRotation(eventValue(event));
    if (rotation == null || rotation == this.selectedWaypoint.getTransform().getRotation()) {
      this.sendUpdate();
      return;
    }

    Waypoint old = this.selectedWaypoint.clone();
    Waypoint updated = this.selectedWaypoint.clone();
    updated.getTransform().setRotation(rotation);

    editingComponent.updateStructureWaypoint(updated.getType(), updated.getPositionId(), updated);
    editingComponent.addOperation(new WaypointOperation(
        old.getWaypointId(), Operation.MOVE, old, updated.clone()));
    // Re-stage the selection on the updated instance so a follow-up Move keeps working.
    editingComponent.setCurrentOperation(new WaypointOperation(null, updated, null));
    this.selectedWaypoint = updated;

    WaypointsSessionUtil.redraw(this.world, editingComponent);
    this.rebuild();
  }

  private void handleSetPositionEvent(Ref<EntityStore> player, Store<EntityStore> store, Document event) {
    WaypointsEditingComponent editingComponent = liveComponent(player, store);
    if (editingComponent == null || this.selectedWaypoint == null) {
      this.close();
      return;
    }

    Double x = numberValue(event, "@X");
    Double y = numberValue(event, "@Y");
    Double z = numberValue(event, "@Z");
    if (x == null || y == null || z == null) {
      this.sendUpdate();
      return;
    }

    Vec3d target = new Vec3d(x, y, z);
    Waypoint old = this.selectedWaypoint.clone();
    Waypoint updated = this.selectedWaypoint.clone();

    UUID changedWaypointId;
    if (updated.isAccess()) {
      updated.setAccessPosition(target);
      changedWaypointId = updated.getAccessId();
    } else {
      updated.setPosition(target);
      changedWaypointId = updated.getPositionId();
    }

    editingComponent.updateStructureWaypoint(updated.getType(), changedWaypointId, updated);
    editingComponent.addOperation(new WaypointOperation(
        old.getWaypointId(), Operation.MOVE, old, updated.clone()));
    // Re-stage the selection on the updated instance so a follow-up Move keeps working.
    editingComponent.setCurrentOperation(new WaypointOperation(null, updated, null));
    this.selectedWaypoint = updated;

    WaypointsSessionUtil.redraw(this.world, editingComponent);
    this.rebuild();
  }

  private void handleMoveEvent(Ref<EntityStore> player, Store<EntityStore> store) {
    WaypointsEditingComponent editingComponent = liveComponent(player, store);
    if (editingComponent == null
        || editingComponent.getCurrentOperation() == null
        || this.selectedWaypoint == null) {
      this.close();
      return;
    }

    WaypointOperation currentOperation = editingComponent.getCurrentOperation();
    currentOperation.setOperationType(Operation.MOVE);
    currentOperation.setWaypointId(this.selectedWaypoint.getWaypointId());
    this.playerRef.sendMessage(Message.raw(
        "Left click a block to move the " + this.selectedWaypoint.getType() + " waypoint"));
    this.close();
  }

  private void handleDeleteEvent(Ref<EntityStore> player, Store<EntityStore> store) {
    WaypointsEditingComponent editingComponent = liveComponent(player, store);
    if (editingComponent == null || this.selectedWaypoint == null) {
      this.close();
      return;
    }

    MarkerType type = this.selectedWaypoint.getType();
    if (!isDeletable(type)) {
      this.playerRef.sendMessage(Message.raw(type + " waypoints cannot be deleted"));
      this.sendUpdate();
      return;
    }

    // The selection staged in the current operation references this waypoint.
    editingComponent.setCurrentOperation(null);
    deleteWaypoint(editingComponent, this.selectedWaypoint);

    if (type == MarkerType.STORAGE) {
      Waypoint access = findAccessFor(editingComponent, MarkerType.STORAGE_ACCESS,
          this.selectedWaypoint.getPositionId());
      if (access != null) {
        deleteWaypoint(editingComponent, access);
      }
    }

    WaypointsSessionUtil.redraw(this.world, editingComponent);
    this.playerRef.sendMessage(Message.raw("Deleted " + type + " waypoint"));
    this.close();
  }

  private void handleTpEvent(Ref<EntityStore> player, Store<EntityStore> store) {
    if (this.selectedWaypoint == null) {
      this.close();
      return;
    }

    Vec3d target = this.selectedWaypoint.getWaypointPosition();
    if (!this.selectedWaypoint.isAccess()) {
      target = target.add(0.5, 1.0, 0.5);
    }

    TransformComponent transform = store.getComponent(player, TransformComponent.getComponentType());
    Rotation3f rotation = transform == null ? new Rotation3f() : transform.getRotation().clone();
    store.putComponent(player, Teleport.getComponentType(), Teleport.createForPlayer(target, rotation));
    this.close();
  }

  private void handleAddEvent(Ref<EntityStore> player, Store<EntityStore> store, Document event) {
    WaypointsEditingComponent editingComponent = liveComponent(player, store);
    if (editingComponent == null) {
      this.close();
      return;
    }

    MarkerType type = parseMarkerType(eventValue(event));
    if (type == null) {
      this.sendUpdate();
      return;
    }

    if (this.hitLocation == null) {
      this.playerRef.sendMessage(Message.raw("No target position for the new waypoint"));
      this.close();
      return;
    }

    Waypoint added = createWaypoint(editingComponent, type, this.hitLocation);
    if (added == null) {
      this.playerRef.sendMessage(Message.raw(
          type + " needs a " + accessParentType(type) + " waypoint to attach to; add one first"));
      this.sendUpdate();
      return;
    }

    editingComponent.addStructureWaypoint(added);
    editingComponent.addOperation(new WaypointOperation(
        added.getWaypointId(), Operation.ADD, null, added.clone()));

    WaypointsSessionUtil.redraw(this.world, editingComponent);
    this.playerRef.sendMessage(Message.raw("Added " + type + " waypoint"));
    this.close();
  }

  private void handleChangeReferenceEvent(Ref<EntityStore> player, Store<EntityStore> store, Document event) {
    WaypointsEditingComponent editingComponent = liveComponent(player, store);
    if (editingComponent == null || this.selectedWaypoint == null || !this.selectedWaypoint.isAccess()) {
      this.sendUpdate();
      return;
    }

    UUID newParentId = parseUuid(eventValue(event));
    if (newParentId == null || newParentId.equals(this.selectedWaypoint.getPositionId())) {
      this.sendUpdate();
      return;
    }

    Waypoint parent = findNonAccessByPositionId(editingComponent, newParentId);
    if (parent == null || parent.isFloatTransform()) {
      this.sendUpdate();
      return;
    }

    Waypoint old = this.selectedWaypoint.clone();

    @SuppressWarnings("unchecked")
    BlockTransform3D parentTransform = (BlockTransform3D) parent.getTransform();
    BlockTransform3D updatedTransform = BlockTransform3D.clone(parentTransform);
    updatedTransform.setAccessOffset(Vec3f.from(this.selectedWaypoint.getTransform().getAccessOffset()));
    updatedTransform.setAccessId(this.selectedWaypoint.getAccessId());

    Waypoint updated = this.selectedWaypoint.clone();
    updated.setTransform(updatedTransform);

    editingComponent.updateStructureWaypoint(updated.getType(), old.getAccessId(), updated);
    editingComponent.addOperation(new WaypointOperation(
        old.getWaypointId(), Operation.MOVE, old, updated.clone()));

    WaypointsSessionUtil.redraw(this.world, editingComponent);
    this.playerRef.sendMessage(Message.raw(
        "Reference of " + updated.getType() + " changed to " + shortId(newParentId.toString())));
    this.sendUpdate();
  }

  private WaypointsEditingComponent liveComponent(Ref<EntityStore> player, Store<EntityStore> store) {
    return store.getComponent(player, WaypointsEditingComponent.getComponentType());
  }

  private static void deleteWaypoint(WaypointsEditingComponent editingComponent, Waypoint waypoint) {
    editingComponent.removeStructureWaypoint(waypoint.getType(), waypoint.getWaypointId());
    editingComponent.addOperation(new WaypointOperation(
        waypoint.getWaypointId(), Operation.DELETE, waypoint.clone(), null));
  }

  @SuppressWarnings("unchecked")
  private static Waypoint createWaypoint(
      WaypointsEditingComponent editingComponent,
      MarkerType type,
      Vec3d at) {
    if (isAccessMarker(type)) {
      Waypoint parent = nearestOfType(editingComponent, accessParentType(type), at);
      if (parent == null || parent.isFloatTransform()) {
        return null;
      }

      BlockTransform3D transform = BlockTransform3D.clone((BlockTransform3D) parent.getTransform());
      transform.setAccessId(null);
      transform.ensureAccessId();
      transform.setAccessOffset(at.to3f());
      return Waypoint.from(type, transform, true);
    }

    if (isFloatMarker(type)) {
      return Waypoint.from(type, new BlockTransform3D(at.to3f()), false);
    }

    return Waypoint.from(type, new BlockTransform3D(at.to3i()), false);
  }

  private static Waypoint nearestOfType(
      WaypointsEditingComponent editingComponent,
      MarkerType type,
      Vec3d target) {
    Waypoint[] waypoints = editingComponent.getStructureWaypoints().get(type);
    if (waypoints == null) {
      return null;
    }

    Waypoint nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    for (Waypoint waypoint : waypoints) {
      if (waypoint == null) {
        continue;
      }

      double distance = waypoint.getWaypointPosition().distanceSquared(target);
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearest = waypoint;
      }
    }

    return nearest;
  }

  private static Waypoint findAccessFor(
      WaypointsEditingComponent editingComponent,
      MarkerType accessType,
      UUID parentPositionId) {
    Waypoint[] waypoints = editingComponent.getStructureWaypoints().get(accessType);
    if (waypoints == null || parentPositionId == null) {
      return null;
    }

    for (Waypoint waypoint : waypoints) {
      if (waypoint != null && parentPositionId.equals(waypoint.getPositionId())) {
        return waypoint;
      }
    }
    return null;
  }

  private static Waypoint findNonAccessByPositionId(
      WaypointsEditingComponent editingComponent,
      UUID positionId) {
    for (Entry<MarkerType, Waypoint[]> entry : editingComponent.getStructureWaypoints().entrySet()) {
      if (isAccessMarker(entry.getKey())) {
        continue;
      }

      for (Waypoint waypoint : entry.getValue()) {
        if (waypoint != null && positionId.equals(waypoint.getPositionId())) {
          return waypoint;
        }
      }
    }
    return null;
  }

  private static boolean isDeletable(MarkerType type) {
    return switch (type) {
      case STORAGE, STORAGE_ACCESS, TAVERN_CHAIR, TAVERN_PLATE -> true;
      default -> false;
    };
  }

  private static boolean isFloatMarker(MarkerType type) {
    return switch (type) {
      case TAVERN_CHAIR, TAVERN_PLATE -> true;
      default -> false;
    };
  }

  private static MarkerType accessParentType(MarkerType accessType) {
    return accessType == MarkerType.BENCH_ACCESS ? MarkerType.BENCH : MarkerType.STORAGE;
  }

  private static Rotation parseRotation(String value) {
    if (value == null) {
      return null;
    }

    try {
      return Rotation.valueOf(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private static MarkerType parseMarkerType(String value) {
    if (value == null) {
      return null;
    }

    try {
      return MarkerType.valueOf(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private static UUID parseUuid(String value) {
    if (value == null) {
      return null;
    }

    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private static Double numberValue(Document event, String key) {
    Object value = event.get(key);
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return Double.parseDouble(text);
      } catch (NumberFormatException exception) {
        return null;
      }
    }
    return null;
  }

  private static String eventValue(Document event) {
    for (String key : new String[] { "@ParamValue", "ParamValue", "Value", "@Value" }) {
      Object value = event.get(key);
      if (value instanceof String text && !text.isBlank()) {
        return text;
      }
    }

    for (Entry<String, Object> entry : event.entrySet()) {
      if (!"action".equals(entry.getKey())
          && entry.getValue() instanceof String text
          && !text.isBlank()) {
        return text;
      }
    }
    return null;
  }

  private void bindEvents(UIEventBuilder eventBuilder) {
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#DeleteButton",
        EventData.of("action", DELETE_ACTION));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#MoveButton",
        EventData.of("action", MOVE_ACTION));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#TpButton",
        EventData.of("action", TP_ACTION));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ApplyPositionButton",
        EventData.of("action", SET_POSITION_ACTION)
            .append("@X", "#PosXInput.Value")
            .append("@Y", "#PosYInput.Value")
            .append("@Z", "#PosZInput.Value"));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#AddButton",
        EventData.of("action", ADD_ACTION)
            .append("@ParamValue", "#MarkerTypeDropdown.Value"));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#ReferenceDropdown",
        EventData.of("action", CHANGE_REFERENCE_ACTION)
            .append("@ParamValue", "#ReferenceDropdown.Value"));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#RotationDropdown",
        EventData.of("action", ROTATE_ACTION)
            .append("@ParamValue", "#RotationDropdown.Value"));
  }

  private void buildSelectedWaypoint(UICommandBuilder builder) {
    BlockTransform3D transform = this.selectedWaypoint.transform();

    this.setText(builder, "SelectedIdValue", this.selectedWaypoint.waypointId().toString());
    this.setText(builder, "SelectedTypeValue", this.selectedWaypoint.type().name());
    this.setText(builder, "SelectedPositionValue", formatVector(transform.getPosition()));

    boolean isAccess = this.selectedWaypoint.isAccess();
    builder.set("#AccessDetails.Visible", isAccess);
    builder.set("#TransformDetails.Visible", !isAccess);
    builder.set("#ReferenceGroup.Visible", isAccess);

    Vec3d editablePosition = this.selectedWaypoint.getWaypointPosition();
    builder.set("#PosXInput.Value", editablePosition.x);
    builder.set("#PosYInput.Value", editablePosition.y);
    builder.set("#PosZInput.Value", editablePosition.z);

    if (isAccess) {
      this.setText(builder, "AccessPositionValue", formatVector(transform.getAccessOffset()));
      builder.set("#ReferenceDropdown.Entries", this.referenceEntries());
      builder.set("#ReferenceDropdown.Value",
          transform.getPositionId() == null ? "" : transform.getPositionId().toString());
      return;
    }

    this.setText(builder, "RotationValue", transform.getRotation() == null ? "none" : transform.getRotation().name());

    boolean isBlock = !this.selectedWaypoint.isFloatTransform();
    builder.set("#RotationDropdown.Visible", isBlock);
    if (isBlock) {
      builder.set("#RotationDropdown.Entries", rotationEntries());
      builder.set("#RotationDropdown.Value",
          transform.getRotation() == null ? Rotation.None.name() : transform.getRotation().name());
    }
  }

  private static List<DropdownEntryInfo> rotationEntries() {
    List<DropdownEntryInfo> entries = new ArrayList<>();
    for (Rotation rotation : Rotation.values()) {
      entries.add(option(rotation.name(), rotation.name()));
    }
    return entries;
  }

  private void buildAddWaypoint(UICommandBuilder builder) {
    List<DropdownEntryInfo> markerEntries = this.markerTypeEntries();
    builder.set("#MarkerTypeDropdown.Entries", markerEntries);
    builder.set("#MarkerTypeDropdown.Value", firstMarkerTypeValue());
  }

  private List<DropdownEntryInfo> markerTypeEntries() {
    List<DropdownEntryInfo> entries = new ArrayList<>();
    for (MarkerType markerType : MarkerType.values()) {
      if (markerType == MarkerType.NONE) {
        continue;
      }

      entries.add(option(markerType.name(), markerType.name()));
    }

    return entries;
  }

  private List<DropdownEntryInfo> referenceEntries() {
    List<DropdownEntryInfo> entries = new ArrayList<>();
    for (Entry<MarkerType, Waypoint[]> entry : this.editingComponent
        .getStructureWaypoints().entrySet()) {
      if (isAccessMarker(entry.getKey())) {
        continue;
      }

      for (Waypoint waypoint : entry.getValue()) {
        if (waypoint.getPositionId() == null) {
          continue;
        }
        entries.add(option(referenceLabel(entry.getKey(), waypoint), waypoint.getPositionId().toString()));
      }
    }

    return entries;
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

  private static DropdownEntryInfo option(String label, String value) {
    return new DropdownEntryInfo(LocalizableString.fromString(label), value);
  }

  private static String firstMarkerTypeValue() {
    for (MarkerType markerType : MarkerType.values()) {
      if (markerType != MarkerType.NONE) {
        return markerType.name();
      }
    }

    return "";
  }

  private static String referenceLabel(MarkerType markerType, Waypoint waypoint) {
    BlockTransform3D transform = waypoint.getTransform();
    return markerType.name() + " " + shortId(waypoint.getPositionId().toString()) + " "
        + formatVector(transform.getPosition());
  }

  private static String shortId(String id) {
    return id.substring(0, 8);
  }

  private static boolean isAccessMarker(MarkerType markerType) {
    return switch (markerType) {
      case BENCH_ACCESS, STORAGE_ACCESS -> true;
      default -> false;
    };
  }

  private static String formatVector(Object vector) {
    if (vector instanceof Vec3i vec3i) {
      return vec3i.x + ", " + vec3i.y + ", " + vec3i.z;
    }
    if (vector instanceof Vec3f vec3f) {
      return formatVector(vec3f.x, vec3f.y, vec3f.z);
    }

    return vector.toString();
  }

  private static String formatVector(float x, float y, float z) {
    return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", x, y, z);
  }

  private void setText(UICommandBuilder builder, String id, String text) {
    builder.set("#" + id + ".TextSpans", Message.raw(text));
  }

}
