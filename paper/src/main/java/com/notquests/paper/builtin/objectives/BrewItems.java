package com.notquests.paper.builtin.objectives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class BrewItems {
    private BrewItems() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        final Map<String, ArrayList<ItemStack>> freshlyBrewedItems = new HashMap<>();

        objectives.objective("BrewItems")
                .displayName("Brew Items")
                .description("Counts matching freshly brewed items taken from a brewing stand.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Potion, bottle, or custom brewed item the player must take after brewing completes.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of freshly brewed items the player must collect. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.brewItems.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%ITEMTOBREWTYPE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"),
                                        "%ITEMTOBREWNAME%",
                                        "",
                                        "%(%",
                                        "",
                                        "%)%",
                                        "")))
                .listen(BrewEvent.class, event -> {
                    final ArrayList<ItemStack> brewedResults = new ArrayList<>();
                    for (final ItemStack result : event.getResults()) {
                        if (!main.getUtilManager().isItemEmpty(result)) {
                            brewedResults.add(result.clone());
                        }
                    }
                    final String brewingStandKey = blockKey(event.getBlock());
                    if (brewedResults.isEmpty()) {
                        freshlyBrewedItems.remove(brewingStandKey);
                    } else {
                        freshlyBrewedItems.put(brewingStandKey, brewedResults);
                    }
                })
                .listen(BlockBreakEvent.class, event -> {
                    if (event.getBlock().getType().name().equals("BREWING_STAND")) {
                        freshlyBrewedItems.remove(blockKey(event.getBlock()));
                    }
                })
                .on(InventoryClickEvent.class, event -> event.getWhoClicked() instanceof Player player ? player : null, (event, objective) -> {
                    final ItemStack currentItem = event.getCurrentItem();
                    if (main.getUtilManager().isItemEmpty(currentItem)) {
                        return;
                    }
                    if (!(event.getClickedInventory() instanceof BrewerInventory brewerInventory)
                            || event.getSlot() < 0
                            || event.getSlot() > 2
                            || !(brewerInventory.getHolder() instanceof BrewingStand brewingStand)) {
                        return;
                    }
                    final int amount = consumeFreshBrewedAmount(freshlyBrewedItems, blockKey(brewingStand.getBlock()), currentItem);
                    if (amount == 0) {
                        return;
                    }
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null && itemStackSelection.checkIfIsIncluded(currentItem)) {
                        objective.addProgress(amount);
                    }
                })
                .register();
    }

    private static int consumeFreshBrewedAmount(
            final Map<String, ArrayList<ItemStack>> freshlyBrewedItems,
            final String brewingStandKey,
            final ItemStack takenItem) {
        final ArrayList<ItemStack> brewedItems = freshlyBrewedItems.get(brewingStandKey);
        if (brewedItems == null || brewedItems.isEmpty()) {
            return 0;
        }

        int remaining = takenItem.getAmount();
        int consumed = 0;
        for (int i = 0; i < brewedItems.size() && remaining > 0; i++) {
            final ItemStack brewedItem = brewedItems.get(i);
            if (!brewedItem.isSimilar(takenItem)) {
                continue;
            }
            final int take = Math.min(remaining, brewedItem.getAmount());
            remaining -= take;
            consumed += take;
            brewedItem.setAmount(brewedItem.getAmount() - take);
            if (brewedItem.getAmount() <= 0) {
                brewedItems.remove(i);
                i--;
            }
        }
        if (brewedItems.isEmpty()) {
            freshlyBrewedItems.remove(brewingStandKey);
        }
        return consumed;
    }

    public static boolean countsBrewedItem(final ItemStackSelection selection, final ItemStack item) {
        return selection != null && item != null && selection.checkIfIsIncluded(item);
    }

    private static String blockKey(final Block block) {
        return block.getWorld().getUID()
                + ":"
                + block.getX()
                + ":"
                + block.getY()
                + ":"
                + block.getZ();
    }
}
