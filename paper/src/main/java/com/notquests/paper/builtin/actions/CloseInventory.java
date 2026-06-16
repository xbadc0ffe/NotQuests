package com.notquests.paper.builtin.actions;

import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;

public final class CloseInventory {
    private CloseInventory() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("CloseInventory")
                .displayName("Close Inventory")
                .description("Closes the target player's currently open inventory.")
                .singleLine((action, arguments) -> {})
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer != null && questPlayer.getPlayer() != null) {
                        questPlayer.getPlayer().closeInventory();
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Closes the target player's inventory.")
                .register();
    }
}
