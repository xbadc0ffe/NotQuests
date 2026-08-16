package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.objectives.support.ItemObjectiveSupport;

import static com.notquests.paper.commands.NotQuestColors.debugHighlightGradient;

public final class CraftItems {
    private CraftItems() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("CraftItems")
                .displayName("Craft Items")
                .description("Counts matching items crafted by the player.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Items or NotQuests custom items that count when crafted. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching items the player must craft. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.craftItems.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%ITEMTOCRAFTTYPE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"),
                                        "%ITEMTOCRAFTNAME%",
                                        "",
                                        "%(%",
                                        "",
                                        "%)%",
                                        "")))
                .on(CraftItemEvent.class, event -> event.getWhoClicked() instanceof Player player ? player : null, (event, objective) -> {
                    if (event.getInventory().getResult() == null) {
                        return;
                    }
                    final ItemStack result = event.getRecipe().getResult();
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection == null || !itemStackSelection.checkIfIsIncluded(result)) {
                        return;
                    }
                    objective.questPlayer()
                            .sendDebugMessage("Inventory craft event. Click type: " + debugHighlightGradient + event.getClick().name() + "</gradient>");
                    final int craftedAmount = ItemObjectiveSupport.craftAmount(
                            main,
                            result,
                            event.getCursor(),
                            event.getClick(),
                            event.getWhoClicked(),
                            event.getHotbarButton(),
                            event.getInventory(),
                            event.getView(),
                            objective.questPlayer());
                    if (craftedAmount != 0) {
                        objective.addProgress(craftedAmount);
                    }
                })
                .register();
    }
}
