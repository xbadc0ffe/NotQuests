package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerFishEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class FishItems {
    private FishItems() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("FishItems")
                .displayName("Fish Items")
                .description("Counts matching items caught by fishing.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Items or NotQuests custom items that count when fished. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching items the player must fish. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.fishItems.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%ITEMTOFISHTYPE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"),
                                        "%ITEMTOFISHNAME%",
                                        "",
                                        "%(%",
                                        "",
                                        "%)%",
                                        "")))
                .on(PlayerFishEvent.class, (event, objective) -> {
                    if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) {
                        return;
                    }
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null && itemStackSelection.checkIfIsIncluded(item.getItemStack())) {
                        objective.addProgress(item.getItemStack().getAmount());
                    }
                })
                .register();
    }
}
