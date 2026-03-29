package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

public class ScanFactory {

  public static IWorkAreaScanner newScanner(VillagerComponent villager) {
    if (villager.getWorkerData() == null) {
      return null;
    }
    WorkType typeOfWorker = villager.getWorkerData().getTypeOfWorker();
    return switch (typeOfWorker) {
      case MAYOR -> new MayorScan();
      case FARMER -> new FarmerScan();
      case MINER -> new MinerScan();
      case INN_KEEPER -> new InnKeeperScan();
      case WOOD_CUTTER -> new WoodCutterScan();
      case STORAGE_KEEPER -> new StorageKeeperScan();
      default -> throw new IllegalArgumentException("Unsupported workplace type: " + typeOfWorker);
    };
  }

}
