package com.pacificbytestudios.projectciviccore.village.commands.markercommands.subcommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.joml.Matrix4d;

import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.AppartmentComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.InteractablesType;

public class Draw extends VillageCommand {

  private ExecutionContext exeContext;
  private Map<MarkerType, List<Vec3d>> markers;

  public Draw() {
    super("draw", "server.system.markers.display.desc");
  }

  @Override
  public void execute(ExecutionContext exeContext) {
    VillageHeartComponent heart = this.getVillageHeart(exeContext);
    if (heart == null) {
      return;
    }

    this.exeContext = exeContext;
    initMarkerMap();

    heart.getWorkplaceIdToWorkplace()
        .values()
        .forEach(this::addWorkplaceMarkers);

    heart.getHouseIdToHouse()
        .values()
        .forEach(this::addHouseMarkers);

    drawMarkers();
  }

  private void initMarkerMap() {
    this.markers = new HashMap<>();
    Stream.of(MarkerType.values())
        .forEach(markerType -> this.markers.put(markerType, new ArrayList<>()));
  }

  private void addWorkplaceMarkers(WorkplaceComponent workplace) {
    this.markers.get(MarkerType.BENCH).add(workplace.getBenchPos().to3d());
    this.markers.get(MarkerType.BENCH_ACCESS).add(workplace.getBenchAccessPos().to3d());
    this.markers.get(MarkerType.ANCHOR).add(workplace.getPrefabAnchor().to3d());

    workplace.getStorage().getAllContainerPositions().forEach(containerPos -> {
      this.markers.get(MarkerType.STORAGE).add(containerPos.to3d());
    });

    workplace.getStorage().getAllContainerAccessPositions().forEach(containerAccPos -> {
      this.markers.get(MarkerType.STORAGE_ACCESS).add(containerAccPos.to3d());
    });

    WorkplaceInteractablesMapComponent interactables = workplace
        .getComponent(WorkplaceInteractablesMapComponent.getComponentType());

    if (interactables == null) {
      return;
    }

    addInteractables(interactables);
  }

  private void addInteractables(WorkplaceInteractablesMapComponent workplaceInteractables) {
    Map<InteractablesType, Interactable[]> interactableMap = workplaceInteractables.getAllInteractables();

    for (Entry<InteractablesType, Interactable[]> interactableEntry : interactableMap.entrySet()) {
      InteractablesType key = interactableEntry.getKey();

      for (Interactable interactable : interactableEntry.getValue()) {
        this.markers.get(key.getMarkerType()).add(interactable.getInteractablePosition().to3d());

        if (key.getDependsOn().equals(MarkerType.NONE)) {
          continue;
        }

        this.markers.get(key.getDependsOn()).add(interactable.getDependsOn().to3d());
      }
    }
  }

  private void addHouseMarkers(HouseComponent houseComponent) {
    if (houseComponent.getCoreBlockPosition() != null) {
      this.markers.get(MarkerType.BENCH).add(houseComponent.getCoreBlockPosition().to3d());
    }
    this.markers.get(MarkerType.ANCHOR).add(houseComponent.getPrefabAnchor().to3d());

    for (AppartmentComponent appartment : houseComponent.getAppartmentRegistry().getAppartments().values()) {
      for (VillageBedComponent bed : appartment.getBeds().values()) {
        this.markers.get(MarkerType.BED).add(bed.getPosition().to3d());
      }
    }
  }

  private void drawMarkers() {
    double scale = 0.2;

    for (Entry<MarkerType, List<Vec3d>> markerTypeEntry : this.markers.entrySet()) {
      MarkerType key = markerTypeEntry.getKey();

      for (Vec3d marker : markerTypeEntry.getValue()) {
        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(marker.x, marker.y, marker.z);
        matrix.scale(scale, scale, scale);

        DrawUtils.add(
            this.exeContext.world(),
            key.getShape(),
            matrix,
            key.getColor(),
            DrawUtils.FOREVER,
            DebugUtils.FLAG_NONE);
      }

    }
  }

}
