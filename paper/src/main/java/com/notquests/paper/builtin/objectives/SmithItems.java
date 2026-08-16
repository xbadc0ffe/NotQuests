package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.objectives.support.ItemObjectiveSupport;

public final class SmithItems {
    private SmithItems() {}

    public static boolean countsSmithingResult(final ItemStackSelection selection, final ItemStack item) {
        return selection != null && item != null && selection.checkIfIsIncluded(item);
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("SmithItems")
                .displayName("Smith Items")
                .description("Counts matching result items taken from a smithing table.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Smithing result item that should count. Use any if every smithing result should count.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of smithing result items the player must take. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.smithItems.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%ITEMTOSMITHTYPE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"),
                                        "%ITEMTOSMITHNAME%",
                                        "",
                                        "%(%",
                                        "",
                                        "%)%",
                                        "")))
                .on(SmithItemEvent.class, event -> event.getWhoClicked() instanceof Player player ? player : null, (event, objective) -> {
                    final ItemStack currentItem = event.getCurrentItem();
                    if (main.getUtilManager().isItemEmpty(currentItem)) {
                        return;
                    }
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (!countsSmithingResult(itemStackSelection, currentItem)) {
                        return;
                    }
                    final int amount = ItemObjectiveSupport.takenResultAmount(
                            main,
                            (Player) event.getWhoClicked(),
                            currentItem,
                            event.getCursor(),
                            event.getClick(),
                            event.getHotbarButton());
                    if (amount != 0) {
                        objective.addProgress(amount);
                    }
                })
                .register();
    }
}
