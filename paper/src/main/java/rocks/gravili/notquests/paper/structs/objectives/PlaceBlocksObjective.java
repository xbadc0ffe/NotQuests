package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class PlaceBlocksObjective extends Objective {

    private ItemStackSelection itemStackSelection;

    private boolean deductIfBlockIsBroken = true;

    public PlaceBlocksObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("materials", itemStackSelectionArgument(main), NQDescription.of("Material of the block which needs to be placed"))
                .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of blocks which need to be placed"))
                .flag(NQFlag.builder("doNotDeductIfBlockIsBroken", NQDescription.of("Makes it so Quest progress is not removed if the block is broken")).build())
                .handler((context) -> {
                    final String amountExpression = context.get("amount");
                    final boolean deductIfBlockIsBroken =
                            !context.flags().isPresent("doNotDeductIfBlockIsBroken");

                    final ItemStackSelection itemStackSelection = context.get("materials");

                    PlaceBlocksObjective placeBlocksObjective = new PlaceBlocksObjective(main);
                    placeBlocksObjective.setItemStackSelection(itemStackSelection);

                    placeBlocksObjective.setDeductIfBlockIsBroken(deductIfBlockIsBroken);
                    placeBlocksObjective.setProgressNeededExpression(amountExpression);

                    main.getObjectiveManager().addObjective(placeBlocksObjective, context, level);
                }));
    }

    public final ItemStackSelection getItemStackSelection() {
        return itemStackSelection;
    }

    public void setItemStackSelection(final ItemStackSelection itemStackSelection) {
        this.itemStackSelection = itemStackSelection;
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager().getString(
                "chat.objectives.taskDescription.placeBlocks.base",
                questPlayer,
                activeObjective,
                Map.of("%BLOCKTOPLACE%", getItemStackSelection().getAllMaterialsListedTranslated("main")));
    }

    public void setDeductIfBlockIsBroken(final boolean deductIfBlockIsBroken) {
        this.deductIfBlockIsBroken = deductIfBlockIsBroken;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        getItemStackSelection()
                .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

        configuration.set(initialPath + ".specifics.deductIfBlockBroken", isDeductIfBlockBroken());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.itemStackSelection = new ItemStackSelection(main);
        itemStackSelection.loadFromFileConfiguration(
                configuration, initialPath + ".specifics.itemStackSelection");

        deductIfBlockIsBroken =
                configuration.getBoolean(initialPath + ".specifics.deductIfBlockBroken", true);
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

    public final boolean isDeductIfBlockBroken() {
        return deductIfBlockIsBroken;
    }
}
