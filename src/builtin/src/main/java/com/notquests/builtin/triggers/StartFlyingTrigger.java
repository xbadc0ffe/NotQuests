package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

/**
 * FORK DIVERGENCE: fires when the quest player actually starts flying (creative-style flight
 * such as /fly). Fed by the platform's toggle-flight event, so elytra gliding never matches and
 * a merely granted-but-unused flight permission does not trigger it.
 */
public final class StartFlyingTrigger {
    private StartFlyingTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("STARTFLYING")
                .displayName("Start Flying")
                .description("Runs the selected action after the quest player starts flying the configured number of times.")
                .field(
                        "amount",
                        platform.fields().integer(1).config("amountNeeded"),
                        "Number of flight starts required before this trigger runs.")
                .register();
    }
}
