package com.notquests.paper.builtin.triggers;

import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.TriggerCatalog;

public final class Complete {
    private Complete() {}

    public static void register(final NotQuests main, final TriggerCatalog triggers) {
        triggers.trigger("COMPLETE")
                .displayName("Complete")
                .description("Runs the selected action when the quest or selected objective is completed.")
                .register();
    }
}
