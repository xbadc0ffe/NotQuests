package com.notquests.paper.builtin.objectives;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityBreedEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class BreedMobs {
    private BreedMobs() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("BreedMobs")
                .displayName("Breed Mobs")
                .description("Counts matching mobs bred by the player.")
                .field("entityType", FieldTypes.entityType().config("specifics.mobToBreed"), "Entity type that must be bred, or any for all breedable mobs.")
                .field("amount", FieldTypes.numberExpression().progressNeeded(), "Number of matching breeding events required.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString("chat.objectives.taskDescription.breed.base", questPlayer, activeObjective)
                        .replace("%ENTITYTOBREED%", objective.text("entityType")))
                .on(EntityBreedEvent.class, event -> event.getBreeder() instanceof Player player ? player : null, (event, objective) -> {
                    final String entityType = objective.text("entityType");
                    if (entityType.equalsIgnoreCase("any") || entityType.equalsIgnoreCase(event.getEntityType().toString())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
