package com.pacificbytestudios.projectciviccore.village.commands.storagecommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.storagecommands.subcommands.ClearQueueRequests;
import com.pacificbytestudios.projectciviccore.village.commands.storagecommands.subcommands.ClearStorageRequest;
import com.pacificbytestudios.projectciviccore.village.commands.storagecommands.subcommands.ItemFetchRequest;
import com.pacificbytestudios.projectciviccore.village.commands.storagecommands.subcommands.StorageDump;

public class Storage extends AbstractCommandCollection {

  public Storage() {
    super("storage", "storage commands");
    this.setPermissionGroups("hytale:WorldEditor");
    this.addSubCommand(new ItemFetchRequest());
    this.addSubCommand(new ClearStorageRequest());
    this.addSubCommand(new ClearQueueRequests());
    this.addSubCommand(new StorageDump());
  }

}
