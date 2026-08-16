package com.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.BaseCommand;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQCommandManager;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.CompletedQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

import static com.notquests.paper.commands.arguments.QuestArgument.questArgument;

public class QuestResetAndRemoveCommand extends BaseCommand {
    public QuestResetAndRemoveCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {

        builder = builder.commandDescription(NQDescription.of("Removes the quest from a specific player players, removes it from completed quests, resets the accept cooldown and basically everything else."))
                .literal("resetAndRemoveQuest", NQDescription.of("Removes a quest from player progress data."));
        commandManager.command(builder
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player whose quest data should be reset and removed."))
                .required("quest", questArgument(notQuests), NQDescription.of("Name of the Quest which should be reset and removed."))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    final OfflinePlayer player = context.get("player");

                    removeQuest(player, context);
                    context.sender().sendMessage(notQuests.parse("<success>Operation done!"));
                }));

        commandManager.command(builder
                .literal("all", NQDescription.of("Applies this operation to all matching players or entries."))
                .required("quest", questArgument(notQuests), NQDescription.of("Name of the Quest which should be reset and removed."))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());

                    notQuests.getQuestPlayerManager().getAllQuestPlayersForAllProfiles().forEach(questPlayer -> {
                        removeQuest(Bukkit.getOfflinePlayer(questPlayer.getUniqueId()), context);
                    });

                    context.sender().sendMessage(notQuests.parse("<success>Operation done!"));
                }));
    }

    private void removeQuest(OfflinePlayer offlinePlayer, NQCommandContext context) {
        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());

        if (questPlayer == null) {
            context.sender().sendMessage(notQuests.parse(
                    "<error>Error: QuestPlayer of Player <highlight>" + offlinePlayer.getName()+ "</highlight> not found."
            ));
            return;
        }
        final Quest quest = context.get("quest");
        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            if (activeQuest.getQuest().equals(quest)) {
                activeQuestsToRemove.add(activeQuest);
                context.sender().sendMessage(notQuests.parse("<success>Removed the quest as an active quest for the player with the UUID <highlight>"
                        + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                        + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                ));

            }
        }

        questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

        final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
            if (completedQuest.getQuest().equals(quest)) {
                completedQuestsToRemove.add(completedQuest);
                context.sender().sendMessage(notQuests.parse("<success>Removed the quest as a completed quest for the player with the UUID <highlight>"
                        + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                        + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                ));
            }

        }

        questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
    }
}
