package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.registry.FieldTypes;

import static com.notquests.paper.commands.arguments.EntityTypeArgument.entityTypeArgument;

public final class SpawnMob {
    private static final String ENTITY_TYPE = "entityType";
    private static final String AMOUNT = "amount";
    private static final String USE_PLAYER_LOCATION = "usePlayerLocation";
    private static final String LOCATION = "location";
    private static final String SPAWN_RADIUS_X = "spawnRadiusX";
    private static final String SPAWN_RADIUS_Y = "spawnRadiusY";
    private static final String SPAWN_RADIUS_Z = "spawnRadiusZ";

    private SpawnMob() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("SpawnMob")
                .displayName("Spawn Mob")
                .description("Spawns vanilla, MythicMobs, or EcoMobs entities at a player or fixed location.")
                .field(ENTITY_TYPE, FieldTypes.entityType().config("specifics.mobToSpawn"), "Entity type or custom mob id to spawn.")
                .field(AMOUNT, FieldTypes.integer(1).config("specifics.amount"), "Number of mobs to spawn.")
                .field(USE_PLAYER_LOCATION, FieldTypes.presenceFlag().config("specifics.usePlayerLocation"), "Whether mobs spawn at the target player's current location.")
                .field(LOCATION, FieldTypes.storedLocation().config("specifics.spawnLocation"), "Fixed location where mobs should spawn.")
                .field(SPAWN_RADIUS_X, FieldTypes.integer(0).config("specifics.spawnRadiusX"), "Horizontal X radius used to randomize each spawn location.")
                .field(SPAWN_RADIUS_Y, FieldTypes.integer(0).config("specifics.spawnRadiusY"), "Vertical Y radius used to randomize each spawn location.")
                .field(SPAWN_RADIUS_Z, FieldTypes.integer(0).config("specifics.spawnRadiusZ"), "Horizontal Z radius used to randomize each spawn location.")
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine(SpawnMob::deserialize)
                .execute((action, questPlayer, objects) -> {
                    final Player player = questPlayer == null ? null : questPlayer.getPlayer();
                    if (player == null) {
                        return;
                    }
                    final Runnable spawn = () -> spawn(main, action.action(), player);
                    if (Bukkit.isPrimaryThread()) {
                        spawn.run();
                    } else {
                        Bukkit.getScheduler().runTask(main.getMain(), spawn);
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Spawns mob: " + action.text(ENTITY_TYPE))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        final NQFlag radiusX = NQFlag.builder(SPAWN_RADIUS_X, NQDescription.of("Horizontal X radius used to randomize each spawn location."))
                .withArgument(NQArguments.integerArgument()).build();
        final NQFlag radiusY = NQFlag.builder(SPAWN_RADIUS_Y, NQDescription.of("Vertical Y radius used to randomize each spawn location."))
                .withArgument(NQArguments.integerArgument()).build();
        final NQFlag radiusZ = NQFlag.builder(SPAWN_RADIUS_Z, NQDescription.of("Horizontal Z radius used to randomize each spawn location."))
                .withArgument(NQArguments.integerArgument()).build();

        final NQCommandBuilder common = builder
                .required(ENTITY_TYPE, entityTypeArgument(main), NQDescription.of("Entity type or custom mob id to spawn."))
                .required(AMOUNT, NQArguments.integerArgument(), NQDescription.of("Number of mobs to spawn."))
                .flag(radiusX)
                .flag(radiusY)
                .flag(radiusZ);

        main.getCommandManager().getNQCommandManager().command(common
                .literal("PlayerLocation", NQDescription.of("Spawns mobs at the target player's current location."))
                .handler(context -> {
                    final DefinedAction action = fromCommon(type, context, true, null);
                    main.getActionCatalog().addAction(action, context, actionFor);
                }));

        main.getCommandManager().getNQCommandManager().command(common
                .literal("Location", NQDescription.of("Spawns mobs at a fixed world location."))
                .required("world", NQArguments.worldArgument(), NQDescription.of("World where the mob should be spawned."))
                .required("x", NQArguments.integerArgument(), NQDescription.of("X coordinate where the mob should be spawned."))
                .required("y", NQArguments.integerArgument(), NQDescription.of("Y coordinate where the mob should be spawned."))
                .required("z", NQArguments.integerArgument(), NQDescription.of("Z coordinate where the mob should be spawned."))
                .handler(context -> {
                    final World world = context.get("world");
                    final Location location = new Vector(
                                    context.<Integer>get("x"),
                                    context.<Integer>get("y"),
                                    context.<Integer>get("z"))
                            .toLocation(world);
                    final DefinedAction action = fromCommon(type, context, false, location);
                    main.getActionCatalog().addAction(action, context, actionFor);
                }));
    }

    private static DefinedAction fromCommon(
            final ActionType type,
            final com.notquests.paper.commands.framework.NQCommandContext context,
            final boolean usePlayerLocation,
            final Location location) {
        final DefinedAction action = type.createAction();
        action.setValue(ENTITY_TYPE, context.get(ENTITY_TYPE));
        action.setValue(AMOUNT, context.get(AMOUNT));
        action.setValue(USE_PLAYER_LOCATION, usePlayerLocation);
        action.setValue(LOCATION, location);
        action.setValue(SPAWN_RADIUS_X, context.flags().getValue(SPAWN_RADIUS_X, 0));
        action.setValue(SPAWN_RADIUS_Y, context.flags().getValue(SPAWN_RADIUS_Y, 0));
        action.setValue(SPAWN_RADIUS_Z, context.flags().getValue(SPAWN_RADIUS_Z, 0));
        return action;
    }

    private static void deserialize(final DefinedAction action, final ArrayList<String> arguments) {
        action.setValue(ENTITY_TYPE, arguments.get(0));
        action.setValue(AMOUNT, Integer.parseInt(arguments.get(1)));
        final boolean usePlayerLocation = arguments.size() < 3 || arguments.get(2).equalsIgnoreCase("PlayerLocation");
        action.setValue(USE_PLAYER_LOCATION, usePlayerLocation);
        if (!usePlayerLocation && arguments.size() >= 6) {
            final World world = Bukkit.getWorld(arguments.get(2));
            if (world != null) {
                action.setValue(
                        LOCATION,
                        new Vector(
                                        Integer.parseInt(arguments.get(3)),
                                        Integer.parseInt(arguments.get(4)),
                                        Integer.parseInt(arguments.get(5)))
                                .toLocation(world));
            }
        }
    }

    private static void spawn(final NotQuests main, final DefinedAction action, final Player player) {
        final String entityTypeName = action.text(ENTITY_TYPE);
        final int amount = Math.max(1, action.value(AMOUNT, 1));
        final Location baseLocation = Boolean.TRUE.equals(action.value(USE_PLAYER_LOCATION, Boolean.class))
                ? player.getLocation().clone().add(new Vector(0, 1, 0))
                : action.value(LOCATION, Location.class);
        if (baseLocation == null || baseLocation.getWorld() == null) {
            main.getLogManager().warn("Tried to execute SpawnMob action with an invalid location.");
            return;
        }
        try {
            final EntityType entityType = EntityType.valueOf(entityTypeName.toUpperCase(Locale.ROOT));
            for (int i = 0; i < amount; i++) {
                baseLocation.getWorld().spawnEntity(randomLocation(action, baseLocation), entityType);
            }
        } catch (final IllegalArgumentException invalidVanillaMob) {
            if (main.getIntegrationsManager().isMythicMobsEnabled()
                    && main.getIntegrationsManager().getMythicMobsManager().isMythicMob(entityTypeName)) {
                main.getIntegrationsManager()
                        .getMythicMobsManager()
                        .spawnMob(entityTypeName, baseLocation, amount, location -> randomLocation(action, location));
            } else if (main.getIntegrationsManager().isEcoMobsEnabled()
                    && main.getIntegrationsManager().getEcoMobsManager().isEcoMob(entityTypeName)) {
                main.getIntegrationsManager()
                        .getEcoMobsManager()
                        .spawnMob(entityTypeName, baseLocation, amount, location -> randomLocation(action, location));
            } else {
                main.getLogManager().warn("Tried to execute SpawnMob with invalid mob type '" + entityTypeName + "'.");
            }
        }
    }

    private static Location randomLocation(final DefinedAction action, final Location baseLocation) {
        final int radiusX = action.value(SPAWN_RADIUS_X, 0);
        final int radiusY = action.value(SPAWN_RADIUS_Y, 0);
        final int radiusZ = action.value(SPAWN_RADIUS_Z, 0);
        if (radiusX == 0 && radiusY == 0 && radiusZ == 0) {
            return baseLocation;
        }
        return baseLocation.clone().add(randomOffset(radiusX), randomOffset(radiusY), randomOffset(radiusZ));
    }

    private static int randomOffset(final int radius) {
        if (radius == 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(-radius, radius + 1);
    }
}
