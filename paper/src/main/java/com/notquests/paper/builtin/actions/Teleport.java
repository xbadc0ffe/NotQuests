package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.registry.FieldTypes;

public final class Teleport {
    private static final String LOCATION = "location";
    private static final String YAW = "yaw";
    private static final String PITCH = "pitch";

    private Teleport() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("Teleport")
                .displayName("Teleport")
                .description("Teleports the target player to a fixed world location.")
                .field(
                        LOCATION,
                        FieldTypes.storedLocation().config("specifics.location"),
                        "World location where the target player should be teleported.")
                .flag(
                        YAW,
                        FieldTypes.optionalDouble().config("specifics.yaw"),
                        "Optional yaw rotation applied after teleporting.")
                .flag(
                        PITCH,
                        FieldTypes.optionalDouble().config("specifics.pitch"),
                        "Optional pitch rotation applied after teleporting.")
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine(Teleport::deserialize)
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null || questPlayer.getPlayer() == null) {
                        return;
                    }
                    final Location storedLocation = action.action().value(LOCATION, Location.class);
                    if (storedLocation == null || storedLocation.getWorld() == null) {
                        return;
                    }
                    final Location location = storedLocation.clone();
                    final Double yaw = action.action().value(YAW, Double.class);
                    final Double pitch = action.action().value(PITCH, Double.class);
                    if (yaw != null) {
                        location.setYaw(yaw.floatValue());
                    }
                    if (pitch != null) {
                        location.setPitch(pitch.floatValue());
                    }
                    questPlayer.getPlayer().teleport(location);
                })
                .actionDescription((action, questPlayer, objects) -> {
                    final Location location = action.action().value(LOCATION, Location.class);
                    return "Teleports player to: " + (location == null ? "unknown location" : location.toVector());
                })
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        main.getCommandManager().getNQCommandManager().command(builder
                .required("world", NQArguments.worldArgument(), NQDescription.of("World where the target player should be teleported."))
                .required("x", NQArguments.doubleArgument(), NQDescription.of("X coordinate where the target player should be teleported."))
                .required("y", NQArguments.doubleArgument(), NQDescription.of("Y coordinate where the target player should be teleported."))
                .required("z", NQArguments.doubleArgument(), NQDescription.of("Z coordinate where the target player should be teleported."))
                .flag(NQFlag.builder("yaw", NQDescription.of("Optional yaw rotation applied after teleporting."))
                        .withArgument(NQArguments.doubleArgument())
                        .build())
                .flag(NQFlag.builder("pitch", NQDescription.of("Optional pitch rotation applied after teleporting."))
                        .withArgument(NQArguments.doubleArgument())
                        .build())
                .handler(context -> {
                    final World world = context.get("world");
                    final DefinedAction action = type.createAction();
                    action.setValue(
                            LOCATION,
                            new Location(
                                    world,
                                    context.<Double>get("x"),
                                    context.<Double>get("y"),
                                    context.<Double>get("z")));
                    action.setValue(YAW, context.flags().getValue("yaw", null));
                    action.setValue(PITCH, context.flags().getValue("pitch", null));
                    main.getActionCatalog().addAction(action, context, actionFor);
                }));
    }

    private static void deserialize(final DefinedAction action, final ArrayList<String> arguments) {
        if (arguments.size() < 4) {
            return;
        }
        final World world = Bukkit.getWorld(arguments.get(0));
        if (world != null) {
            action.setValue(
                    LOCATION,
                    new Vector(
                                    Double.parseDouble(arguments.get(1)),
                                    Double.parseDouble(arguments.get(2)),
                                    Double.parseDouble(arguments.get(3)))
                            .toLocation(world));
        }
    }
}
