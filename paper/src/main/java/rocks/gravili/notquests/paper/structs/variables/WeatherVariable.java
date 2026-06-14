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

import org.bukkit.World;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class WeatherVariable extends Variable<String> {
    public WeatherVariable(final NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return null;
        }

        final World world = questPlayer.getPlayer().getWorld();
        if (world.isThundering()) {
            return "thunder";
        }
        if (world.hasStorm()) {
            return "rain";
        }
        return "clear";
    }

    @Override
    public boolean setValueInternally(final String newValue, final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return false;
        }

        final World world = questPlayer.getPlayer().getWorld();
        if (newValue.equalsIgnoreCase("clear")) {
            world.setStorm(false);
            world.setThundering(false);
            return true;
        }
        if (newValue.equalsIgnoreCase("rain")) {
            world.setStorm(true);
            world.setThundering(false);
            return true;
        }
        if (newValue.equalsIgnoreCase("thunder")) {
            world.setStorm(true);
            world.setThundering(true);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        return List.of("clear", "rain", "thunder");
    }

    @Override
    public String getPlural() {
        return "Weather States";
    }

    @Override
    public String getSingular() {
        return "Weather";
    }
}
