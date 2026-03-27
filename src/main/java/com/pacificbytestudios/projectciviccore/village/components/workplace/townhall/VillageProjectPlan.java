package com.pacificbytestudios.projectciviccore.village.components.workplace.townhall;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.Int2ObjectMapCodec;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveStatus;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class VillageProjectPlan {
  public static final BuilderCodec<VillageProjectPlan> CODEC = BuilderCodec
      .builder(VillageProjectPlan.class, VillageProjectPlan::new)
      .append(new KeyedCodec<>("ProjectList", new Int2ObjectMapCodec<>(
          new ArrayCodec<>(
              VillageProject.CODEC,
              VillageProject[]::new),
          Int2ObjectOpenHashMap::new), false),
          (obj, value) -> obj.projectList = decodeProjectQueue(value),
          obj -> encodeProjectQueue(obj.projectList))
      .add()
      .build();

  private PriorityQueue<VillageProject> projectList = new PriorityQueue<>();

  public PriorityQueue<VillageProject> getProjectList() {
    return this.projectList;
  }

  public void setProjectList(PriorityQueue<VillageProject> projectList) {
    this.projectList = new PriorityQueue<>();
    if (projectList == null) {
      return;
    }
    for (VillageProject project : projectList) {
      if (project != null) {
        this.projectList.add(project);
      }
    }
  }

  public void addProject(VillageProject project) {
    if (project == null) {
      return;
    }
    this.projectList.add(project);
  }

  public VillageProject peekNextProject() {
    return this.projectList.peek();
  }

  public VillageProject pollNextProject() {
    return this.projectList.poll();
  }

  public boolean hasProjects() {
    return !this.projectList.isEmpty();
  }

  public boolean contains(VillageObjectiveType objectiveType) {
    if (this.projectList.isEmpty()) {
      return false;
    }

    for (VillageProject project : this.projectList) {
      if (project != null && objectiveType.equals(project.getObjectiveType())) {
        return true;
      }
    }

    return false;
  }

  public boolean contains(VillageObjectiveStatus status) {
    if (this.projectList.isEmpty()) {
      return false;
    }

    for (VillageProject project : this.projectList) {
      if (project != null && status.equals(project.getObjectiveStatus())) {
        return true;
      }
    }

    return false;
  }

  public boolean contains(StructureType structureType) {
    if (this.projectList.isEmpty()) {
      return false;
    }

    for (VillageProject project : this.projectList) {
      if (project != null && structureType.equals(project.getStructureType())) {
        return true;
      }
    }

    return false;
  }

  public boolean contains(UUID structureId) {
    if (this.projectList.isEmpty()) {
      return false;
    }

    for (VillageProject project : this.projectList) {
      if (project != null && structureId.equals(project.getStructureId())) {
        return true;
      }
    }

    return false;
  }

  public boolean updateProjectPlan(VillageProject updatedProject) {
    if (updatedProject == null || updatedProject.getProjectId() == null || this.projectList.isEmpty()) {
      return false;
    }

    PriorityQueue<VillageProject> updatedQueue = new PriorityQueue<>();
    boolean replaced = false;

    for (VillageProject project : this.projectList) {
      if (project == null) {
        continue;
      }

      if (!replaced && updatedProject.getProjectId().equals(project.getProjectId())) {
        updatedQueue.add(updatedProject.clone());
        replaced = true;
        continue;
      }

      updatedQueue.add(project);
    }

    if (replaced) {
      this.projectList = updatedQueue;
    }

    return replaced;
  }

  public void removeProject(VillageProject projectToRemove) {
    this.projectList.removeIf(project -> project != null
        && projectToRemove.getProjectId().equals(project.getProjectId()));
  }

  public short getObjectiveTypeCount(VillageObjectiveType objectiveType) {
    return (short) this.projectList.stream()
        .map(VillageProject::getObjectiveType)
        .filter(objectiveType::equals)
        .count();
  }

  public VillageProjectPlan clone() {
    VillageProjectPlan component = new VillageProjectPlan();
    component.projectList = new PriorityQueue<>();

    for (VillageProject project : this.projectList) {
      if (project != null) {
        component.projectList.add(project.clone());
      }
    }

    return component;
  }

  private static PriorityQueue<VillageProject> decodeProjectQueue(Int2ObjectMap<VillageProject[]> encodedProjects) {
    PriorityQueue<VillageProject> decoded = new PriorityQueue<>();

    if (encodedProjects == null) {
      return decoded;
    }

    encodedProjects.forEach((weight, projectsAtWeight) -> {
      if (projectsAtWeight == null) {
        return;
      }

      for (VillageProject project : projectsAtWeight) {
        if (project == null) {
          continue;
        }
        project.setWeight(weight);
        decoded.add(project);
      }
    });

    return decoded;
  }

  private static Int2ObjectMap<VillageProject[]> encodeProjectQueue(PriorityQueue<VillageProject> projectQueue) {
    Int2ObjectOpenHashMap<List<VillageProject>> groupedProjects = new Int2ObjectOpenHashMap<>();

    if (projectQueue == null || projectQueue.isEmpty()) {
      return new Int2ObjectOpenHashMap<>();
    }

    PriorityQueue<VillageProject> orderedProjects = new PriorityQueue<>(projectQueue);
    while (!orderedProjects.isEmpty()) {
      VillageProject project = orderedProjects.poll();
      if (project == null) {
        continue;
      }
      groupedProjects.computeIfAbsent(project.getWeight(), key -> new ArrayList<>()).add(project);
    }

    Int2ObjectOpenHashMap<VillageProject[]> encoded = new Int2ObjectOpenHashMap<>();
    groupedProjects.int2ObjectEntrySet().forEach(entry -> {
      List<VillageProject> projectsAtWeight = entry.getValue();
      encoded.put(entry.getIntKey(), projectsAtWeight.toArray(VillageProject[]::new));
    });

    return encoded;
  }

  @Override
  public String toString() {
    return "VillageProjectPlan{projectList=" + projectList + "}";
  }

  public static class VillageProject implements Comparable<VillageProject> {
    public static final BuilderCodec<VillageProject> CODEC = BuilderCodec
        .builder(VillageProject.class, VillageProject::new)

        .append(new KeyedCodec<>("ProjectId", Codec.UUID_BINARY),
            (obj, value) -> obj.projectId = value,
            obj -> obj.projectId)
        .add()

        .append(new KeyedCodec<>(
            "ObjectiveType",
            new EnumCodec<>(VillageObjectiveType.class)),
            (obj, value) -> obj.objectiveType = value,
            obj -> obj.objectiveType)
        .add()

        .append(new KeyedCodec<>(
            "ObjectiveStatus",
            new EnumCodec<>(VillageObjectiveStatus.class),
            false),
            (obj, value) -> obj.objectiveStatus = value,
            obj -> obj.objectiveStatus)
        .add()

        .append(new KeyedCodec<>("Weight", new EnumCodec<>(Weight.class), false),
            (obj, value) -> obj.weight = value,
            obj -> obj.weight)
        .add()

        .append(new KeyedCodec<>("StructureType", new EnumCodec<>(StructureType.class), false),
            (obj, value) -> obj.structureType = value,
            obj -> obj.structureType)
        .add()

        .append(new KeyedCodec<>("StructureId", Codec.UUID_BINARY, false),
            (obj, value) -> obj.structureId = value,
            obj -> obj.structureId)
        .add()

        .append(new KeyedCodec<>("TargetLevel", Codec.INTEGER, false),
            (obj, value) -> obj.targetLevel = value,
            obj -> obj.targetLevel)
        .add()

        .append(new KeyedCodec<>("Stage", Codec.SHORT, false),
            (obj, value) -> obj.stage = value,
            obj -> obj.stage)
        .add()

        .append(new KeyedCodec<>("TotalStages", Codec.SHORT, false),
            (obj, value) -> obj.totalStages = value,
            obj -> obj.totalStages)
        .add()

        .append(new KeyedCodec<>("RequiredMaterials", new ArrayCodec<>(
            MaterialQuantity.CODEC,
            MaterialQuantity[]::new), false),
            (obj, value) -> obj.requiredMaterialPerStage = value,
            obj -> obj.requiredMaterialPerStage)
        .add()

        .build();

    private UUID projectId;
    private VillageObjectiveType objectiveType;
    private VillageObjectiveStatus objectiveStatus = VillageObjectiveStatus.RESOURCE_GATHERING;
    private Weight weight = Weight.LOW;
    private StructureType structureType;
    private UUID structureId;
    private int targetLevel = 1;
    private short stage = 0;
    private short totalStages = 1;
    private MaterialQuantity[] requiredMaterialPerStage = new MaterialQuantity[0];

    public UUID getProjectId() {
      return projectId;
    }

    public void setProjectId(UUID projectId) {
      this.projectId = projectId;
    }

    public VillageObjectiveType getObjectiveType() {
      return this.objectiveType;
    }

    public void setObjectiveType(VillageObjectiveType objectiveType) {
      this.objectiveType = objectiveType;
    }

    public VillageObjectiveStatus getObjectiveStatus() {
      return this.objectiveStatus;
    }

    public void setObjectiveStatus(VillageObjectiveStatus objectiveStatus) {
      this.objectiveStatus = objectiveStatus;
    }

    public int getWeight() {
      return this.weight.getValue();
    }

    public Weight getWeightType() {
      return this.weight;
    }

    public void setWeight(int weight) {
      this.weight = Weight.fromValue(weight);
    }

    public void setWeight(Weight weight) {
      this.weight = weight != null ? weight : Weight.LOW;
    }

    public short getStage() {
      return this.stage;
    }

    public void setStage(short stage) {
      this.stage = stage;
    }

    public short getTotalStages() {
      return this.totalStages;
    }

    public void setTotalStages(short totalStages) {
      this.totalStages = totalStages;
    }

    public MaterialQuantity[] getRequiredMaterialPerStage() {
      return this.requiredMaterialPerStage;
    }

    public void setRequiredMaterialPerStage(MaterialQuantity[] requiredMaterials) {
      this.requiredMaterialPerStage = requiredMaterials != null ? requiredMaterials : new MaterialQuantity[0];
    }

    public StructureType getStructureType() {
      return structureType;
    }

    public void setStructureType(StructureType structureType) {
      this.structureType = structureType;
    }

    public UUID getStructureId() {
      return structureId;
    }

    public void setStructureId(UUID structureId) {
      this.structureId = structureId;
    }

    public int getTargetLevel() {
      return targetLevel;
    }

    public void setTargetLevel(int targetLevel) {
      this.targetLevel = targetLevel;
    }

    public VillageProject clone() {
      VillageProject component = new VillageProject();
      component.projectId = this.projectId;
      component.objectiveType = this.objectiveType;
      component.objectiveStatus = this.objectiveStatus;
      component.structureType = this.structureType;
      component.structureId = this.structureId;
      component.weight = this.weight;
      component.stage = this.stage;
      component.totalStages = this.totalStages;
      component.targetLevel = this.targetLevel;

      component.requiredMaterialPerStage = new MaterialQuantity[this.requiredMaterialPerStage.length];
      for (int i = 0; i < this.requiredMaterialPerStage.length; i++) {
        MaterialQuantity material = this.requiredMaterialPerStage[i];
        component.requiredMaterialPerStage[i] = material != null
            ? material.clone(material.getQuantity())
            : null;
      }
      return component;
    }

    @Override
    public int compareTo(VillageProject other) {
      if (other == null) {
        return -1;
      }
      return Integer.compare(other.getWeight(), this.getWeight());
    }

    @Override
    public String toString() {
      return "VillageProject{" +
          "\n projectId=" + projectId +
          ",\n objectiveType=" + objectiveType +
          ",\n objectiveStatus=" + objectiveStatus +
          ",\n weight=" + weight +
          ",\n structureType=" + structureType +
          ",\n structureId=" + structureId +
          ",\n targetLevel=" + targetLevel +
          ",\n stage=" + stage +
          ",\n totalStages=" + totalStages +
          '}';
    }

    public enum Weight {
      HIGH(3),
      MID(2),
      LOW(1);

      private final int value;

      Weight(int value) {
        this.value = value;
      }

      public int getValue() {
        return this.value;
      }

      public static Weight fromValue(int value) {
        for (Weight weight : values()) {
          if (weight.value == value) {
            return weight;
          }
        }
        return LOW;
      }
    }

  }
}
