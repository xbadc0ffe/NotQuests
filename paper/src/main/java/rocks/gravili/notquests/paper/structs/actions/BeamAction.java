package rocks.gravili.notquests.paper.structs.actions;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BeamAction extends Action {

    private String beamName = "";
    private boolean remove = false;
    private Location beamLocation = null;

    public BeamAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder builder,
            ActionFor actionFor) {
        manager.command(builder.required("beamName", NQArguments.stringArgument(), NQDescription.of("Identifier of the beam to remove."), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("<Enter beam name>");
                    return completions;
                })
                .literal("remove", NQDescription.of("Removes the named beam from the player's screen."))
                .handler((context) -> {
                    String beamName = context.get("beamName");
                    BeamAction beamAction = new BeamAction(main);
                    beamAction.setBeamName(beamName);
                    beamAction.setRemove(true);
                    main.getActionManager().addAction(beamAction, context, actionFor);
                }));

        manager.command(builder.required("beamName", NQArguments.stringArgument(), NQDescription.of("Identifier to assign to the spawned beam."), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("<Enter beam name>");
                    return completions;
                })
                .literal("spawn", NQDescription.of("Spawns or displays the configured object or effect."))
                .required("world", NQArguments.worldArgument(), NQDescription.of("World where the beam should be spawned."))
                .required("x", NQArguments.integerArgument(), NQDescription.of("X coordinate where the beam should be spawned."))
                .required("y", NQArguments.integerArgument(), NQDescription.of("Y coordinate where the beam should be spawned."))
                .required("z", NQArguments.integerArgument(), NQDescription.of("Z coordinate where the beam should be spawned."))
                .handler(
                        (context) -> {
                            String beamName = context.get("beamName");
                            final World world = context.get("world");
                            final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                            final Location location = coordinates.toLocation(world);

                            BeamAction beamAction = new BeamAction(main);
                            beamAction.setBeamName(beamName);
                            beamAction.setRemove(false);
                            beamAction.setBeamLocation(location);
                            main.getActionManager().addAction(beamAction, context, actionFor);
                        }));
    }

    public final String getBeamName() {
        return beamName;
    }

    public void setBeamName(final String beamName) {
        this.beamName = beamName;
    }

    public final boolean isRemove() {
        return remove;
    }

    public void setRemove(final boolean remove) {
        this.remove = remove;
    }

    public final Location getBeamLocation() {
        return beamLocation;
    }

    public void setBeamLocation(final Location beamLocation) {
        this.beamLocation = beamLocation;
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        if (isRemove()) {
            if (questPlayer != null) {
                if (questPlayer.getActiveLocationsAndBeacons().containsKey(getBeamName())) {
                    questPlayer.clearBeacons();
                }
            }
        } else {
            if (getBeamLocation() == null) {
                return;
            }
            questPlayer.trackBeacon(beamName, getBeamLocation());
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.beamName", getBeamName());
        configuration.set(initialPath + ".specifics.remove", isRemove());
        configuration.set(initialPath + ".specifics.location", getBeamLocation());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.beamName = configuration.getString(initialPath + ".specifics.beamName");
        this.remove = configuration.getBoolean(initialPath + ".specifics.remove");
        this.beamLocation = configuration.getLocation(initialPath + ".specifics.location", null);
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.beamName = arguments.get(0);

        this.remove = String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--remove");

        if (!remove) {
            final World world = Bukkit.getWorld(arguments.get(1));

            if (world != null) {
                final Vector coordinates =
                        new Vector(
                                Integer.parseInt(arguments.get(2)),
                                Integer.parseInt(arguments.get(3)),
                                Integer.parseInt(arguments.get(4)));

                this.beamLocation = coordinates.toLocation(world);
            }
        }
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        if (remove) {
            return "Despawns beam: " + getBeamName();
        } else {
            return "Spawns beam: " + getBeamName();
        }
    }
}
