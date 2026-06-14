package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class TradeWithVillagerObjective extends Objective {
    private ItemStackSelection itemStackSelection;

    public TradeWithVillagerObjective(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("materials", itemStackSelectionArgument(main), NQDescription.of("Trade result item that should count. Use 'any' if every villager trade result should count."))
                .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of traded result items the player needs to receive from villagers."))
                .handler(context -> {
                    final TradeWithVillagerObjective objective = new TradeWithVillagerObjective(main);
                    objective.setItemStackSelection(context.get("materials"));
                    objective.setProgressNeededExpression(context.get("amount"));
                    main.getObjectiveManager().addObjective(objective, context, level);
                }));
    }

    public ItemStackSelection getItemStackSelection() {
        return itemStackSelection;
    }

    public void setItemStackSelection(final ItemStackSelection itemStackSelection) {
        this.itemStackSelection = itemStackSelection;
    }

    public boolean countsTradeResult(final ItemStack itemStack) {
        return itemStackSelection.checkIfIsIncluded(itemStack);
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer,
            final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.tradeWithVillager.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%ITEMTOTRADETYPE%", getItemStackSelection().getAllMaterialsListedTranslated("main"),
                                "%ITEMTOTRADENAME%", "",
                                "%(%", "",
                                "%)%", ""));
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
