package com.notquests.paper.builtin.objectives;

import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.ActiveObjective;

public final class SlimefunResearch {
    private static final String TYPE = "SlimefunResearch";

    private SlimefunResearch() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        if (!main.getIntegrationsManager().isSlimefunEnabled()) {
            return;
        }
        objectives.objective(TYPE)
                .displayName("Slimefun Research")
                .description("Counts Slimefun research cost spent by the player.")
                .field(
                        "amount",
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Total Slimefun research cost the player must spend.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString("chat.objectives.taskDescription.SlimefunResearch.base", questPlayer, activeObjective))
                .register();
    }

    public static boolean isSlimefunResearch(final ActiveObjective activeObjective) {
        return activeObjective.getObjective() instanceof final DefinedObjective objective && objective.isType(TYPE);
    }
}
