package com.notquests.paper.builtin.actions;

import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.managers.items.NQItem;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class GiveItem {
    private static final String MATERIAL = "material";
    private static final String AMOUNT = "amount";

    private GiveItem() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("GiveItem")
                .displayName("Give Item")
                .description("Gives one or more materials or NotQuests custom items to the target player.")
                .field(
                        MATERIAL,
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Material, NotQuests custom item, hand item, or comma-separated item selection to give.")
                .field(
                        AMOUNT,
                        FieldTypes.integer(1).config("specifics.nqitemamount"),
                        "Amount of each selected item stack to give.")
                .singleLine((action, arguments) -> {
                    action.setValue(MATERIAL, parseSingleLineSelection(main, arguments));
                    action.setValue(AMOUNT, arguments.size() >= 2 ? Integer.parseInt(arguments.get(1)) : 1);
                })
                .execute((action, questPlayer, objects) -> {
                    final ItemStackSelection selection = action.itemSelection(MATERIAL);
                    if (questPlayer == null
                            || questPlayer.getPlayer() == null
                            || selection == null
                            || selection.isEmptyOrAny()) {
                        main.getLogManager().warn("Tried to execute GiveItem action with invalid target player or item selection.");
                        return;
                    }
                    final int amount = Math.max(1, action.integer(AMOUNT, 1));
                    final Runnable giveItems = () -> {
                        for (final ItemStack itemStack : selection.toItemStackList()) {
                            itemStack.setAmount(amount);
                            questPlayer.getPlayer().getInventory().addItem(itemStack);
                        }
                    };
                    if (Bukkit.isPrimaryThread()) {
                        giveItems.run();
                    } else {
                        Bukkit.getScheduler().runTask(main.getMain(), giveItems);
                    }
                })
                .actionDescription((action, questPlayer, objects) -> {
                    final ItemStackSelection selection = action.itemSelection(MATERIAL);
                    return "Gives item: " + (selection == null ? "" : selection.getAllMaterialsListedTranslated("main"));
                })
                .register();
    }

    private static ItemStackSelection parseSingleLineSelection(final NotQuests main, final java.util.List<String> arguments) {
        final ItemStackSelection selection = new ItemStackSelection(main);
        final String itemName = arguments.get(0);
        final NQItem nqItem = main.getItemsManager().getItem(itemName);
        if (nqItem != null) {
            selection.addNqItem(nqItem);
            return selection;
        }
        for (final String part : itemName.split(",")) {
            selection.addItemStack(new ItemStack(parseStoredMaterial(main, part)));
        }
        return selection;
    }

    private static Material parseStoredMaterial(final NotQuests main, final String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (final RuntimeException exception) {
            main.getLogManager().warn(
                    "Invalid GiveItem material '" + materialName
                            + "'. Falling back to STONE for this server version.");
            return Material.STONE;
        }
    }
}
