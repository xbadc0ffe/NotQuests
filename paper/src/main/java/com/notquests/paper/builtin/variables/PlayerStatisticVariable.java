package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import org.bukkit.Statistic;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerStatisticVariable extends Variable<Integer> {
    public PlayerStatisticVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
        addRequiredString(StringVariableValueParser.of("Statistic", null, (context, input) -> {
            final ArrayList<String> suggestions = new ArrayList<>();
            for (final Statistic statistic : Statistic.values()) {
                if (statistic.getType() == Statistic.Type.UNTYPED) {
                    suggestions.add(statistic.name());
                }
            }
            suggestions.add("<Enter Statistic name>");
            return suggestions;
        }));
    }

    @Override
    public Integer getValueInternally(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            final String statisticName = getRequiredStringValue("Statistic");
            final Statistic statistic;
            try {
                statistic = Statistic.valueOf(statisticName);
            } catch (final IllegalArgumentException e) {
                main.getLogManager().severe("Tried to get statistic with name <highlight>" + statisticName + "</highlight2> when getting variable but it doesn't exist! This is not an error in NotQuests - you simply entered a statistic which does not exist. Please fix it!");
                return null;
            }

            return questPlayer.getPlayer().getStatistic(statistic);
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            final String statisticName = getRequiredStringValue("Statistic");
            final Statistic statistic;
            try {
                statistic = Statistic.valueOf(statisticName);
            } catch (final IllegalArgumentException e) {
                main.getLogManager().severe("Tried to get statistic with name <highlight>" + statisticName + "</highlight2> when setting variable but it doesn't exist! This is not an error in NotQuests - you simply entered a statistic which does not exist. Please fix it!");
                return false;
            }
            questPlayer.getPlayer().setStatistic(statistic, newValue);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Statistics";
    }

    @Override
    public String getSingular() {
        return "Statistic";
    }
}
