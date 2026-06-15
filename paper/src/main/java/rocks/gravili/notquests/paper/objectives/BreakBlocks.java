package rocks.gravili.notquests.paper.objectives;

import java.util.Map;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.managers.registering.ObjectiveManager;
import rocks.gravili.notquests.paper.registry.FieldTypes;

public final class BreakBlocks {
    private BreakBlocks() {}

    public static void register(final NotQuests main, final ObjectiveManager objectives) {
        objectives.objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Blocks or NotQuests custom items that count when broken. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching blocks the player must break. Supports math and NotQuests number variables.")
                .flag(
                        "doNotDeductIfBlockIsPlaced",
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.deductIfBlockPlaced"),
                        "Stops NotQuests from removing progress when the player places a matching block again.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.breakBlocks.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%BLOCKTOBREAK%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"))))
                .on(BlockBreakEvent.class, (event, objective) -> {
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null && itemStackSelection.checkIfIsIncluded(event.getBlock().getType())) {
                        objective.addProgress(1);
                    }
                })
                .on(BlockPlaceEvent.class, (event, objective) -> {
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null
                            && itemStackSelection.checkIfIsIncluded(event.getBlock().getType())
                            && !objective.flag("doNotDeductIfBlockIsPlaced")) {
                        objective.removeProgress(1, false);
                    }
                })
                .register();
    }
}
