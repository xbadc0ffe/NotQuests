package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.objectives.support.ItemObjectiveSupport;

public final class SmeltItems {
    private SmeltItems() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("SmeltItems")
                .displayName("Smelt Items")
                .description("Counts matching items taken from a furnace, blast furnace, or smoker output slot.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Items or NotQuests custom items that count when smelted. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching smelted items the player must take. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.smelt.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%ITEMTOSMELTTYPE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"),
                                        "%ITEMTOSMELTNAME%",
                                        "",
                                        "%(%",
                                        "",
                                        "%)%",
                                        "")))
                .on(InventoryClickEvent.class, event -> event.getWhoClicked() instanceof Player player ? player : null, (event, objective) -> {
                    final InventoryType inventoryType = event.getInventory().getType();
                    if (inventoryType != InventoryType.FURNACE
                            && inventoryType != InventoryType.BLAST_FURNACE
                            && inventoryType != InventoryType.SMOKER) {
                        return;
                    }
                    if (event.getRawSlot() != 2) {
                        return;
                    }
                    final ItemStack currentItem = event.getCurrentItem();
                    if (main.getUtilManager().isItemEmpty(currentItem)) {
                        return;
                    }
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection == null || !itemStackSelection.checkIfIsIncluded(currentItem)) {
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
