package com.notquests.paper.builtin.triggers;

import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.TriggerCatalog;

public final class Begin {
    private Begin() {}

    public static void register(final NotQuests main, final TriggerCatalog triggers) {
        triggers.trigger("BEGIN")
                .displayName("Begin")
                .description("Runs the selected action when the quest starts or an objective unlocks.")
                .register();
    }
}
