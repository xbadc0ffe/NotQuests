package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class PickupItems {
    private PickupItems() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("PickupItems")
                .displayName("Pick Up Items")
                .description("Counts matching items picked up by the player.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Items or NotQuests custom items that count when picked up. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching items the player must pick up. Supports math and NotQuests number variables.")
                .flag(
                        "doNotDeductIfItemIsDropped",
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.deductIfItemDropped"),
                        "Stops NotQuests from removing progress when the player drops a matching item.")
                .flag(
                        "doNotDeductIfItemIsPlaced",
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.deductIfItemPlaced"),
                        "Stops NotQuests from removing progress when the player places a matching block item.")
                .flag(
                        "doNotDeductIfItemIsRemovedFromInventory",
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.deductIfItemRemovedFromInventory"),
                        "Reserved for inventory-removal deduction behavior. Currently kept for config compatibility.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.pickupItems.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%ITEMTOPICKUPTYPE%",
                                        objective.itemSelection("materials")
                                                .getAllMaterialsListedTranslated("main"),
                                        "%ITEMTOPICKUPNAME%",
                                        "",
                                        "%(%",
                                        "",
                                        "%)%",
                                        "")))
                .on(EntityPickupItemEvent.class, event -> event.getEntity() instanceof Player player ? player : null, (event, objective) -> {
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null && itemStackSelection.checkIfIsIncluded(event.getItem().getItemStack())) {
                        objective.addProgress(event.getItem().getItemStack().getAmount());
                    }
                })
                .on(PlayerDropItemEvent.class, (event, objective) -> {
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null
                            && itemStackSelection.checkIfIsIncluded(event.getItemDrop().getItemStack())
                            && !objective.flag("doNotDeductIfItemIsDropped")) {
                        objective.removeProgress(event.getItemDrop().getItemStack().getAmount(), false);
                    }
                })
                .on(BlockPlaceEvent.class, (event, objective) -> {
                    final ItemStackSelection itemStackSelection = objective.itemSelection("materials");
                    if (itemStackSelection != null
                            && itemStackSelection.checkIfIsIncluded(event.getBlock().getType())
                            && !objective.flag("doNotDeductIfItemIsPlaced")) {
                        objective.removeProgress(1, false);
                    }
                })
                .register();
    }
}
