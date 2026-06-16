package com.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArgumentType;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code QuestParser}: resolves a {@link Quest} by identifier. Reference
 * implementation for porting the remaining Cloud {@code ArgumentParser}s to {@link NQArgumentType}.
 */
public final class QuestArgument extends NQArgumentType<Quest> {
    private final NotQuests main;
    private final boolean takeEnabledOnly;

    public QuestArgument(final NotQuests main, final boolean takeEnabledOnly) {
        this.main = main;
        this.takeEnabledOnly = takeEnabledOnly;
    }

    public static QuestArgument questArgument(final NotQuests main, final boolean takeEnabledOnly) {
        return new QuestArgument(main, takeEnabledOnly);
    }

    public static QuestArgument questArgument(final NotQuests main) {
        return new QuestArgument(main, false);
    }

    @Override
    public String valueTypeName() {
        return "quest name";
    }

    @Override
    public Quest convert(final String input) throws CommandSyntaxException {
        final Quest foundQuest = main.getQuestManager().getQuest(input);
        if (foundQuest == null) {
            throw fail(
                    main.getLanguageManager()
                            .getString("chat.quest-does-not-exist", (QuestPlayer) null)
                            .replace("%QUESTNAME%", input));
        }
        if (takeEnabledOnly && !foundQuest.isTakeEnabled()) {
            throw fail(main.getLanguageManager().getString("chat.take-disabled", (QuestPlayer) null, foundQuest));
        }
        return foundQuest;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> names = new ArrayList<>();
        for (final Quest quest : main.getQuestManager().getAllQuests()) {
            if (!takeEnabledOnly || quest.isTakeEnabled()) {
                names.add(quest.getIdentifier());
            }
        }
        return names;
    }
}
