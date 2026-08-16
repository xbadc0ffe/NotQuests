package com.notquests.paper.builtin.actions;

import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class ShowActionBar {
    private static final String MESSAGE = "message";

    private ShowActionBar() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("ShowActionBar")
                .displayName("Show Action Bar")
                .description("Shows a short action-bar message above the target player's hotbar.")
                .field(
                        MESSAGE,
                        FieldTypes.greedyText().config("specifics.message"),
                        "Action-bar message shown above the target player's hotbar. Supports MiniMessage formatting and NotQuests placeholders.")
                .singleLine((action, arguments) -> action.setValue(MESSAGE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String message = action.text(MESSAGE);
                    if (questPlayer == null || questPlayer.getPlayer() == null || message.isBlank()) {
                        return;
                    }
                    questPlayer.getPlayer().sendActionBar(main.parse(SendMessage.resolve(
                            main, action.action(), questPlayer, message, objects)));
                })
                .actionDescription((action, questPlayer, objects) -> "Shows action bar: " + action.text(MESSAGE))
                .register();
    }
}
