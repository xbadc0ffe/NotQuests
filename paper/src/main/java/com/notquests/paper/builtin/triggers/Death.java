package com.notquests.paper.builtin.triggers;

import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.TriggerCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class Death {
    private Death() {}

    public static void register(final NotQuests main, final TriggerCatalog triggers) {
        triggers.trigger("DEATH")
                .displayName("Death")
                .description("Runs the selected action after the quest player dies the configured number of times.")
                .field(
                        "amount",
                        FieldTypes.integer(1).config("amountNeeded"),
                        "Number of player deaths required before this trigger runs.")
                .register();
    }
}
