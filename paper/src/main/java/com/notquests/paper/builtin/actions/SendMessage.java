package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.QuestPlayer;

public final class SendMessage {
    private static final String MESSAGE = "message";

    private SendMessage() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("SendMessage")
                .displayName("Send Message")
                .description("Sends a private chat message to the target player.")
                .field(
                        MESSAGE,
                        FieldTypes.greedyText().config("specifics.message"),
                        "Message sent privately to the target player. Supports MiniMessage formatting and NotQuests placeholders.")
                .singleLine((action, arguments) -> action.setValue(MESSAGE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String message = action.text(MESSAGE);
                    if (questPlayer == null || questPlayer.getPlayer() == null || message.isBlank()) {
                        main.getLogManager().warn("Tried to execute SendMessage action without a target player or message.");
                        return;
                    }
                    questPlayer.getPlayer().sendMessage(main.parse(resolve(main, action.action(), questPlayer, message, objects)));
                })
                .actionDescription((action, questPlayer, objects) -> "Sends message: " + action.text(MESSAGE))
                .register();
    }

    static String resolve(
            final NotQuests main,
            final com.notquests.paper.registry.DefinedAction action,
            final QuestPlayer questPlayer,
            final String text,
            final Object... objects) {
        return main.getUtilManager().applyPlaceholders(
                text, questPlayer.getPlayer(), questPlayer, action.getObjectiveHolder(), objects);
    }
}
