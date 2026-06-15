package rocks.gravili.notquests.paper.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import rocks.gravili.notquests.paper.NotQuests;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateManager implements Listener {
    private final NotQuests main;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateManager(final NotQuests main) {
        this.main = main;
        this.latestVersion = main.getMain().getDescription().getVersion();

        Bukkit.getPluginManager().registerEvents(this, main.getMain());

        // Check on startup, then every 8 hours
        Bukkit.getScheduler().runTaskTimerAsynchronously(main.getMain(), this::checkForUpdates, 100L, 8 * 60 * 60 * 20L);
    }

    private void checkForUpdates() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.notquests.com/latest-version.txt"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String fetchedVersion = response.body().trim();
                if (isNewerVersion(fetchedVersion)) {
                    latestVersion = fetchedVersion;
                    updateAvailable = true;
                    main.getLogManager().info("<warn>A new version of NotQuests is available: <green>" + latestVersion
                            + "</green> (current: <red>" + main.getMain().getDescription().getVersion() + "</red>). "
                            + "Download: <highlight2>https://www.notquests.com/update</highlight2>");
                } else {
                    updateAvailable = false;
                }
            }
        } catch (Exception e) {
            main.getLogManager().warn("Unable to check for updates: " + e.getMessage());
        }
    }

    private boolean isNewerVersion(String remoteVersion) {
        try {
            String[] remoteParts = remoteVersion.split("\\.");
            String[] localParts = main.getMain().getDescription().getVersion().split("\\.");

            int remoteMajor = Integer.parseInt(remoteParts[0]);
            int remoteMinor = Integer.parseInt(remoteParts[1]);
            int remotePatch = Integer.parseInt(remoteParts[2]);

            int localMajor = Integer.parseInt(localParts[0]);
            int localMinor = Integer.parseInt(localParts[1]);
            int localPatch = Integer.parseInt(localParts[2]);

            if (remoteMajor != localMajor) return remoteMajor > localMajor;
            if (remoteMinor != localMinor) return remoteMinor > localMinor;
            return remotePatch > localPatch;
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!updateAvailable || !main.getConfiguration().isUpdateCheckerNotifyOpsInChat()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(main.getMain(), () -> {
            if (player.isOnline()) {
                player.sendMessage(main.parse(
                        "<hover:show_text:\"<highlight>Click to update!\"><click:open_url:\"https://www.notquests.com/update/\">"
                                + "<main>[NotQuests]</main> <warn>Your version <red>"
                                + main.getMain().getDescription().getVersion()
                                + "</red> is not the latest version (<green>"
                                + latestVersion
                                + "</green>). <bold>Click this message to update: <underlined>https://www.notquests.com/update/</underlined></bold></click></hover>"));
            }
        }, 60L);
    }

    public void checkForPluginUpdates(final CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
            checkForUpdates();
            if (updateAvailable) {
                sender.sendMessage(main.parse(
                        "<hover:show_text:\"<highlight>Click to update!\"><click:open_url:\"https://www.notquests.com/update/\">"
                                + "<main>[NotQuests]</main> <warn>Your version <red>"
                                + main.getMain().getDescription().getVersion()
                                + "</red> is not the latest version (<green>"
                                + latestVersion
                                + "</green>). <bold>Click this message to update: <underlined>https://www.notquests.com/update/</underlined></bold></click></hover>"));
            } else {
                sender.sendMessage(main.parse("<success>NotQuests is up to date! (version: <green>"
                        + main.getMain().getDescription().getVersion() + "</green>)"));
            }
        });
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
