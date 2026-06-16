package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.structs.*;
import com.notquests.paper.conditions.Condition;
import com.notquests.paper.conditions.Condition.ConditionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This variable is true if the player is able to take the Quest. That means, they fulfill all Quest
 * conditions, as well as other factors like the Quest cooldown or maxAccepts.
 */
public class QuestAbleToAcceptVariable extends Variable<Boolean> {
    public QuestAbleToAcceptVariable(NotQuests main) {
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

        if (quest == null) {
            return true;
        }

        if (questPlayer != null) {

            int completedAmount = 0;
            long mostRecentCompleteTime = 0;

            int failedAmount = 0;
            long mostRecentFailTime = 0;

            int acceptedAmount = 0;
            for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                if (completedQuest.getQuest().equals(quest)) {
                    completedAmount += 1;
                    acceptedAmount += 1;
                    if (completedQuest.getTimeCompleted() > mostRecentCompleteTime) {
                        mostRecentCompleteTime = completedQuest.getTimeCompleted();
                    }
                }
            }
            for (final FailedQuest failedQuest : questPlayer.getFailedQuests()) {
                if (failedQuest.getQuest().equals(quest)) {
                    failedAmount += 1;
                    acceptedAmount += 1;
                    if (failedQuest.getTimeFailed() > mostRecentFailTime) {
                        mostRecentFailTime = failedQuest.getTimeFailed();
                    }
                }
            }
            for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                if (activeQuest.getQuest().equals(quest)) {
                    acceptedAmount += 1;
                }
            }


            final long completeTimeDifference = System.currentTimeMillis() - mostRecentCompleteTime;
            final long completeTimeDifferenceMinutes =
                    TimeUnit.MILLISECONDS.toMinutes(completeTimeDifference);

            if (completeTimeDifferenceMinutes < quest.getAcceptCooldownComplete()
                    || quest.getMaxCompletions() == 0
                    || (quest.getMaxCompletions() > -1 && completedAmount >= quest.getMaxCompletions())
                    || (quest.getMaxAccepts() > -1 && acceptedAmount >= quest.getMaxAccepts())
                    || (quest.getMaxFails() > -1 && failedAmount >= quest.getMaxFails())
            ) {
                return false;
            }
        } else {
            if (quest.getMaxAccepts() == 0) {
                return false;
            }
        }

        for (final Condition condition : quest.getRequirements()) {
            final ConditionResult check = condition.check(questPlayer);
            if (!check.fulfilled()) {
                return false;
            }
        }

        return true; // Able to accept the Quest
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
        return "Able to accept Quest";
    }

    @Override
    public String getSingular() {
        return "Able to accept Quest";
    }
}
