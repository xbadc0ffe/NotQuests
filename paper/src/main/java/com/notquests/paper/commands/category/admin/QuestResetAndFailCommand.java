package com.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.BaseCommand;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandManager;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.CompletedQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

import static com.notquests.paper.commands.arguments.QuestArgument.questArgument;

public class QuestResetAndFailCommand extends BaseCommand {

    public QuestResetAndFailCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("Fails the quest from all players, removes it from completed quests, resets the accept cooldown and basically everything else."))
                .literal("resetAndFailQuestForAllPlayers", NQDescription.of("Resets and fails this quest for every player."))
                .required("quest", questArgument(notQuests), NQDescription.of("Name of the Quest which should be reset and failed."))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());

                    final Quest quest = context.get("quest");


                    for (final QuestPlayer questPlayer : notQuests.getQuestPlayerManager().getAllQuestPlayersForAllProfiles()) { //TODO: Doesn't include players which aren't loaded from the database
                        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            if (activeQuest.getQuest().equals(quest)) {
                                activeQuestsToRemove.add(activeQuest);
                            }
                        }

                        for (final ActiveQuest activeQuest : activeQuestsToRemove) {
                            questPlayer.failQuest(activeQuest);
                            context.sender().sendMessage(notQuests.parse("<success>Failed the quest as an active quest for the player with the UUID <highlight>"
                                    + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                                    + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                            ));

                        }

                        // questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

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
                    context.sender().sendMessage(notQuests.parse("<success>Operation done!"));

                }));
    }
}
