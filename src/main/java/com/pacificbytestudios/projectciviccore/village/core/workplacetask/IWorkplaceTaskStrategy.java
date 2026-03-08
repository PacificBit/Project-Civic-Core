package com.pacificbytestudios.projectciviccore.village.core.workplacetask;

import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;

public interface IWorkplaceTaskStrategy {

  public boolean execute(WorkplaceComponent workplace);

  public IWorkplaceTaskStrategy clone();

}
