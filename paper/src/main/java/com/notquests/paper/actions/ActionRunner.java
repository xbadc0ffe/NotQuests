package com.notquests.paper.actions;

import com.notquests.paper.NotQuests;
import com.notquests.paper.conditions.Condition;
import com.notquests.paper.conditions.Condition.ConditionResult;
import com.notquests.paper.structs.QuestPlayer;
import org.bukkit.command.CommandSender;

public final class ActionRunner {
    private final NotQuests main;

    public ActionRunner(final NotQuests main) {
        this.main = main;
    }

    public void executeActionWithConditions(
            final Action action,
            final QuestPlayer questPlayer,
            final CommandSender sender,
            final boolean silent,
            final Object... objects) {
        executeActionWithConditions(action, questPlayer, sender, silent, -1, objects);
    }

    public void executeActionWithConditions(
            final Action action,
            final QuestPlayer questPlayer,
            final CommandSender sender,
            final boolean silent,
            final int delay,
            final Object... objects) {
        main.getLogManager()
                .debug(
                        "Executing Action "
                                + action.getActionName()
                                + " of type "
                                + action.getActionType()
                                + " with conditions!");
        questPlayer.sendDebugMessage(
                "Executing Action "
                        + action.getActionName()
                        + " of type "
                        + action.getActionType()
                        + " with conditions!");

        if (action.getConditions().isEmpty()) {
            main.getLogManager().debug("   Skipping Conditions");
            action.execute(questPlayer, delay, objects);
            if (!silent) {
                sender.sendMessage(
                        main.parse(
                                "<success>Action with the name <highlight>"
                                        + action.getActionName()
                                        + "</highlight> has been executed!"));
            }
            return;
        }

        final StringBuilder unfulfilledConditions = new StringBuilder();
        for (final Condition condition : action.getConditions()) {
            final ConditionResult check = condition.check(questPlayer);
            main.getLogManager().debug("   Condition Check Result: " + check.message());
            if (!check.fulfilled()) {
                unfulfilledConditions.append("\n").append(check.message());
            }
        }

        if (!unfulfilledConditions.toString().isBlank()) {
            if (!silent) {
                sender.sendMessage(
                        main.parse(
                                main.getLanguageManager()
                                        .getString(
                                                "chat.action-not-all-conditions-fulfilled",
                                                questPlayer.getPlayer(),
                                                questPlayer)
                                        + unfulfilledConditions));
            }
            questPlayer.sendDebugMessage(
                    "Skipping action "
                            + action.getActionName()
                            + ". Unfulfilled conditions: "
                            + unfulfilledConditions);
        } else {
            main.getLogManager().debug("   All Conditions fulfilled!");

            action.execute(questPlayer, objects);
            if (!silent) {
                sender.sendMessage(
                        main.parse(
                                "<success>Action with the name <highlight>"
                                        + action.getActionName()
                                        + "</highlight> has been executed!"));
            }
        }
    }
}
