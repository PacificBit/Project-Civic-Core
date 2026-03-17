package com.pacificbytestudios.projectciviccore.village.core.interfaces;

import java.util.List;
import java.util.Set;

import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;

public interface IWorkplaceMissingResource {

  public void newMissingResouceBuffer();

  public Set<ItemRequestCriterion> getMissingResources();

  public void setMissingResource(List<ItemRequestCriterion> criterion);

  public void addMissingResource(ItemRequestCriterion criterion);

  public void resetMissingResources();

}
