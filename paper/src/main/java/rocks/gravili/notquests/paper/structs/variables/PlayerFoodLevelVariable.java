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

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerFoodLevelVariable extends Variable<Integer> {
    public PlayerFoodLevelVariable(final NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Integer getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
        return questPlayer == null ? 0 : questPlayer.getPlayer().getFoodLevel();
    }

    @Override
    public boolean setValueInternally(final Integer newValue, final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return false;
        }
        questPlayer.getPlayer().setFoodLevel(Math.max(0, Math.min(20, newValue)));
        return true;
    }

    @Override
    public List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        final List<String> possibleValues = new ArrayList<>();
        for (int foodLevel = 0; foodLevel <= 20; foodLevel++) {
            possibleValues.add(String.valueOf(foodLevel));
        }
        return possibleValues;
    }

    @Override
    public String getPlural() {
        return "Food Levels";
    }

    @Override
    public String getSingular() {
        return "Food Level";
    }
}
