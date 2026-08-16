package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import org.bukkit.World;
import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

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
