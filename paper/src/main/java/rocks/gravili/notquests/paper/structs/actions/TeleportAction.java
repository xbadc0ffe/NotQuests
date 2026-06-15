package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

public class TeleportAction extends Action {
    private Location teleportLocation;
    private float yaw = Float.NaN;
    private float pitch = Float.NaN;

    public TeleportAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder builder,
            final ActionFor actionFor) {
        final NQFlag yawFlag = NQFlag.builder("yaw", NQDescription.of("Optional yaw rotation after teleporting."))
                .withArgument(NQArguments.doubleArgument())
                .build();
        final NQFlag pitchFlag = NQFlag.builder("pitch", NQDescription.of("Optional pitch rotation after teleporting."))
                .withArgument(NQArguments.doubleArgument())
                .build();

        manager.command(builder
                .required("world", NQArguments.worldArgument(), NQDescription.of("World where the target player should be teleported."))
                .required("x", NQArguments.doubleArgument(), NQDescription.of("X coordinate where the target player should be teleported."))
                .required("y", NQArguments.doubleArgument(), NQDescription.of("Y coordinate where the target player should be teleported."))
                .required("z", NQArguments.doubleArgument(), NQDescription.of("Z coordinate where the target player should be teleported."))
                .flag(yawFlag)
                .flag(pitchFlag)
                .handler(context -> {
                    final World world = context.get("world");
                    final Location location = new Location(world, context.get("x"), context.get("y"), context.get("z"));
                    final TeleportAction action = new TeleportAction(main);
                    action.setTeleportLocation(location);
                    if (context.flags().contains(yawFlag)) {
                        action.setYaw(context.flags().<Double>getValue("yaw", 0d).floatValue());
                    }
                    if (context.flags().contains(pitchFlag)) {
                        action.setPitch(context.flags().<Double>getValue("pitch", 0d).floatValue());
                    }
                    main.getActionManager().addAction(action, context, actionFor);
                }));
    }

    public Location getTeleportLocation() {
        return teleportLocation;
    }

    public void setTeleportLocation(final Location teleportLocation) {
        this.teleportLocation = teleportLocation;
    }

    public void setYaw(final float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(final float pitch) {
        this.pitch = pitch;
    }

    @Override
    protected void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null || getTeleportLocation() == null || getTeleportLocation().getWorld() == null) {
            return;
        }

        final Location location = getTeleportLocation().clone();
        if (!Float.isNaN(yaw)) {
            location.setYaw(yaw);
        }
        if (!Float.isNaN(pitch)) {
            location.setPitch(pitch);
        }
        questPlayer.getPlayer().teleport(location);
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.location", getTeleportLocation());
        if (!Float.isNaN(yaw)) {
            configuration.set(initialPath + ".specifics.yaw", (double) yaw);
        }
        if (!Float.isNaN(pitch)) {
            configuration.set(initialPath + ".specifics.pitch", (double) pitch);
        }
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        teleportLocation = configuration.getLocation(initialPath + ".specifics.location", null);
        yaw = configuration.contains(initialPath + ".specifics.yaw")
                ? (float) configuration.getDouble(initialPath + ".specifics.yaw")
                : Float.NaN;
        pitch = configuration.contains(initialPath + ".specifics.pitch")
                ? (float) configuration.getDouble(initialPath + ".specifics.pitch")
                : Float.NaN;
    }

    @Override
    public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
        if (arguments.size() < 4) {
            return;
        }
        final World world = Bukkit.getWorld(arguments.get(0));
        if (world != null) {
            teleportLocation = new Vector(
                    Double.parseDouble(arguments.get(1)),
                    Double.parseDouble(arguments.get(2)),
                    Double.parseDouble(arguments.get(3))).toLocation(world);
        }
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Teleports player to: " + (getTeleportLocation() == null ? "unknown location" : getTeleportLocation().toVector());
    }
}
