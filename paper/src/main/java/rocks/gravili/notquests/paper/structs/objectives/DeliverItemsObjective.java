package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;
import static rocks.gravili.notquests.paper.commands.arguments.NQNPCArgument.nqNPCArgument;

public class DeliverItemsObjective extends Objective {

    private NQNPC recipientNPC;
    private ItemStackSelection itemStackSelection;



    //For Citizens NPCs
    public DeliverItemsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, NQCommandManager manager, NQCommandBuilder addObjectiveBuilder,
                                      final int level) {
        manager.command(addObjectiveBuilder
                        .required("materials", itemStackSelectionArgument(main), NQDescription.of("Material of the item which needs to be delivered"))
                        .required("amount", numberVariableArgument("amount", null, false), NQDescription.of("Amount of items which need to be delivered"))
                        .required("NPC", nqNPCArgument(main, false, true), NQDescription.of("NPC to whom the items should be delivered."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final String amountToDeliverExpression = context.get("amount");


                    final ItemStackSelection itemStackSelection = context.get("materials");


                    final NQNPCResult nqNPCResult = context.get("NPC");

                    if (nqNPCResult.isRightClickSelect()) {//Armor Stands
                        if (context.sender() instanceof final Player player) {
                            main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                                (nqNPC) -> {
                                    final DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main);
                                    deliverItemsObjective.setItemStackSelection(itemStackSelection);

                                    deliverItemsObjective.setProgressNeededExpression(amountToDeliverExpression);
                                    deliverItemsObjective.setRecipientNPC(nqNPC);

                                    main.getObjectiveManager().addObjective(deliverItemsObjective, context, level);
                                },
                                player,
                                "<success>You have been given an item with which you can add the DeliverItems Objective to an NPC by rightclicking the NPC. Check your inventory!",
                                "<LIGHT_PURPLE>Add DeliverItems Objective to NPC",
                                "<WHITE>Right-click an NPC to add the following objective to it:",
                                "<YELLOW>DeliverItems <WHITE>Objective of Quest <highlight>" + quest.getIdentifier()  + "</highlight>."
                            );

                        } else {
                            context.sender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                        }
                    }else {
                        final NQNPC nqNPC = nqNPCResult.getNQNPC();

                        final DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main);
                        deliverItemsObjective.setItemStackSelection(itemStackSelection);

                        deliverItemsObjective.setProgressNeededExpression(amountToDeliverExpression);
                        deliverItemsObjective.setRecipientNPC(nqNPC);

                        main.getObjectiveManager().addObjective(deliverItemsObjective, context, level);
                    }

                }));
    }

    public final ItemStackSelection getItemStackSelection(){
        return itemStackSelection;
    }

    public void setItemStackSelection(final ItemStackSelection itemStackSelection){
        this.itemStackSelection = itemStackSelection;
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

    public final NQNPC getRecipientNPC() {
        return recipientNPC;
    }

    public void setRecipientNPC(final NQNPC recipientNPC) {
        this.recipientNPC = recipientNPC;
    }

    @Override
    public String getTaskDescriptionInternal(final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        String toReturn;
        toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.base", questPlayer, activeObjective, Map.of(
                "%ITEMTODELIVERTYPE%", getItemStackSelection().getAllMaterialsListedTranslated("main"),
                "%ITEMTODELIVERNAME%", "",
                "%(%", "",
                "%)%", ""
        ));

        if (recipientNPC != null) {;
            toReturn += "\n" + main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-npc", questPlayer, activeObjective, Map.of(
                "%NPCNAME%", recipientNPC.getName() != null ? recipientNPC.getName() : recipientNPC.getID().getEitherAsString()
            ));
        } else {
            toReturn += "\n" + main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-npc-not-available", questPlayer, activeObjective);
        }
        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        getItemStackSelection().saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");
        recipientNPC.saveToConfig(configuration, initialPath + ".specifics.recipientNPC");
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.itemStackSelection = new ItemStackSelection(main);
        itemStackSelection.loadFromFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

        recipientNPC = NQNPC.fromConfig(main, configuration, initialPath + ".specifics.recipientNPC");
    }
}
