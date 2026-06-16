package com.notquests.paper.managers.integrations;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.notquests.paper.NotQuests;

import java.util.UUID;

public class LuckpermsManager {
  private final NotQuests main;
  private final LuckPerms luckPerms;

  public LuckpermsManager(final NotQuests main) {
    this.main = main;
    RegisteredServiceProvider<LuckPerms> provider =
        Bukkit.getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
      luckPerms = provider.getProvider();
    } else {
      luckPerms = null;
    }
  }

  public void givePermission(final UUID uuid, final String permissionNode) {
    if (!permissionNode.isBlank()) {
      luckPerms
          .getUserManager()
          .modifyUser(
              uuid,
              user -> {
                // Add the permission
                user.data().add(Node.builder(permissionNode).value(true).build());
              });
    }
  }

  public void denyPermission(final UUID uuid, final String permissionNode) {
    if (!permissionNode.isBlank()) {
      luckPerms
          .getUserManager()
          .modifyUser(
              uuid,
              user -> {
                // Add the permission
                user.data().add(Node.builder(permissionNode).value(false).build());
              });
    }
  }

  public void unsetPermission(final UUID uuid, final String permissionNode) {
    if (!permissionNode.isBlank()) {
      luckPerms
          .getUserManager()
          .modifyUser(
              uuid,
              user -> {
                // Add the permission
                user.data().remove(Node.builder(permissionNode).build());
              });
    }
  }
}
