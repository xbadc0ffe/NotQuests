package com.notquests.paper.builtin.objectives;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTameEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class TameMobs {
    private TameMobs() {}

    public static boolean countsEntityType(final String configuredEntityType, final String actualEntityType) {
        return configuredEntityType != null
                && actualEntityType != null
                && (configuredEntityType.equalsIgnoreCase("any")
                        || configuredEntityType.equalsIgnoreCase(actualEntityType));
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("TameMobs")
                .displayName("Tame Mobs")
                .description("Counts matching mobs tamed by the player.")
                .field("entityType", FieldTypes.entityType().config("specifics.mobToTame"), "Entity type that must be tamed, or any for all tameable mobs.")
                .field("amount", FieldTypes.numberExpression().progressNeeded(), "Number of matching mobs the player must tame.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString("chat.objectives.taskDescription.tameMobs.base", questPlayer, activeObjective)
                        .replace("%ENTITYTOTAME%", objective.text("entityType")))
                .on(EntityTameEvent.class, event -> event.getOwner() instanceof Player player ? player : null, (event, objective) -> {
                    if (countsEntityType(objective.text("entityType"), event.getEntityType().toString())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
