package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompletedObjectiveIDsOfQuestVariable extends Variable<String[]> {
    public CompletedObjectiveIDsOfQuestVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);

        addRequiredString(StringVariableValueParser.of("QuestName", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            for (Quest quest : main.getQuestManager().getAllQuests()) {
                suggestions.add(quest.getIdentifier());
            }
            return suggestions;
        }));
    }

    @Override
    public String[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
        final String questName = getRequiredStringValue("QuestName");

        ActiveQuest foundQuest = null;
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            if (activeQuest.getQuestIdentifier().equalsIgnoreCase(questName)) {
                foundQuest = activeQuest;
            }
        }
        final ArrayList<String> completedObjectivesOfQuestNames = new ArrayList<>();

        if (foundQuest == null) {
            return completedObjectivesOfQuestNames.toArray(new String[0]);
        }

        for (final ActiveObjective activeObjective : foundQuest.getCompletedObjectives()) {
            completedObjectivesOfQuestNames.add("" + activeObjective.getObjective().getObjectiveID());
        }

        return completedObjectivesOfQuestNames.toArray(new String[0]);
    }

    @Override
    public boolean setValueInternally(String[] newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer == null) {
            return false;
        }
        final String questName = getRequiredStringValue("QuestName");

        ActiveQuest foundQuest = null;
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            if (activeQuest.getQuestIdentifier().equalsIgnoreCase(questName)) {
                foundQuest = activeQuest;
            }
        }
        if (foundQuest == null) {
            return false;
        }

        final List<String> newValues = Arrays.asList(newValue);

        for (ActiveObjective activeObjective : foundQuest.getActiveObjectives()) {
            if (newValues.contains("" + activeObjective.getObjectiveID())) {
                activeObjective.addProgress(
                        (activeObjective.getProgressNeeded() - activeObjective.getCurrentProgress()));
            }
        }
        foundQuest.removeCompletedObjectives(true);
        questPlayer.removeCompletedQuests();

        return true;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Completed Objective IDs of Quest";
    }

    @Override
    public String getSingular() {
        return "Completed Objective ID of Quest";
    }
}
