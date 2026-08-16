package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
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

public final class SpawnParticle {
    private static final String PARTICLE = "particle";
    private static final String COUNT = "count";
    private static final String USE_PLAYER_LOCATION = "usePlayerLocation";
    private static final String SHOW_TO_EVERYONE = "forEveryone";
    private static final String LOCATION = "location";
    private static final String OFFSET_X = "offsetX";
    private static final String OFFSET_Y = "offsetY";
    private static final String OFFSET_Z = "offsetZ";
    private static final String SPEED = "speed";

    private SpawnParticle() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("SpawnParticle")
                .displayName("Spawn Particle")
                .description("Spawns a particle effect at the target player or a fixed location.")
                .field(PARTICLE, FieldTypes.text((context, input) -> particleSuggestions()).config("specifics.particleName"), "Particle effect to spawn. Only particles that do not require extra data are supported.")
                .field(COUNT, FieldTypes.integer(1).config("specifics.count"), "Number of particles to spawn.")
                .field(USE_PLAYER_LOCATION, FieldTypes.presenceFlag().config("specifics.usePlayerLocation"), "Whether particles spawn at the target player's current location.")
                .field(SHOW_TO_EVERYONE, FieldTypes.presenceFlag().config("specifics.showToEveryone"), "Whether every online player should see the particle effect.")
                .field(LOCATION, FieldTypes.storedLocation().config("specifics.location"), "Fixed world location where the particles should spawn.")
                .field(OFFSET_X, FieldTypes.doubleNumber(0).config("specifics.offsetX"), "Random X spread around the particle location.")
                .field(OFFSET_Y, FieldTypes.doubleNumber(0).config("specifics.offsetY"), "Random Y spread around the particle location.")
                .field(OFFSET_Z, FieldTypes.doubleNumber(0).config("specifics.offsetZ"), "Random Z spread around the particle location.")
                .field(SPEED, FieldTypes.doubleNumber(0).config("specifics.speed"), "Particle speed value passed to Minecraft's particle system.")
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine(SpawnParticle::deserialize)
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null || questPlayer.getPlayer() == null) {
                        return;
                    }
                    final Particle particle = findParticle(action.text(PARTICLE));
                    if (particle == null || particle.getDataType() != Void.class) {
                        main.getLogManager().warn("Cannot execute SpawnParticle action: particle '"
                                + action.text(PARTICLE)
                                + "' is unknown or requires extra data.");
                        return;
                    }
                    final Player player = questPlayer.getPlayer();
                    final Location location = action.flag(USE_PLAYER_LOCATION)
                            ? player.getLocation()
                            : action.action().value(LOCATION, Location.class);
                    if (location == null || location.getWorld() == null) {
                        main.getLogManager().warn("Cannot execute SpawnParticle action: target location has no world.");
                        return;
                    }
                    final int count = Math.max(1, action.integer(COUNT, 1));
                    final double offsetX = Math.max(0, action.action().value(OFFSET_X, 0d));
                    final double offsetY = Math.max(0, action.action().value(OFFSET_Y, 0d));
                    final double offsetZ = Math.max(0, action.action().value(OFFSET_Z, 0d));
                    final double speed = Math.max(0, action.action().value(SPEED, 0d));
                    try {
                        if (action.flag(SHOW_TO_EVERYONE)) {
                            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
                        } else {
                            player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
                        }
                    } catch (final IllegalArgumentException exception) {
                        main.getLogManager().warn("Cannot execute SpawnParticle action with particle '"
                                + action.text(PARTICLE)
                                + "': "
                                + exception.getMessage());
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Spawns particle: " + action.text(PARTICLE))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        final NQFlag offsetX = NQFlag.builder(OFFSET_X, NQDescription.of("Random X spread around the particle location."))
                .withArgument(NQArguments.doubleArgument()).build();
        final NQFlag offsetY = NQFlag.builder(OFFSET_Y, NQDescription.of("Random Y spread around the particle location."))
                .withArgument(NQArguments.doubleArgument()).build();
        final NQFlag offsetZ = NQFlag.builder(OFFSET_Z, NQDescription.of("Random Z spread around the particle location."))
                .withArgument(NQArguments.doubleArgument()).build();
        final NQFlag speed = NQFlag.builder(SPEED, NQDescription.of("Particle speed value passed to Minecraft's particle system."))
                .withArgument(NQArguments.doubleArgument()).build();
        final NQCommandBuilder common = builder
                .required(PARTICLE, NQArguments.stringArgument(), NQDescription.of("Particle effect to spawn. Only particles that do not require extra data are supported."), (context, input) -> particleSuggestions())
                .required(COUNT, NQArguments.integerArgument(), NQDescription.of("Number of particles to spawn."))
                .flag(offsetX)
                .flag(offsetY)
                .flag(offsetZ)
                .flag(speed)
                .flag(NQFlag.presence(SHOW_TO_EVERYONE, NQDescription.of("Show the particles to every online player instead of only the target player.")));

        main.getCommandManager().getNQCommandManager().command(common
                .literal("PlayerLocation", NQDescription.of("Spawns the particles at the target player's current location."))
                .handler(context -> {
                    final DefinedAction action = fromCommon(type, context, true, null);
                    main.getActionCatalog().addAction(action, context, actionFor);
                }));

        main.getCommandManager().getNQCommandManager().command(common
                .literal("Location", NQDescription.of("Spawns the particles at a fixed world location."))
                .required("world", NQArguments.worldArgument(), NQDescription.of("World where the particles should be spawned."))
                .required("x", NQArguments.doubleArgument(), NQDescription.of("X coordinate where the particles should be spawned."))
                .required("y", NQArguments.doubleArgument(), NQDescription.of("Y coordinate where the particles should be spawned."))
                .required("z", NQArguments.doubleArgument(), NQDescription.of("Z coordinate where the particles should be spawned."))
                .handler(context -> {
                    final World world = context.get("world");
                    final Location location = new Location(world, context.get("x"), context.get("y"), context.get("z"));
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
        action.setValue(PARTICLE, context.get(PARTICLE));
        action.setValue(COUNT, context.get(COUNT));
        action.setValue(USE_PLAYER_LOCATION, usePlayerLocation);
        action.setValue(SHOW_TO_EVERYONE, context.flags().isPresent(SHOW_TO_EVERYONE));
        action.setValue(LOCATION, location);
        action.setValue(OFFSET_X, context.flags().getValue(OFFSET_X, 0d));
        action.setValue(OFFSET_Y, context.flags().getValue(OFFSET_Y, 0d));
        action.setValue(OFFSET_Z, context.flags().getValue(OFFSET_Z, 0d));
        action.setValue(SPEED, context.flags().getValue(SPEED, 0d));
        return action;
    }

    private static void deserialize(final DefinedAction action, final ArrayList<String> arguments) {
        if (arguments.size() < 3) {
            return;
        }
        action.setValue(PARTICLE, arguments.get(0));
        action.setValue(COUNT, Integer.parseInt(arguments.get(1)));
        final boolean usePlayerLocation = arguments.get(2).equalsIgnoreCase("PlayerLocation");
        action.setValue(USE_PLAYER_LOCATION, usePlayerLocation);
        if (!usePlayerLocation && arguments.size() >= 7) {
            final World world = Bukkit.getWorld(arguments.get(3));
            if (world != null) {
                action.setValue(
                        LOCATION,
                        new Vector(
                                        Double.parseDouble(arguments.get(4)),
                                        Double.parseDouble(arguments.get(5)),
                                        Double.parseDouble(arguments.get(6)))
                                .toLocation(world));
            }
        }
    }

    private static Particle findParticle(final String name) {
        for (final Particle particle : Particle.values()) {
            if (particle.name().equalsIgnoreCase(name)
                    || particle.getKey().getKey().equalsIgnoreCase(name)
                    || particle.getKey().asString().equalsIgnoreCase(name)) {
                return particle;
            }
        }
        return null;
    }

    private static List<String> particleSuggestions() {
        final List<String> completions = new ArrayList<>();
        for (final Particle particle : Particle.values()) {
            if (particle.getDataType() == Void.class) {
                completions.add(particle.getKey().getKey().toLowerCase(Locale.ROOT));
            }
        }
        return completions;
    }
}
