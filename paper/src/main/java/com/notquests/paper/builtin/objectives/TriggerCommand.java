package com.notquests.paper.builtin.objectives;

import java.util.Map;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.objectives.Objective;

public final class TriggerCommand {
    private TriggerCommand() {}

    public static final String TYPE = "TriggerCommand";
    public static final String TRIGGER_NAME = "triggerName";

    public static boolean matches(final Objective objective, final String triggerName) {
        return objective instanceof DefinedObjective defined
                && defined.isType(TYPE)
                && defined.text(TRIGGER_NAME).equalsIgnoreCase(triggerName);
    }

    public static String triggerName(final Objective objective) {
        return objective instanceof DefinedObjective defined && defined.isType(TYPE)
                ? defined.text(TRIGGER_NAME)
                : "";
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective(TYPE)
                .displayName("Trigger Command")
                .description("Counts when a matching NotQuests trigger command is fired.")
                .field(
                        TRIGGER_NAME,
                        FieldTypes.text().config("specifics.triggerName"),
                        "Trigger command name that actions, BetonQuest events, or `/qa trigger` can fire.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of times this trigger command must be fired.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.triggerCommand.base",
                                questPlayer,
                                activeObjective,
                                Map.of("%TRIGGERNAME%", objective.text(TRIGGER_NAME))))
                .register();
    }
}
