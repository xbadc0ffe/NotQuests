package com.notquests.paper.builtin.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class PlayerCommand {
    private static final String COMMAND = "command";

    private PlayerCommand() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("PlayerCommand")
                .displayName("Player Command")
                .description("Runs a command from the target player's perspective.")
                .field(
                        COMMAND,
                        FieldTypes.commandText().config("specifics.playerCommand"),
                        "Command executed by the target player. The leading slash is optional.")
                .singleLine((action, arguments) -> action.setValue(COMMAND, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String command = action.text(COMMAND);
                    final Player player = questPlayer == null ? null : questPlayer.getPlayer();
                    if (player == null || command.isBlank()) {
                        main.getLogManager().warn("Tried to execute PlayerCommand action without a target player or command.");
                        return;
                    }
                    final String resolved = ConsoleCommand.trimSlash(
                            SendMessage.resolve(main, action.action(), questPlayer, command, objects));
                    if (Bukkit.isPrimaryThread()) {
                        player.performCommand(resolved);
                    } else {
                        Bukkit.getScheduler().runTask(main.getMain(), () -> player.performCommand(resolved));
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Player command: " + action.text(COMMAND))
                .register();
    }
}
