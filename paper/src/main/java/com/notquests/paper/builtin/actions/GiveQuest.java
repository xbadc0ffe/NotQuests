package com.notquests.paper.builtin.actions;

import java.util.Locale;
import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.Quest;

public final class GiveQuest {
    private static final String QUEST = "quest";
    private static final String FORCE_GIVE = "forceGive";

    private GiveQuest() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("GiveQuest")
                .displayName("Give Quest")
                .description("Gives another quest to the target player.")
                .field(
                        QUEST,
                        FieldTypes.questName().config("specifics.quest"),
                        "Quest that should be given to the target player.")
                .flag(
                        FORCE_GIVE,
                        FieldTypes.presenceFlag().config("specifics.forceGive"),
                        "Force-gives the quest and skips the normal take requirements and cooldown checks.")
                .singleLine((action, arguments) -> {
                    action.setValue(QUEST, arguments.get(0));
                    action.setValue(FORCE_GIVE, String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--forcegive"));
                })
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null) {
                        return;
                    }
                    final Quest quest = main.getQuestManager().getQuest(action.text(QUEST));
                    if (quest == null) {
                        main.getLogManager().warn("Tried to execute GiveQuest action with unknown quest: " + action.text(QUEST));
                        return;
                    }
                    if (action.flag(FORCE_GIVE)) {
                        main.getQuestPlayerManager().forceAcceptQuestSilent(questPlayer.getUniqueId(), quest);
                        return;
                    }
                    final String result = main.getQuestPlayerManager().acceptQuest(questPlayer, quest, true, true);
                    if (questPlayer.getPlayer() != null && result != null && !result.equals("accepted")) {
                        main.sendMessage(questPlayer.getPlayer(), result);
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Gives quest: " + action.text(QUEST))
                .register();
    }
}
