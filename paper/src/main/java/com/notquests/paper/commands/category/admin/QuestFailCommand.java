package com.notquests.paper.commands.category.admin;

import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.BaseCommand;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandManager;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

import static com.notquests.paper.commands.arguments.ActiveQuestArgument.activeQuestArgument;

public class QuestFailCommand extends BaseCommand {

    public QuestFailCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.literal("failQuest", NQDescription.of("Fails an active quest for a player.")).commandDescription(NQDescription.of("Fails an active quest for a player"))
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player name whose quest should be failed."))
                .required("activeQuest", activeQuestArgument(notQuests), NQDescription.of("Active quest which should be failed."))
                .handler((context) -> {
                    final Player player = context.get("player");
                    final ActiveQuest activeQuest = context.get("activeQuest");
                    final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                    if (questPlayer != null) {
                        questPlayer.failQuest(activeQuest);
                        context.sender().sendMessage(notQuests.parse(
                                "<main>The active quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> has been failed for player <highlight2>" + player.getName() + "</highlight2>!"
                        ));

                    } else {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Player <highlight>" + player.getName() + "</highlight> seems to not have accepted any quests!"
                        ));
                    }
                }));
    }
}
