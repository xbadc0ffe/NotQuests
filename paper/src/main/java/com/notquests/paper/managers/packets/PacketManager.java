package com.notquests.paper.managers.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;
import com.notquests.paper.NotQuests;
import com.notquests.paper.managers.packets.ownpacketstuff.modern.PacketInjector;
import com.notquests.paper.managers.packets.ownpacketstuff.reflection.ReflectionPacketInjector;
import com.notquests.paper.managers.packets.packetevents.PacketEventsPacketListener;

public class PacketManager implements Listener {
  private final NotQuests main;

  private final boolean usePacketEvents;
  private ReflectionPacketInjector injector;
  private PacketInjector modernInjector;

  private boolean modern = false;

  public PacketManager(final NotQuests main) {
    this.main = main;
    usePacketEvents = main.getConfiguration().usePacketEvents;
    modern = Bukkit.getVersion().contains("26.");
  }

  public final ReflectionPacketInjector getPacketInjector() {
    return injector;
  }


  public final @Nullable PacketInjector getModernPacketInjector() {
    return modernInjector;
  }

  public final boolean isModern() {
    return modern;
  }

  public void initialize() {
    if (main.getConfiguration().packetMagic) {
      if (usePacketEvents) {
        PacketEvents.getAPI()
            .getEventManager()
            .registerListener(new PacketEventsPacketListener(main), PacketListenerPriority.LOW);

        PacketEventsSettings settings = PacketEvents.getAPI().getSettings();
        settings.bStats(false).checkForUpdates(false).debug(false);

        PacketEvents.getAPI().init();
      } else {
        if (modern) {
          main.getLogManager().info("Initializing modern packet injector...");
          this.modernInjector = new PacketInjector(main);
          Bukkit.getServer().getPluginManager().registerEvents(this, main.getMain());

          // For Serverutils reload
          for (final Player player : Bukkit.getOnlinePlayers()) {
            main.getLogManager()
                .debug("Added SU player for modern packet injector. Name: " + player.getName());
            modernInjector.addPlayer(player);
          }
        } else {
          main.getLogManager().info("Initializing reflection packet injector...");

          this.injector = new ReflectionPacketInjector(main);
          Bukkit.getServer().getPluginManager().registerEvents(this, main.getMain());

          // For Serverutils reload
          for (final Player player : Bukkit.getOnlinePlayers()) {
            main.getLogManager()
                .debug("Added SU player for packet injector. Name: " + player.getName());
            injector.addPlayer(player);
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onJoin(PlayerJoinEvent e) {
    if (!usePacketEvents && main.getConfiguration().packetMagic) {
      Player player = e.getPlayer();
      main.getLogManager().debug("Added player for packet injector. Name: " + player.getName());

      if (modern) {
        modernInjector.addPlayer(player);

      } else {
        injector.addPlayer(player);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onQuit(PlayerQuitEvent e) {
    if (!usePacketEvents && main.getConfiguration().packetMagic) {
      Player player = e.getPlayer();
      main.getLogManager().debug("Removed player for packet injector. Name: " + player.getName());

      if (modern) {
        modernInjector.removePlayer(player);

      } else {
        injector.removePlayer(player);
      }
    }
  }

  public void onLoad() {
    if (usePacketEvents && main.getConfiguration().packetMagic) {
      PacketEvents.setAPI(SpigotPacketEventsBuilder.build(main.getMain()));
      PacketEvents.getAPI().load();
    }
  }

  public void terminate() {
    if (usePacketEvents && main.getConfiguration().packetMagic) {
      PacketEvents.getAPI().terminate();
    } else {
      for (final Player player : Bukkit.getOnlinePlayers()) {
        main.getLogManager().debug("Removed player for packet injector. Name: " + player.getName());
        if (modern) {
          if (modernInjector != null) {
            modernInjector.removePlayer(player);
            modernInjector.setPacketStuffEnabled(false);
          }
        } else {
          if (injector != null) {
            injector.removePlayer(player);
            injector.setPacketStuffEnabled(false);
          }
        }
      }
    }
  }

  public void sendBeaconUpdatePacket(Player player, Location location, BlockState blockState) {
    getModernPacketInjector().sendBeaconUpdatePacket(player, location, blockState);
  }
}
