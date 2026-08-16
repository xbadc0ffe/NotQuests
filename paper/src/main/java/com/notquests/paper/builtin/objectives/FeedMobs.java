package com.notquests.paper.builtin.objectives;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class FeedMobs {
    private FeedMobs() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("FeedMobs")
                .displayName("Feed Mobs")
                .description("Counts matching mobs fed by the player.")
                .field("entityType", FieldTypes.entityType().config("specifics.mobToFeed"), "Entity type that must be fed, or any for all feedable mobs.")
                .field("amount", FieldTypes.numberExpression().progressNeeded(), "Number of matching feeding events required.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString("chat.objectives.taskDescription.feedMobs.base", questPlayer, activeObjective)
                        .replace("%ENTITYTOFEED%", objective.text("entityType")))
                .on(EntityEnterLoveModeEvent.class, event -> event.getHumanEntity() instanceof Player player ? player : null, (event, objective) -> {
                    final String entityType = objective.text("entityType");
                    if (entityType.equalsIgnoreCase("any") || entityType.equalsIgnoreCase(event.getEntityType().toString())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
