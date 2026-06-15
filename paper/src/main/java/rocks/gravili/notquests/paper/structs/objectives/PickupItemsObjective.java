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

public class PickupItemsObjective extends Objective {

    private ItemStackSelection itemStackSelection;
    private boolean deductIfItemIsDropped = true;
    private boolean deductIfItemIsPlaced = true;
    private boolean deductIfItemIsRemovedFromInventory = true; //TODO: Implement


    public PickupItemsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("materials", itemStackSelectionArgument(main), NQDescription.of("Material of the item which needs to be collected"))
                .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of items which need to be collected"))
                .flag(NQFlag.builder("doNotDeductIfItemIsDropped", NQDescription.of("Makes it so Quest progress is NOT removed if the item is dropped.")).build())
                .flag(NQFlag.builder("doNotDeductIfItemIsPlaced", NQDescription.of("Makes it so Quest progress is NOT removed if the item is placed.")).build())
                .flag(NQFlag.builder("doNotDeductIfItemIsRemovedFromInventory", NQDescription.of("Makes it so Quest progress is NOT removed if the item is removed from inventory.")).build())
                .handler((context) -> {
                    final String amountExpression = context.get("amount");
                    final boolean deductIfItemIsDropped =
                            !context.flags().isPresent("doNotDeductIfItemIsDropped");
                    final boolean deductIfItemIsPlaced =
                            !context.flags().isPresent("doNotDeductIfItemIsPlaced");
                    final boolean deductIfItemIsRemovedFromInventory =
                            !context.flags().isPresent("doNotDeductIfItemIsRemovedFromInventory");

                    final ItemStackSelection itemStackSelection = context.get("materials");

                    PickupItemsObjective pickupItemsObjective = new PickupItemsObjective(main);
                    pickupItemsObjective.setItemStackSelection(itemStackSelection);

                    pickupItemsObjective.setProgressNeededExpression(amountExpression);
                    pickupItemsObjective.setDeductIfItemIsDropped(deductIfItemIsDropped);
                    pickupItemsObjective.setDeductIfItemIsPlaced(deductIfItemIsPlaced);
                    pickupItemsObjective.setDeductIfItemIsRemovedFromInventory(deductIfItemIsRemovedFromInventory);


                    main.getObjectiveManager().addObjective(pickupItemsObjective, context, level);
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
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.pickupItems.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%ITEMTOPICKUPTYPE%", getItemStackSelection().getAllMaterialsListedTranslated("main"),
                                "%ITEMTOPICKUPNAME%", "",
                                "%(%", "",
                                "%)%", ""));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        getItemStackSelection()
                .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

        configuration.set(initialPath + ".specifics.deductIfItemDropped", isDeductIfItemIsDropped());
        configuration.set(initialPath + ".specifics.deductIfItemPlaced", isDeductIfItemIsPlaced());
        configuration.set(initialPath + ".specifics.deductIfItemRemovedFromInventory", isDeductIfItemIsRemovedFromInventory());

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.itemStackSelection = new ItemStackSelection(main);
        itemStackSelection.loadFromFileConfiguration(
                configuration, initialPath + ".specifics.itemStackSelection");

        deductIfItemIsDropped =
                configuration.getBoolean(initialPath + ".specifics.deductIfItemDropped", true);
        deductIfItemIsPlaced =
                configuration.getBoolean(initialPath + ".specifics.deductIfItemPlaced", true);
        deductIfItemIsRemovedFromInventory =
                configuration.getBoolean(initialPath + ".specifics.deductIfItemRemovedFromInventory", true);
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

    public final boolean isDeductIfItemIsDropped() {
        return deductIfItemIsDropped;
    }

    public void setDeductIfItemIsDropped(final boolean deductIfItemIsDropped) {
        this.deductIfItemIsDropped = deductIfItemIsDropped;
    }

    public final boolean isDeductIfItemIsPlaced() {
        return deductIfItemIsPlaced;
    }

    public void setDeductIfItemIsPlaced(final boolean deductIfItemIsPlaced) {
        this.deductIfItemIsPlaced = deductIfItemIsPlaced;
    }

    public final boolean isDeductIfItemIsRemovedFromInventory() {
        return deductIfItemIsRemovedFromInventory;
    }

    public void setDeductIfItemIsRemovedFromInventory(final boolean deductIfItemIsRemovedFromInventory) {
        this.deductIfItemIsRemovedFromInventory = deductIfItemIsRemovedFromInventory;
    }
}
