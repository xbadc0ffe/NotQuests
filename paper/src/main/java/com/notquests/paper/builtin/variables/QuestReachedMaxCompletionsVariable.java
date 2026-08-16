package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.structs.CompletedQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * This variable is true if the amount of times the player has previously completed this Quest is
 * equal or higher than the Quests max completions
 */
public class QuestReachedMaxCompletionsVariable extends Variable<Boolean> {
    public QuestReachedMaxCompletionsVariable(NotQuests main) {
        super(main);
        addRequiredString(StringVariableValueParser.of("Quest to check", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            for (Quest quest : main.getQuestManager().getAllQuests()) {
                suggestions.add(quest.getIdentifier());
            }
            return suggestions;
        }));
    }

    @Override
    public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
        final Quest quest = main.getQuestManager().getQuest(getRequiredStringValue("Quest to check"));

        if (quest == null || questPlayer == null) {
            return false;
        }

        if (quest.getMaxCompletions() <= -1) {
            return false;
        } else if (quest.getMaxCompletions() == 0) {
            return true;
        }

        int completedAmount = 0; // only needed for maxCompletions

        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
            if (completedQuest.getQuest().equals(quest)) {
                completedAmount += 1;
            }
        }

        return completedAmount >= quest.getMaxCompletions();
    }

    @Override
    public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Quest reached max completions";
    }

    @Override
    public String getSingular() {
        return "Quest reached max completions";
    }
}
