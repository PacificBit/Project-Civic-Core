package com.pacificbytestudios.projectciviccore.village.components.workplace.townhall;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.Int2ObjectMapCodec;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveStatus;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import lombok.Getter;
import lombok.Setter;

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

  @Getter
  private PriorityQueue<VillageProject> projectList = new PriorityQueue<>();

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

  @SerializableClass
  public static class VillageProject implements Comparable<VillageProject> {
    public static final BuilderCodec<VillageProject> CODEC = VillageProjectCodec.CODEC;

    @Getter
    @Setter
    UUID projectId;
    @Getter
    @Setter
    VillageObjectiveType objectiveType;
    @Getter
    @Setter
    VillageObjectiveStatus objectiveStatus = VillageObjectiveStatus.RESOURCE_GATHERING;
    @Getter
    @SerialName("Weight")
    Weight weightType = Weight.LOW;
    @Getter
    @Setter
    StructureType structureType;
    @Getter
    @Setter
    UUID structureId;
    @Getter
    @Setter
    int targetLevel = 1;
    @Getter
    @Setter
    short stage = 0;
    @Getter
    @Setter
    short totalStages = 1;
    @Getter
    @SerialName("RequiredMaterials")
    MaterialQuantity[] requiredMaterialPerStage = new MaterialQuantity[0];
    @Getter
    @Setter
    ConstructionMode constructionMode;

    public int getWeight() {
      return this.weightType.getValue();
    }

    public void setWeight(int weight) {
      this.weightType = Weight.fromValue(weight);
    }

    public void setWeight(Weight weight) {
      this.weightType = weight != null ? weight : Weight.LOW;
    }

    public void setRequiredMaterialPerStage(MaterialQuantity[] requiredMaterials) {
      this.requiredMaterialPerStage = requiredMaterials != null ? requiredMaterials : new MaterialQuantity[0];
    }

    public VillageProject clone() {
      return VillageProjectCodec.clone(this);
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
          ",\n weight=" + weightType +
          ",\n structureType=" + structureType +
          ",\n structureId=" + structureId +
          ",\n targetLevel=" + targetLevel +
          ",\n stage=" + stage +
          ",\n totalStages=" + totalStages +
          '}';
    }

    public enum Weight {
      TOP(4),
      HIGH(3),
      MID(2),
      LOW(1);

      @Getter
      private final int value;

      Weight(int value) {
        this.value = value;
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
