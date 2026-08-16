package com.notquests.paper.builtin.actions;

import org.bukkit.Bukkit;
import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class BroadcastMessage {
    private static final String MESSAGE = "message";

    private BroadcastMessage() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("BroadcastMessage")
                .displayName("Broadcast Message")
                .description("Broadcasts a chat message to every online player.")
                .field(
                        MESSAGE,
                        FieldTypes.greedyText().config("specifics.message"),
                        "Message broadcast to the whole server. Supports MiniMessage formatting and NotQuests placeholders.")
                .singleLine((action, arguments) -> action.setValue(MESSAGE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String message = action.text(MESSAGE);
                    if (message.isBlank()) {
                        main.getLogManager().warn("Tried to execute BroadcastMessage action with an empty message.");
                        return;
                    }
                    Bukkit.broadcast(main.parse(message));
                })
                .actionDescription((action, questPlayer, objects) -> "Broadcasts message: " + action.text(MESSAGE))
                .register();
    }
}
