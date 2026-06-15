package rocks.gravili.notquests.paper.structs.variables;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.FailedQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * This variable is true if the amount of times the player has previously failed this Quest is
 * equal or higher than the Quests max fails
 */
public class QuestReachedMaxFailsVariable extends Variable<Boolean> {
    public QuestReachedMaxFailsVariable(NotQuests main) {
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

        if (quest.getMaxFails() <= -1) {
            return false;
        } else if (quest.getMaxFails() == 0) {
            return true;
        }

        int failedAmount = 0; // only needed for maxFails

        for (final FailedQuest failedQuest : questPlayer.getFailedQuests()) {
            if (failedQuest.getQuest().equals(quest)) {
                failedAmount += 1;
            }
        }

        return failedAmount >= quest.getMaxFails();
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
        return "Quest reached max fails";
    }

    @Override
    public String getSingular() {
        return "Quest reached max fails";
    }
}
