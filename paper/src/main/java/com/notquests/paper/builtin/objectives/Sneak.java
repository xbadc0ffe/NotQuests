package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class Sneak {
    private Sneak() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("Sneak")
                .displayName("Sneak")
                .description("Counts times the player starts sneaking.")
                .field("amount", FieldTypes.numberExpression().progressNeeded(), "Number of sneaks required.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.sneak.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%AMOUNTOFSNEAKS%",
                                        ""
                                                + (activeObjective != null
                                                        ? activeObjective.getProgressNeeded()
                                                        : objective.text("amount")))))
                .on(PlayerToggleSneakEvent.class, (event, objective) -> {
                    if (event.isSneaking()) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
