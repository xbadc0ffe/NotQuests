/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpawnParticleAction extends Action {
    private String particleName = "";
    private int count = 1;
    private boolean usePlayerLocation = true;
    private boolean showToEveryone = false;
    private Location particleLocation = null;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetZ = 0;
    private double speed = 0;

    public SpawnParticleAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder builder,
            final ActionFor actionFor) {
        final NQFlag offsetXFlag = NQFlag.builder("offsetX", NQDescription.of("Random X spread around the particle location."))
                .withArgument(NQArguments.doubleArgument())
                .build();
        final NQFlag offsetYFlag = NQFlag.builder("offsetY", NQDescription.of("Random Y spread around the particle location."))
                .withArgument(NQArguments.doubleArgument())
                .build();
        final NQFlag offsetZFlag = NQFlag.builder("offsetZ", NQDescription.of("Random Z spread around the particle location."))
                .withArgument(NQArguments.doubleArgument())
                .build();
        final NQFlag speedFlag = NQFlag.builder("speed", NQDescription.of("Particle speed value passed to Minecraft's particle system."))
                .withArgument(NQArguments.doubleArgument())
                .build();

        final NQCommandBuilder commonBuilder = builder
                .required("particle", NQArguments.stringArgument(), NQDescription.of("Particle effect to spawn. Only particles that do not require extra data are supported."), (context, input) -> particleSuggestions())
                .required("count", NQArguments.integerArgument(), NQDescription.of("Number of particles to spawn."))
                .flag(offsetXFlag)
                .flag(offsetYFlag)
                .flag(offsetZFlag)
                .flag(speedFlag)
                .flag(NQFlag.presence("forEveryone", NQDescription.of("Show the particles to every online player instead of only the target player.")));

        manager.command(commonBuilder
                .literal("PlayerLocation", NQDescription.of("Spawns the particles at the target player's current location."))
                .handler(context -> {
                    final SpawnParticleAction action = createFromContext(context.get("particle"), context.get("count"), true, null);
                    applyFlags(action, context.flags().<Double>getValue("offsetX", 0d), context.flags().<Double>getValue("offsetY", 0d),
                            context.flags().<Double>getValue("offsetZ", 0d), context.flags().<Double>getValue("speed", 0d),
                            context.flags().isPresent("forEveryone"));
                    main.getActionManager().addAction(action, context, actionFor);
                }));

        manager.command(commonBuilder
                .literal("Location", NQDescription.of("Spawns the particles at a fixed world location."))
                .required("world", NQArguments.worldArgument(), NQDescription.of("World where the particles should be spawned."))
                .required("x", NQArguments.doubleArgument(), NQDescription.of("X coordinate where the particles should be spawned."))
                .required("y", NQArguments.doubleArgument(), NQDescription.of("Y coordinate where the particles should be spawned."))
                .required("z", NQArguments.doubleArgument(), NQDescription.of("Z coordinate where the particles should be spawned."))
                .handler(context -> {
                    final World world = context.get("world");
                    final Location location = new Location(world, context.get("x"), context.get("y"), context.get("z"));
                    final SpawnParticleAction action = createFromContext(context.get("particle"), context.get("count"), false, location);
                    applyFlags(action, context.flags().<Double>getValue("offsetX", 0d), context.flags().<Double>getValue("offsetY", 0d),
                            context.flags().<Double>getValue("offsetZ", 0d), context.flags().<Double>getValue("speed", 0d),
                            context.flags().isPresent("forEveryone"));
                    main.getActionManager().addAction(action, context, actionFor);
                }));
    }

    private static SpawnParticleAction createFromContext(
            final String particleName,
            final int count,
            final boolean usePlayerLocation,
            final Location particleLocation) {
        final SpawnParticleAction action = new SpawnParticleAction(NotQuests.getInstance());
        action.setParticleName(particleName);
        action.setCount(count);
        action.setUsePlayerLocation(usePlayerLocation);
        action.setParticleLocation(particleLocation);
        return action;
    }

    private static void applyFlags(
            final SpawnParticleAction action,
            final double offsetX,
            final double offsetY,
            final double offsetZ,
            final double speed,
            final boolean showToEveryone) {
        action.setOffsetX(offsetX);
        action.setOffsetY(offsetY);
        action.setOffsetZ(offsetZ);
        action.setSpeed(speed);
        action.setShowToEveryone(showToEveryone);
    }

    public String getParticleName() {
        return particleName;
    }

    public void setParticleName(final String particleName) {
        this.particleName = particleName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = Math.max(1, count);
    }

    public boolean isUsePlayerLocation() {
        return usePlayerLocation;
    }

    public void setUsePlayerLocation(final boolean usePlayerLocation) {
        this.usePlayerLocation = usePlayerLocation;
    }

    public boolean isShowToEveryone() {
        return showToEveryone;
    }

    public void setShowToEveryone(final boolean showToEveryone) {
        this.showToEveryone = showToEveryone;
    }

    public Location getParticleLocation() {
        return particleLocation;
    }

    public void setParticleLocation(final Location particleLocation) {
        this.particleLocation = particleLocation;
    }

    public void setOffsetX(final double offsetX) {
        this.offsetX = Math.max(0, offsetX);
    }

    public void setOffsetY(final double offsetY) {
        this.offsetY = Math.max(0, offsetY);
    }

    public void setOffsetZ(final double offsetZ) {
        this.offsetZ = Math.max(0, offsetZ);
    }

    public void setSpeed(final double speed) {
        this.speed = Math.max(0, speed);
    }

    @Override
    protected void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return;
        }
        final Particle particle = findParticle(getParticleName());
        if (particle == null || particle.getDataType() != Void.class) {
            main.getLogManager().warn("Cannot execute SpawnParticle action: particle '"
                    + getParticleName()
                    + "' is unknown or requires extra data.");
            return;
        }

        final Player player = questPlayer.getPlayer();
        final Location location = isUsePlayerLocation() ? player.getLocation() : getParticleLocation();
        if (location == null || location.getWorld() == null) {
            main.getLogManager().warn("Cannot execute SpawnParticle action: target location has no world.");
            return;
        }

        try {
            if (isShowToEveryone()) {
                location.getWorld().spawnParticle(particle, location, getCount(), offsetX, offsetY, offsetZ, speed);
            } else {
                player.spawnParticle(particle, location, getCount(), offsetX, offsetY, offsetZ, speed);
            }
        } catch (final IllegalArgumentException exception) {
            main.getLogManager().warn("Cannot execute SpawnParticle action with particle '"
                    + getParticleName()
                    + "': "
                    + exception.getMessage());
        }
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.particleName", getParticleName());
        configuration.set(initialPath + ".specifics.count", getCount());
        configuration.set(initialPath + ".specifics.usePlayerLocation", isUsePlayerLocation());
        configuration.set(initialPath + ".specifics.showToEveryone", isShowToEveryone());
        configuration.set(initialPath + ".specifics.location", getParticleLocation());
        configuration.set(initialPath + ".specifics.offsetX", offsetX);
        configuration.set(initialPath + ".specifics.offsetY", offsetY);
        configuration.set(initialPath + ".specifics.offsetZ", offsetZ);
        configuration.set(initialPath + ".specifics.speed", speed);
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        particleName = configuration.getString(initialPath + ".specifics.particleName", "");
        count = configuration.getInt(initialPath + ".specifics.count", 1);
        usePlayerLocation = configuration.getBoolean(initialPath + ".specifics.usePlayerLocation", true);
        showToEveryone = configuration.getBoolean(initialPath + ".specifics.showToEveryone", false);
        particleLocation = configuration.getLocation(initialPath + ".specifics.location", null);
        offsetX = configuration.getDouble(initialPath + ".specifics.offsetX", 0);
        offsetY = configuration.getDouble(initialPath + ".specifics.offsetY", 0);
        offsetZ = configuration.getDouble(initialPath + ".specifics.offsetZ", 0);
        speed = configuration.getDouble(initialPath + ".specifics.speed", 0);
    }

    @Override
    public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
        if (arguments.size() < 3) {
            return;
        }
        particleName = arguments.get(0);
        count = Integer.parseInt(arguments.get(1));
        usePlayerLocation = arguments.get(2).equalsIgnoreCase("PlayerLocation");
        if (!usePlayerLocation && arguments.size() >= 7) {
            final World world = Bukkit.getWorld(arguments.get(3));
            if (world != null) {
                particleLocation = new Vector(
                        Double.parseDouble(arguments.get(4)),
                        Double.parseDouble(arguments.get(5)),
                        Double.parseDouble(arguments.get(6))).toLocation(world);
            }
        }
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Spawns particle: " + getParticleName();
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
