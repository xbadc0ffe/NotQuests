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

public class PlayerSaturationVariable extends Variable<Float> {
    public PlayerSaturationVariable(final NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Float getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
        return questPlayer == null ? 0f : questPlayer.getPlayer().getSaturation();
    }

    @Override
    public boolean setValueInternally(final Float newValue, final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return false;
        }
        questPlayer.getPlayer().setSaturation(Math.max(0f, Math.min(20f, newValue)));
        return true;
    }

    @Override
    public List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        final List<String> possibleValues = new ArrayList<>();
        for (double saturation = 0; saturation <= 20; saturation += 0.5d) {
            possibleValues.add(String.valueOf(saturation));
        }
        return possibleValues;
    }

    @Override
    public String getPlural() {
        return "Saturation Values";
    }

    @Override
    public String getSingular() {
        return "Saturation";
    }
}
