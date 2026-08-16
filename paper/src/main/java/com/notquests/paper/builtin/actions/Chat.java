package com.notquests.paper.builtin.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class Chat {
    private static final String MESSAGE = "message";

    private Chat() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("Chat")
                .displayName("Chat")
                .description("Makes the target player send a chat message.")
                .field(
                        MESSAGE,
                        FieldTypes.text().config("specifics.chatMessage"),
                        "Chat message sent by the target player. Wrap it in quotes when using spaces in commands.")
                .singleLine((action, arguments) -> action.setValue(MESSAGE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final Player player = questPlayer == null ? null : questPlayer.getPlayer();
                    final String message = action.text(MESSAGE);
                    if (player == null || message.isBlank()) {
                        main.getLogManager().warn("Tried to execute Chat action without a target player or message.");
                        return;
                    }
                    final String resolved = SendMessage.resolve(main, action.action(), questPlayer, message, objects);
                    if (Bukkit.isPrimaryThread()) {
                        player.chat(resolved);
                    } else {
                        Bukkit.getScheduler().runTask(main.getMain(), () -> player.chat(resolved));
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Player chat message: " + action.text(MESSAGE))
                .register();
    }
}
