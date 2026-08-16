package com.notquests.paper.builtin.triggers;

import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.TriggerCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class Disconnect {
    private Disconnect() {}

    public static void register(final NotQuests main, final TriggerCatalog triggers) {
        triggers.trigger("DISCONNECT")
                .displayName("Disconnect")
                .description("Runs the selected action after the quest player disconnects the configured number of times.")
                .field(
                        "amount",
                        FieldTypes.integer(1).config("amountNeeded"),
                        "Number of disconnects required before this trigger runs.")
                .register();
    }
}
