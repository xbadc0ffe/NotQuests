package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class PlaceBlocks {
    private PlaceBlocks() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("PlaceBlocks")
                .displayName("Place Blocks")
                .description("Counts matching blocks placed by the player.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Blocks or NotQuests custom items that count when placed. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching blocks the player must place. Supports math and NotQuests number variables.")
                .flag(
                        "doNotDeductIfBlockIsBroken",
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.deductIfBlockBroken"),
                        "Stops NotQuests from removing progress when the player breaks a matching placed block.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.placeBlocks.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%BLOCKTOPLACE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"))))
                .on(BlockPlaceEvent.class, (event, objective) -> {
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null && itemStackSelection.checkIfIsIncluded(event.getBlock().getType())) {
                        objective.addProgress(1);
                    }
                })
                .on(BlockBreakEvent.class, (event, objective) -> {
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null
                            && itemStackSelection.checkIfIsIncluded(event.getBlock().getType())
                            && !objective.flag("doNotDeductIfBlockIsBroken")) {
                        objective.removeProgress(1, false);
                    }
                })
                .register();
    }
}
