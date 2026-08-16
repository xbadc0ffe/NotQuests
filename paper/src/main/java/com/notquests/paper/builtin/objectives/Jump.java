package com.notquests.paper.builtin.objectives;

import java.util.Map;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class Jump {
    private Jump() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("Jump")
                .displayName("Jump")
                .description("Counts times the player jumps.")
                .field("amount", FieldTypes.numberExpression().progressNeeded(), "Number of jumps required.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.jump.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%AMOUNTOFJUMPS%",
                                        ""
                                                + (activeObjective != null
                                                        ? activeObjective.getProgressNeeded()
                                                        : objective.text("amount")))))
                .on(PlayerJumpEvent.class, (event, objective) -> objective.addProgress(1))
                .register();
    }
}
