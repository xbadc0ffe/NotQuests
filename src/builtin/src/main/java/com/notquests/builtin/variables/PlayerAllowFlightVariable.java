package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

/**
 * FORK DIVERGENCE: exposes the server-granted flight permission (the /fly state,
 * {@code Player#getAllowFlight()}), distinct from the Flying variable which reports whether the
 * player is actually airborne right now.
 */
public final class PlayerAllowFlightVariable {
    private PlayerAllowFlightVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().booleanVariable("AllowFlight")
                .displayName("Allow Flight")
                .description("Reads or changes whether the target player has flight enabled (for example via /fly).")
                .singular("Allow Flight")
                .plural("Allow Flight")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isFlightAllowed())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setFlightAllowed(Boolean.TRUE.equals(newValue)))
                .register();
    }
}
