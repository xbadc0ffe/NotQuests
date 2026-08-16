package com.notquests.paper.builtin.actions;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class ConsoleCommand {
    private static final String COMMAND = "command";

    private ConsoleCommand() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("ConsoleCommand")
                .displayName("Console Command")
                .description("Runs a server command from the console.")
                .field(
                        COMMAND,
                        FieldTypes.commandText().config("specifics.consoleCommand"),
                        "Command executed by the server console. The leading slash is optional.")
                .singleLine((action, arguments) -> action.setValue(COMMAND, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String command = action.text(COMMAND);
                    if (questPlayer == null || questPlayer.getPlayer() == null || command.isBlank()) {
                        main.getLogManager().warn("Tried to execute ConsoleCommand action without a target player or command.");
                        return;
                    }
                    final String resolved = SendMessage.resolve(main, action.action(), questPlayer, command, objects);
                    final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                    if (Bukkit.isPrimaryThread()) {
                        Bukkit.dispatchCommand(console, trimSlash(resolved));
                    } else {
                        Bukkit.getScheduler().runTask(main.getMain(), () -> Bukkit.dispatchCommand(console, trimSlash(resolved)));
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Console command: " + action.text(COMMAND))
                .register();
    }

    static String trimSlash(final String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }
}
