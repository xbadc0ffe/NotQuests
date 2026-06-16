package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.registry.FieldTypes;

public final class Beam {
    private static final String BEAM_NAME = "beamName";
    private static final String REMOVE = "remove";
    private static final String LOCATION = "location";

    private Beam() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("Beam")
                .displayName("Beam")
                .description("Shows or removes a guiding beam marker for the target player.")
                .field(
                        BEAM_NAME,
                        FieldTypes.text((context, input) -> java.util.List.of("<beam name>"))
                                .config("specifics.beamName"),
                        "Beam identifier. Reusing the same identifier replaces or removes that player's existing beam.")
                .field(
                        REMOVE,
                        FieldTypes.presenceFlag().config("specifics.remove"),
                        "Whether this action removes the named beam instead of showing it.")
                .field(
                        LOCATION,
                        FieldTypes.storedLocation().config("specifics.location"),
                        "World location where the beam should point.")
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine(Beam::deserialize)
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null) {
                        return;
                    }
                    final String beamName = action.text(BEAM_NAME);
                    if (action.flag(REMOVE)) {
                        if (questPlayer.getActiveLocationsAndBeacons().containsKey(beamName)) {
                            questPlayer.clearBeacons();
                        }
                        return;
                    }
                    final Location location = action.action().value(LOCATION, Location.class);
                    if (location != null) {
                        questPlayer.trackBeacon(beamName, location);
                    }
                })
                .actionDescription((action, questPlayer, objects) ->
                        (action.flag(REMOVE) ? "Removes beam: " : "Shows beam: ") + action.text(BEAM_NAME))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        main.getCommandManager().getNQCommandManager().command(builder
                .required(
                        BEAM_NAME,
                        NQArguments.stringArgument(),
                        NQDescription.of("Beam identifier to remove from the target player's screen."))
                .literal("remove", NQDescription.of("Removes the named beam from the target player's screen."))
                .handler(context -> {
                    final DefinedAction action = type.createAction();
                    action.setValue(BEAM_NAME, context.get(BEAM_NAME));
                    action.setValue(REMOVE, true);
                    main.getActionCatalog().addAction(action, context, actionFor);
                }));

        main.getCommandManager().getNQCommandManager().command(builder
                .required(
                        BEAM_NAME,
                        NQArguments.stringArgument(),
                        NQDescription.of("Beam identifier to show or replace for the target player."))
                .literal("spawn", NQDescription.of("Shows the named beam at a fixed world location."))
                .required("world", NQArguments.worldArgument(), NQDescription.of("World where the beam should be shown."))
                .required("x", NQArguments.integerArgument(), NQDescription.of("X coordinate where the beam should be shown."))
                .required("y", NQArguments.integerArgument(), NQDescription.of("Y coordinate where the beam should be shown."))
                .required("z", NQArguments.integerArgument(), NQDescription.of("Z coordinate where the beam should be shown."))
                .handler(context -> {
                    final World world = context.get("world");
                    final Location location = new Vector(
                                    context.<Integer>get("x"),
                                    context.<Integer>get("y"),
                                    context.<Integer>get("z"))
                            .toLocation(world);
                    final DefinedAction action = type.createAction();
                    action.setValue(BEAM_NAME, context.get(BEAM_NAME));
                    action.setValue(REMOVE, false);
                    action.setValue(LOCATION, location);
                    main.getActionCatalog().addAction(action, context, actionFor);
                }));
    }

    private static void deserialize(final DefinedAction action, final ArrayList<String> arguments) {
        action.setValue(BEAM_NAME, arguments.get(0));
        final String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT);
        final boolean remove = joined.contains("--remove") || joined.contains(" remove");
        action.setValue(REMOVE, remove);
        if (remove) {
            return;
        }
        int offset = arguments.size() > 1 && arguments.get(1).equalsIgnoreCase("spawn") ? 1 : 0;
        if (arguments.size() < offset + 5) {
            return;
        }
        final World world = Bukkit.getWorld(arguments.get(offset + 1));
        if (world == null) {
            return;
        }
        action.setValue(
                LOCATION,
                new Vector(
                                Integer.parseInt(arguments.get(offset + 2)),
                                Integer.parseInt(arguments.get(offset + 3)),
                                Integer.parseInt(arguments.get(offset + 4)))
                        .toLocation(world));
    }
}
