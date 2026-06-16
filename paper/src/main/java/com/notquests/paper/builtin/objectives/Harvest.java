package com.notquests.paper.builtin.objectives;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockBreakEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class Harvest {
    private Harvest() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("Harvest")
                .displayName("Harvest")
                .description("Counts fully grown crops, plants, or fruit blocks harvested by the player.")
                .field(
                        "crops",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Crop, plant, or fruit block that must be harvested after it is fully grown. Crop item names like carrot or potato are accepted as aliases.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of fully grown crops, plants, or fruit blocks the player must harvest.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.harvest.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%CROPTOHARVEST%",
                                        objective.itemSelection("crops")
                                                .getAllMaterialsListedTranslated("main"))))
                .on(BlockBreakEvent.class, (event, objective) -> {
                    if (countsHarvest(
                            objective.itemSelection("crops"),
                            event.getBlock(),
                            main.getQuestEvents().isPlayerPlacedHarvestBlock(event.getBlock()))) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }

    public static boolean countsHarvest(
            final ItemStackSelection selection,
            final Block block,
            final boolean playerPlacedHarvestBlock) {
        return !playerPlacedHarvestBlock
                && isFullyGrownHarvestable(block)
                && isSelectedHarvestMaterial(selection, block.getType());
    }

    private static boolean isSelectedHarvestMaterial(final ItemStackSelection selection, final Material harvestedMaterial) {
        if (selection.checkIfIsIncluded(harvestedMaterial)) {
            return true;
        }
        for (final String alias : aliasesFor(harvestedMaterial)) {
            final Material aliasMaterial = Material.matchMaterial(alias);
            if (aliasMaterial != null && selection.checkIfIsIncluded(aliasMaterial)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFullyGrownHarvestable(final Block block) {
        final Material type = block.getType();
        if (isStackGrownPlant(type)) {
            return block.getRelative(BlockFace.DOWN).getType() == type;
        }
        if (type == Material.MELON || type == Material.PUMPKIN) {
            return true;
        }
        if (block.getBlockData() instanceof final Ageable ageable) {
            return !type.name().endsWith("_STEM") && ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    public static boolean shouldTrackAsPlayerPlacedHarvestBlock(final Block block) {
        final Material type = block.getType();
        return isStackGrownPlant(type)
                || type == Material.MELON
                || type == Material.PUMPKIN
                || (block.getBlockData() instanceof final Ageable ageable
                        && ageable.getAge() >= ageable.getMaximumAge());
    }

    private static boolean isStackGrownPlant(final Material type) {
        return type == Material.SUGAR_CANE || type == Material.CACTUS;
    }

    private static List<String> aliasesFor(final Material harvestedMaterial) {
        return switch (harvestedMaterial.name()) {
            case "CARROTS" -> List.of("CARROT");
            case "POTATOES" -> List.of("POTATO");
            case "BEETROOTS" -> List.of("BEETROOT");
            case "MELON" -> List.of("MELON_SLICE");
            case "SWEET_BERRY_BUSH" -> List.of("SWEET_BERRIES");
            case "COCOA" -> List.of("COCOA_BEANS");
            case "TORCHFLOWER_CROP" -> List.of("TORCHFLOWER", "TORCHFLOWER_SEEDS");
            case "PITCHER_CROP" -> List.of("PITCHER_PLANT", "PITCHER_POD");
            default -> List.of();
        };
    }
}
