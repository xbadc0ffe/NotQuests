package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;
import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class HarvestObjective extends Objective {
    private ItemStackSelection itemStackSelection;

    public HarvestObjective(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(
                addObjectiveBuilder
                        .required(
                                "crops",
                                itemStackSelectionArgument(main),
                                NQDescription.of("Crop, plant, or fruit block that must be harvested after it is fully grown. Supports crop item names like carrot or potato as aliases."))
                        .required(
                                "amount",
                                numberVariableArgument("amount", null),
                                NQDescription.of("Amount of fully grown crops, plants, or fruit blocks the player must harvest."))
                        .handler(
                                context -> {
                                    final HarvestObjective harvestObjective = new HarvestObjective(main);
                                    harvestObjective.setItemStackSelection(context.get("crops"));
                                    harvestObjective.setProgressNeededExpression(context.get("amount"));
                                    main.getObjectiveManager().addObjective(harvestObjective, context, level);
                                }));
    }

    public ItemStackSelection getItemStackSelection() {
        return itemStackSelection;
    }

    public void setItemStackSelection(final ItemStackSelection itemStackSelection) {
        this.itemStackSelection = itemStackSelection;
    }

    public boolean countsHarvest(final Block block, final boolean playerPlacedHarvestBlock) {
        return !playerPlacedHarvestBlock
                && isFullyGrownHarvestable(block)
                && isSelectedHarvestMaterial(block.getType());
    }

    private boolean isSelectedHarvestMaterial(final Material harvestedMaterial) {
        if (itemStackSelection.checkIfIsIncluded(harvestedMaterial)) {
            return true;
        }
        for (final String alias : aliasesFor(harvestedMaterial)) {
            final Material aliasMaterial = Material.matchMaterial(alias);
            if (aliasMaterial != null && itemStackSelection.checkIfIsIncluded(aliasMaterial)) {
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

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.harvest.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%CROPTOHARVEST%", getItemStackSelection().getAllMaterialsListedTranslated("main")));
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        getItemStackSelection()
                .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        itemStackSelection = new ItemStackSelection(main);
        itemStackSelection.loadFromFileConfiguration(
                configuration, initialPath + ".specifics.itemStackSelection");
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }
}
