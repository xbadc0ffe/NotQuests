package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.NumberVariableArgument;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveType;

public final class Interact {
    private static final int LOOKING_TARGET_RANGE = 120;
    private static final String LOCATION = "locationToInteract";
    private static final String LEFT_CLICK = "leftClick";
    private static final String RIGHT_CLICK = "rightClick";
    private static final String MAX_DISTANCE = "maxDistance";
    private static final String CANCEL_INTERACTION = "cancelInteraction";

    private Interact() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("Interact")
                .displayName("Interact")
                .description("Counts clicks on a configured block or location.")
                .field("amount", FieldTypes.numberExpression(false).progressNeeded(), "Number of matching interactions required.")
                .field(LOCATION, FieldTypes.storedLocation().config("specifics.locationToInteract"), "Block or location the player must interact with.")
                .field(LEFT_CLICK, FieldTypes.presenceFlag().config("specifics.leftClick"), "Whether left-clicks count.")
                .field(RIGHT_CLICK, FieldTypes.presenceFlag().config("specifics.rightClick"), "Whether right-clicks count.")
                .field(MAX_DISTANCE, FieldTypes.storedInteger(1).config("specifics.maxDistance"), "Maximum distance in blocks from the configured location.")
                .field(CANCEL_INTERACTION, FieldTypes.presenceFlag().config("specifics.cancelInteraction"), "Whether matching interactions are cancelled while this objective is active.")
                .commands((type, builder, level) -> registerCommands(main, type, builder, level))
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .on(PlayerInteractEvent.class, (event, objective) -> {
                    final Location target = objective.value(LOCATION, Location.class);
                    if (target == null
                            || event.getClickedBlock() == null
                            || event.getClickedBlock().getLocation().getWorld() == null
                            || target.getWorld() == null) {
                        return;
                    }
                    if (!countsInteractionAction(
                            event.getAction(),
                            Boolean.TRUE.equals(objective.value(LEFT_CLICK, Boolean.class)),
                            Boolean.TRUE.equals(objective.value(RIGHT_CLICK, Boolean.class)))) {
                        return;
                    }
                    if (!event.getClickedBlock().getLocation().getWorld().getName().equalsIgnoreCase(target.getWorld().getName())) {
                        return;
                    }
                    final Integer maxDistance = objective.value(MAX_DISTANCE, Integer.class);
                    if (event.getClickedBlock().getLocation().distance(target) > (maxDistance == null ? 1 : maxDistance)) {
                        return;
                    }
                    objective.addProgress(1);
                    if (Boolean.TRUE.equals(objective.value(CANCEL_INTERACTION, Boolean.class))) {
                        event.setCancelled(true);
                    }
                })
                .afterLoad((objective, context) -> objective.setLocation(objective.value(LOCATION, Location.class), false))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandBuilder builder,
            final int level) {
        final NQCommandBuilder amountBuilder = builder.required(
                "amount",
                NumberVariableArgument.numberVariableArgument("amount", null, false),
                NQDescription.of("Amount of interactions needed."));

        main.getCommandManager().getNQCommandManager().command(withInteractionFlags(main, amountBuilder
                .required("world", NQArguments.worldArgument(), NQDescription.of("World containing the block or location the player must interact with."))
                .required("x", NQArguments.integerArgument(), NQDescription.of("X coordinate of the block or location the player must interact with."))
                .required("y", NQArguments.integerArgument(), NQDescription.of("Y coordinate of the block or location the player must interact with."))
                .required("z", NQArguments.integerArgument(), NQDescription.of("Z coordinate of the block or location the player must interact with.")))
                .handler(context -> {
                    final World world = context.get("world");
                    final Vector coordinates =
                            new Vector(context.get("x"), context.get("y"), context.get("z"));
                    addObjective(main, type, context, level, coordinates.toLocation(world));
                }));

        main.getCommandManager().getNQCommandManager().command(withInteractionFlags(main, amountBuilder
                .literal("looking", NQDescription.of("Uses the block you are looking at as the interaction location.")))
                .handler(context -> {
                    if (!(context.sender() instanceof final Player player)) {
                        context.sender().sendMessage(main.parse(
                                "<error>This shortcut can only be used by a player. Use the coordinate form from console."));
                        return;
                    }
                    final Block targetBlock = player.getTargetBlockExact(LOOKING_TARGET_RANGE);
                    if (targetBlock == null) {
                        context.sender().sendMessage(main.parse("<error>No block found in your line of sight."));
                        return;
                    }
                    addObjective(main, type, context, level, targetBlock.getLocation());
                }));
    }

    private static NQCommandBuilder withInteractionFlags(final NotQuests main, final NQCommandBuilder builder) {
        return builder
                .flag(NQFlag.presence(LEFT_CLICK, NQDescription.of("Count left-clicks of the location.")))
                .flag(NQFlag.presence(RIGHT_CLICK, NQDescription.of("Count right-clicks of the location.")))
                .flag(NQFlag.presence(CANCEL_INTERACTION, NQDescription.of("Cancel matching interactions while this objective is active.")))
                .flag(main.getCommandManager().maxDistance);
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level,
            final Location location) {
        final DefinedObjective objective = type.createObjective();
        objective.setProgressNeededExpression(context.get("amount"));
        objective.setValue(LOCATION, location);
        objective.setValue(LEFT_CLICK, context.flags().isPresent(LEFT_CLICK));
        objective.setValue(RIGHT_CLICK, context.flags().isPresent(RIGHT_CLICK));
        objective.setValue(MAX_DISTANCE, context.flags().getValue(main.getCommandManager().maxDistance, 1));
        objective.setValue(CANCEL_INTERACTION, context.flags().isPresent(CANCEL_INTERACTION));
        objective.setLocation(location, false);
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    public static boolean countsInteractionAction(final Action action, final boolean leftClick, final boolean rightClick) {
        if (action == Action.RIGHT_CLICK_BLOCK) {
            return rightClick || (!leftClick && !rightClick);
        }
        if (action == Action.LEFT_CLICK_BLOCK) {
            return leftClick || (!leftClick && !rightClick);
        }
        return false;
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final com.notquests.paper.structs.ActiveObjective activeObjective) {
        final boolean leftClick = Boolean.TRUE.equals(objective.value(LEFT_CLICK, Boolean.class));
        final boolean rightClick = Boolean.TRUE.equals(objective.value(RIGHT_CLICK, Boolean.class));
        String interactType = "";
        if (leftClick) {
            interactType = "Left-Click";
        }
        if (rightClick) {
            interactType = "Right-Click";
        }
        if (leftClick && rightClick) {
            interactType = "Left/Right-Click";
        }
        if (!leftClick && !rightClick) {
            interactType = "Interact with";
        }

        final Location location = objective.value(LOCATION, Location.class);
        String worldName = "???";
        if (location != null && location.getWorld() != null) {
            worldName = location.getWorld().getName();
        }

        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.interact.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%INTERACTTYPE%", interactType,
                                "%COORDINATES%",
                                location == null
                                        ? "???"
                                        : "X: " + location.getX() + " Y: " + location.getY() + " Z: " + location.getZ(),
                                "%WORLDNAME%", worldName));
    }
}
