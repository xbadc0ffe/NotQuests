package com.notquests.paper.builtin.triggers;

import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.TriggerCatalog;

public final class Fail {
    private Fail() {}

    public static void register(final NotQuests main, final TriggerCatalog triggers) {
        triggers.trigger("FAIL")
                .displayName("Fail")
                .description("Runs the selected action when the quest fails.")
                .register();
    }
}
