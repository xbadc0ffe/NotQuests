package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class ConsumeItems {
    private ConsumeItems() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("ConsumeItems")
                .displayName("Consume Items")
                .description("Counts matching food, potions, or other consumable items used by the player.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Items or NotQuests custom items that count when consumed. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching items the player must consume. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.consumeItems.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%ITEMTOCONSUMETYPE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"),
                                        "%ITEMTOCONSUMENAME%",
                                        "",
                                        "%(%",
                                        "",
                                        "%)%",
                                        "")))
                .on(PlayerItemConsumeEvent.class, (event, objective) -> {
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null && itemStackSelection.checkIfIsIncluded(event.getItem())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
