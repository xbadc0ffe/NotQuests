package com.notquests.paper.objectives.support;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

public final class ItemObjectiveSupport {
    private ItemObjectiveSupport() {}

    public static int takenResultAmount(
            final NotQuests main,
            final Player player,
            final ItemStack currentItem,
            final ItemStack cursor,
            final ClickType click,
            final int hotbarButton) {
        int amount = currentItem.getAmount();

        switch (click) {
            case LEFT:
                if (!main.getUtilManager().isItemEmpty(cursor)
                        && (!cursor.isSimilar(currentItem)
                                || cursor.getAmount() + currentItem.getAmount() > cursor.getMaxStackSize())) {
                    amount = 0;
                }
                break;
            case RIGHT:
                if (!main.getUtilManager().isItemEmpty(cursor)
                        && (!cursor.isSimilar(currentItem)
                                || cursor.getAmount() + currentItem.getAmount() > cursor.getMaxStackSize())) {
                    amount = 0;
                }
                amount = (amount + 1) / 2;
                break;
            case NUMBER_KEY:
                if (player.getInventory().getItem(hotbarButton) != null) {
                    amount = 0;
                }
                break;
            case DROP:
                if (!main.getUtilManager().isItemEmpty(cursor)) {
                    amount = 0;
                }
                amount = 1;
                break;
            case CONTROL_DROP:
                if (!main.getUtilManager().isItemEmpty(cursor)) {
                    amount = 0;
                }
                break;
            case SWAP_OFFHAND:
                if (!main.getUtilManager().isItemEmpty(player.getInventory().getItemInOffHand())) {
                    amount = 0;
                }
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                if (amount != 0) {
                    amount = Math.min(inventorySpaceLeftForItem(player.getInventory(), currentItem), amount);
                }
                break;
            default:
                amount = 0;
        }

        return Math.max(amount, 0);
    }

    public static int craftAmount(
            final NotQuests main,
            final ItemStack result,
            final ItemStack cursor,
            final ClickType click,
            final HumanEntity whoClicked,
            final int hotbarButton,
            final CraftingInventory craftingInventory,
            final InventoryView inventoryView,
            final QuestPlayer questPlayer) {
        int recipeAmount = result.getAmount();

        switch (click) {
            case LEFT:
            case RIGHT:
                if (!main.getUtilManager().isItemEmpty(cursor)) {
                    questPlayer.sendDebugMessage("Inventory craft event: Cursor is not empty");
                    if (!cursor.isSimilar(result) || cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize()) {
                        recipeAmount = 0;
                    }
                }
                break;
            case NUMBER_KEY:
                if (whoClicked.getInventory().getItem(hotbarButton) != null) {
                    recipeAmount = 0;
                }
                break;
            case DROP:
            case CONTROL_DROP:
                if (!main.getUtilManager().isItemEmpty(cursor)) {
                    recipeAmount = 0;
                }
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                if (recipeAmount != 0) {
                    int maxCraftable = maxCraftAmount(craftingInventory);
                    final int capacity = fits(result, inventoryView.getBottomInventory());
                    if (capacity < maxCraftable) {
                        maxCraftable = ((capacity + recipeAmount - 1) / recipeAmount) * recipeAmount;
                    }
                    recipeAmount = maxCraftable;
                }
                break;
            case SWAP_OFFHAND:
                if (!main.getUtilManager().isItemEmpty(whoClicked.getInventory().getItemInOffHand())) {
                    recipeAmount = 0;
                }
                break;
            default:
                recipeAmount = 0;
        }

        return recipeAmount;
    }

    public static int inventorySpaceLeftForItem(final Inventory inventory, final ItemStack item) {
        int remaining = 0;
        for (final ItemStack itemStack : inventory.getStorageContents()) {
            if (itemStack == null || itemStack.getType().isAir()) {
                remaining += item.getMaxStackSize();
            } else if (itemStack.isSimilar(item)) {
                remaining += item.getMaxStackSize() - itemStack.getAmount();
            }
        }
        return remaining;
    }

    private static int maxCraftAmount(final CraftingInventory inventory) {
        if (inventory.getResult() == null) {
            return 0;
        }

        final int resultCount = inventory.getResult().getAmount();
        int materialCount = Integer.MAX_VALUE;
        for (final ItemStack stack : inventory.getMatrix()) {
            if (stack != null && stack.getAmount() < materialCount) {
                materialCount = stack.getAmount();
            }
        }
        return resultCount * materialCount;
    }

    private static int fits(final ItemStack stack, final Inventory inventory) {
        int result = 0;
        for (final ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null) {
                result += stack.getMaxStackSize();
            } else if (itemStack.isSimilar(stack)) {
                result += Math.max(stack.getMaxStackSize() - itemStack.getAmount(), 0);
            }
        }
        return result;
    }
}
