package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.objectives.support.ItemObjectiveSupport;

public final class TradeWithVillager {
    private TradeWithVillager() {}

    public static boolean countsTradeResult(final ItemStackSelection selection, final ItemStack item) {
        return selection != null && item != null && selection.checkIfIsIncluded(item);
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("TradeWithVillager")
                .displayName("Trade With Villager")
                .description("Counts matching result items taken from villager trade windows.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Villager trade result item that should count. Use any if every trade result should count.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of villager trade result items the player must receive. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.tradeWithVillager.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%ITEMTOTRADETYPE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"),
                                        "%ITEMTOTRADENAME%",
                                        "",
                                        "%(%",
                                        "",
                                        "%)%",
                                        "")))
                .on(InventoryClickEvent.class, event -> event.getWhoClicked() instanceof Player player ? player : null, (event, objective) -> {
                    if (!(event.getInventory() instanceof MerchantInventory) || event.getRawSlot() != 2) {
                        return;
                    }
                    final ItemStack currentItem = event.getCurrentItem();
                    if (main.getUtilManager().isItemEmpty(currentItem)) {
                        return;
                    }
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (!countsTradeResult(itemStackSelection, currentItem)) {
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
