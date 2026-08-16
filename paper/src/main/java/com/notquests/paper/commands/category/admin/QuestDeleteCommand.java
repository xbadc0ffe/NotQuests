package com.notquests.paper.commands.category.admin;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.BaseCommand;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandManager;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.structs.Quest;

import java.util.ArrayList;
import java.util.List;

import static com.notquests.paper.commands.arguments.QuestArgument.questArgument;

public class QuestDeleteCommand extends BaseCommand {
    public QuestDeleteCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.literal("delete", NQDescription.of("Delete an existing Quest."))
                .required("questName", questArgument(notQuests), NQDescription.of("Identifier of the quest to delete."), (context, input) -> {
                    final List<String> completions = new ArrayList<>();
                    for (final Quest quest : notQuests.getQuestManager().getAllQuests()) {
                        completions.add(quest.getIdentifier());
                    }
                    return completions;
                })
                .handler((context) -> {
                    final Quest quest = context.get("questName");
                    context.sender().sendMessage(notQuests.parse(notQuests.getQuestManager().deleteQuest(quest.getIdentifier())));
                }));
    }
}
