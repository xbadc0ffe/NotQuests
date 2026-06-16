package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class RunCommand {
    private RunCommand() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("RunCommand")
                .displayName("Run Command")
                .description("Counts when the player runs a matching command.")
                .field(
                        "amount",
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Number of matching command runs required.")
                .field(
                        "Command",
                        FieldTypes.commandText().config("specifics.commandToRun"),
                        "Command the player must run. The leading slash is optional when creating the objective.")
                .flag(
                        "ignoreCase",
                        FieldTypes.presenceFlag().config("specifics.ignoreCase"),
                        "Match the command even if the player uses different uppercase or lowercase letters.")
                .flag(
                        "cancelCommand",
                        FieldTypes.presenceFlag().config("specifics.cancelCommand"),
                        "Cancel the matching command instead of letting it run while this objective is active.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.runCommand.base",
                                questPlayer,
                                activeObjective,
                                Map.of("%COMMANDTORUN%", normalizeCommand(objective.text("Command")))))
                .on(PlayerCommandPreprocessEvent.class, (event, objective) -> {
                    final String expectedCommand = normalizeCommand(objective.text("Command"));
                    objective.questPlayer()
                            .sendDebugMessage("Found RunCommand Objective in PlayerCommandPreprocessEvent. Command: <highlight>"
                                    + event.getMessage()
                                    + "</highlight> Objective command to run: <highlight2>"
                                    + expectedCommand
                                    + "</highlight2>.");

                    final boolean matches = objective.flag("ignoreCase")
                            ? event.getMessage().equalsIgnoreCase(expectedCommand)
                            : event.getMessage().equals(expectedCommand);
                    if (!matches) {
                        return;
                    }

                    objective.addProgress(1);
                    if (objective.flag("cancelCommand")) {
                        event.setCancelled(true);
                    }
                })
                .register();
    }

    private static String normalizeCommand(final String command) {
        if (command == null || command.isBlank()) {
            return "";
        }
        return command.startsWith("/") ? command : "/" + command;
    }
}
