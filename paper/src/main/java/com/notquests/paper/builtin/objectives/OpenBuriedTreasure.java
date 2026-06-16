package com.notquests.paper.builtin.objectives;

import org.bukkit.block.Chest;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.loot.LootTables;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class OpenBuriedTreasure {
    private OpenBuriedTreasure() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("OpenBuriedTreasure")
                .displayName("Open Buried Treasure")
                .description("Counts unopened buried-treasure chests opened by the player.")
                .field("amount", FieldTypes.numberExpression().progressNeeded(), "Number of buried treasure chests the player must open.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString("chat.objectives.taskDescription.openBuriedTreasure.base", questPlayer, activeObjective))
                .on(PlayerInteractEvent.class, (event, objective) -> {
                    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
                        return;
                    }
                    if (event.getClickedBlock().getState() instanceof final Chest chest
                            && chest.getLootTable() != null
                            && chest.getLootTable().getKey().equals(LootTables.BURIED_TREASURE.getKey())
                            && !chest.hasPlayerLooted(event.getPlayer().getUniqueId())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
