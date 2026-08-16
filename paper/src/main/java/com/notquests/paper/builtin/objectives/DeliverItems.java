package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import com.notquests.paper.commands.arguments.variables.NumberVariableArgument;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.npc.NQNPC;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveType;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.Quest;

import static com.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;
import static com.notquests.paper.commands.arguments.NQNPCArgument.nqNPCArgument;

public final class DeliverItems {
    private static final String TYPE = "DeliverItems";
    private static final String MATERIALS = "materials";
    private static final String AMOUNT = "amount";
    private static final String NPC = "npc";

    private DeliverItems() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective(TYPE)
                .displayName("Deliver Items")
                .description("Counts selected items delivered to a configured NPC.")
                .field(
                        MATERIALS,
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Material, custom item, hand item, any item, or comma-separated item list the player must deliver.")
                .field(
                        AMOUNT,
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Number of matching items the player must deliver.")
                .field(NPC, FieldTypes.npc().config("specifics.recipientNPC"), "NPC that receives the delivered items.")
                .commands((type, builder, level) -> registerCommands(main, type, builder, level))
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandBuilder builder,
            final int level) {
        main.getCommandManager().getNQCommandManager().command(builder
                .required(
                        MATERIALS,
                        itemStackSelectionArgument(main),
                        NQDescription.of("Material, custom item, hand item, any item, or comma-separated item list the player must deliver."))
                .required(
                        AMOUNT,
                        NumberVariableArgument.numberVariableArgument(AMOUNT, null, false),
                        NQDescription.of("Number of matching items the player must deliver."))
                .required(
                        NPC,
                        nqNPCArgument(main, false, true),
                        NQDescription.of("NPC that receives the delivered items. Use rightClickSelect to choose one in-game."))
                .handler(context -> addObjective(main, type, context, level)));
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level) {
        final ItemStackSelection itemStackSelection = context.get(MATERIALS);
        final String amount = context.get(AMOUNT);
        final NQNPCResult npcResult = context.get(NPC);

        if (npcResult.isRightClickSelect()) {
            if (!(context.sender() instanceof final Player player)) {
                context.sender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                return;
            }
            final Quest quest = context.get("quest");
            main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                    npc -> addObjective(main, type, context, level, itemStackSelection, amount, npc),
                    player,
                    "<success>You have been given an item with which you can add the DeliverItems Objective to an NPC by rightclicking the NPC. Check your inventory!",
                    "<LIGHT_PURPLE>Add DeliverItems Objective to NPC",
                    "<WHITE>Right-click an NPC to add the following objective to it:",
                    "<YELLOW>DeliverItems <WHITE>Objective of Quest <highlight>"
                            + quest.getIdentifier()
                            + "</highlight>.");
            return;
        }

        addObjective(main, type, context, level, itemStackSelection, amount, npcResult.getNQNPC());
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level,
            final ItemStackSelection itemStackSelection,
            final String amount,
            final NQNPC npc) {
        final DefinedObjective objective = type.createObjective();
        objective.setValue(MATERIALS, itemStackSelection);
        objective.setValue(AMOUNT, amount);
        objective.setProgressNeededExpression(amount);
        objective.setValue(NPC, npc);
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    public static boolean tryDeliverToNpc(
            final NotQuests main,
            final ActiveObjective activeObjective,
            final Player player,
            final NQNPC clickedNpc,
            final String npcName) {
        if (!(activeObjective.getObjective() instanceof final DefinedObjective objective) || !objective.isType(TYPE)) {
            return false;
        }
        if (!clickedNpc.equals(objective.value(NPC, NQNPC.class))) {
            return false;
        }
        final ItemStackSelection selection = objective.value(MATERIALS, ItemStackSelection.class);
        if (selection == null) {
            return false;
        }

        boolean handled = false;
        for (final ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || !selection.checkIfIsIncluded(itemStack)) {
                continue;
            }
            final double progressLeft = activeObjective.getProgressNeeded() - activeObjective.getCurrentProgress();
            if (progressLeft == 0) {
                continue;
            }
            handled = true;
            if (progressLeft < itemStack.getAmount()) {
                itemStack.setAmount(itemStack.getAmount() - (int) progressLeft);
                activeObjective.addProgress(progressLeft, clickedNpc);
                player.sendMessage(main.parse("<GREEN>You have delivered <highlight>"
                        + progressLeft
                        + "</highlight> items to <highlight>"
                        + npcName));
                break;
            }
            player.getInventory().removeItemAnySlot(itemStack);
            activeObjective.addProgress(itemStack.getAmount(), clickedNpc);
            player.sendMessage(main.parse("<GREEN>You have delivered <highlight>"
                    + itemStack.getAmount()
                    + "</highlight> items to <highlight>"
                    + npcName));
        }
        if (handled) {
            player.updateInventory();
        }
        return handled;
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final ActiveObjective activeObjective) {
        final ItemStackSelection selection = objective.itemSelection(MATERIALS);
        String description = main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.deliverItems.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%ITEMTODELIVERTYPE%",
                                selection == null ? "???" : selection.getAllMaterialsListedTranslated("main"),
                                "%ITEMTODELIVERNAME%",
                                "",
                                "%(%",
                                "",
                                "%)%",
                                ""));

        final NQNPC npc = objective.value(NPC, NQNPC.class);
        if (npc != null) {
            description += "\n"
                    + main.getLanguageManager()
                            .getString(
                                    "chat.objectives.taskDescription.deliverItems.deliver-to-npc",
                                    questPlayer,
                                    activeObjective,
                                    Map.of("%NPCNAME%", npc.getName() != null ? npc.getName() : npc.getID().getEitherAsString()));
        } else {
            description += "\n"
                    + main.getLanguageManager()
                            .getString(
                                    "chat.objectives.taskDescription.deliverItems.deliver-to-npc-not-available",
                                    questPlayer,
                                    activeObjective);
        }
        return description;
    }
}
