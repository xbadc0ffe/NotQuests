/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class DistanceToLocationVariable extends Variable<Double> {
    public DistanceToLocationVariable(final NotQuests main) {
        super(main);

        addRequiredString(StringVariableValueParser.of("world", null, (context, input) -> {
            final List<String> suggestions = new ArrayList<>();
            for (final World world : Bukkit.getWorlds()) {
                suggestions.add(world.getName());
            }
            return suggestions;
        }));
        addRequiredNumber(NumberVariableValueParser.of("x", null));
        addRequiredNumber(NumberVariableValueParser.of("y", null));
        addRequiredNumber(NumberVariableValueParser.of("z", null));
    }

    @Override
    public Double getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return Double.MAX_VALUE;
        }

        final World targetWorld = Bukkit.getWorld(getRequiredStringValue("world"));
        if (targetWorld == null) {
            main.getLogManager().warn("Cannot calculate DistanceToLocation: world '"
                    + getRequiredStringValue("world")
                    + "' does not exist.");
            return Double.MAX_VALUE;
        }
        if (!questPlayer.getPlayer().getWorld().equals(targetWorld)) {
            return Double.MAX_VALUE;
        }

        final Location targetLocation = new Location(
                targetWorld,
                getRequiredNumberValue("x", questPlayer),
                getRequiredNumberValue("y", questPlayer),
                getRequiredNumberValue("z", questPlayer));
        return questPlayer.getPlayer().getLocation().distance(targetLocation);
    }

    @Override
    public boolean setValueInternally(final Double newValue, final QuestPlayer questPlayer, final Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        return List.of("0", "5", "10", "25", "50", "100");
    }

    @Override
    public String getPlural() {
        return "Distances";
    }

    @Override
    public String getSingular() {
        return "Distance";
    }
}
