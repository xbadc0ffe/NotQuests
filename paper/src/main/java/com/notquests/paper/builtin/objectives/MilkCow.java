package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class MilkCow {
    private MilkCow() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("MilkCow")
                .displayName("Milk Cow")
                .description("Counts cows milked with an empty bucket.")
                .field("amount", FieldTypes.numberExpression().progressNeeded(), "Number of cows the player must milk.")
                .flag("cancelMilking", FieldTypes.presenceFlag().config("specifics.cancelMilking"), "Cancels the vanilla milking interaction while this objective is active.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.milkCow.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%AMOUNTOFCOWS%",
                                        ""
                                                + (activeObjective != null
                                                        ? activeObjective.getProgressNeeded()
                                                        : objective.text("amount")))))
                .on(PlayerInteractEntityEvent.class, (event, objective) -> {
                    if (!(event.getRightClicked() instanceof Cow)) {
                        return;
                    }
                    final ItemStack handItem = event.getPlayer().getInventory().getItem(event.getHand());
                    if (handItem == null || handItem.getType() != Material.BUCKET) {
                        return;
                    }
                    objective.addProgress(1);
                    if (objective.flag("cancelMilking")) {
                        event.setCancelled(true);
                    }
                })
                .register();
    }
}
