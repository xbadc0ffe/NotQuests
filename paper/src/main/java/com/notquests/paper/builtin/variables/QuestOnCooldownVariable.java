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
import java.util.concurrent.TimeUnit;

/**
 * This variable is true if the Quest is on cooldown for the player
 */
public class QuestOnCooldownVariable extends Variable<Boolean> {
    public QuestOnCooldownVariable(NotQuests main) {
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

        // int completedAmount = 0; //only needed for maxAccepts

        long mostRecentCompleteTime = 0;
        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
            if (completedQuest.getQuest().equals(quest)) {
                // completedAmount += 1;
                if (completedQuest.getTimeCompleted() > mostRecentCompleteTime) {
                    mostRecentCompleteTime = completedQuest.getTimeCompleted();
                }
            }
        }

        final long completeTimeDifference = System.currentTimeMillis() - mostRecentCompleteTime;
        final long completeTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(completeTimeDifference);

        return completeTimeDifferenceMinutes < quest.getAcceptCooldownComplete(); // on cooldown
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
        return "Quest on cooldown";
    }

    @Override
    public String getSingular() {
        return "Quest on cooldown";
    }
}
