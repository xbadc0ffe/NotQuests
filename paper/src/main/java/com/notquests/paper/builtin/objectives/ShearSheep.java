package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Sheep;
import org.bukkit.event.player.PlayerShearEntityEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class ShearSheep {
    private ShearSheep() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("ShearSheep")
                .displayName("Shear Sheep")
                .description("Counts sheep sheared by the player.")
                .field("amount", FieldTypes.numberExpression().progressNeeded(), "Number of sheep the player must shear.")
                .flag("cancelShearing", FieldTypes.presenceFlag().config("specifics.cancelShearing"), "Cancels the vanilla shearing interaction while this objective is active.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.shearSheep.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%AMOUNTOFSHEEP%",
                                        ""
                                                + (activeObjective != null
                                                        ? activeObjective.getProgressNeeded()
                                                        : objective.text("amount")))))
                .on(PlayerShearEntityEvent.class, (event, objective) -> {
                    if (event.getEntity() instanceof Sheep) {
                        objective.addProgress(1);
                        if (objective.flag("cancelShearing")) {
                            event.setCancelled(true);
                        }
                    }
                })
                .register();
    }
}
