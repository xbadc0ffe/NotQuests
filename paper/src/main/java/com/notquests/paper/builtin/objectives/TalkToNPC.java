package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.NQNPCResult;
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

import static com.notquests.paper.commands.arguments.NQNPCArgument.nqNPCArgument;

public final class TalkToNPC {
    private static final String TYPE = "TalkToNPC";
    private static final String NPC = "npc";

    private TalkToNPC() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective(TYPE)
                .displayName("Talk to NPC")
                .description("Counts when the player right-clicks a configured NPC.")
                .field(NPC, FieldTypes.npc().config("specifics.npcToTalkTo"), "NPC the player must talk to.")
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
                        NPC,
                        nqNPCArgument(main, false, true),
                        NQDescription.of("NPC the player must talk to. Use rightClickSelect to choose one in-game."))
                .handler(context -> addObjective(main, type, context, level)));
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level) {
        final NQNPCResult npcResult = context.get(NPC);
        if (npcResult.isRightClickSelect()) {
            if (!(context.sender() instanceof final Player player)) {
                context.sender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                return;
            }
            final Quest quest = context.get("quest");
            main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                    npc -> addObjective(main, type, context, level, npc),
                    player,
                    "<success>You have been given an item with which you can add the TalkToNPC Objective to an NPC by rightclicking the NPC. Check your inventory!",
                    "<LIGHT_PURPLE>Add TalkToNPC Objective to NPC",
                    "<WHITE>Right-click an NPC to add the following objective to it:",
                    "<YELLOW>TalkToNPC <WHITE>Objective of Quest <highlight>"
                            + quest.getIdentifier()
                            + "</highlight>.");
            return;
        }

        addObjective(main, type, context, level, npcResult.getNQNPC());
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level,
            final NQNPC npc) {
        final DefinedObjective objective = type.createObjective();
        objective.setValue(NPC, npc);
        objective.setProgressNeededExpression("1");
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    public static boolean tryTalkToNpc(
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
        activeObjective.addProgress(1, clickedNpc);
        player.sendMessage(main.parse("<GREEN>You talked to <highlight>" + npcName));
        return true;
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final ActiveObjective activeObjective) {
        final NQNPC npc = objective.value(NPC, NQNPC.class);
        if (npc == null) {
            return main.getLanguageManager()
                    .getString("chat.objectives.taskDescription.talkToNPC.npc-not-available", questPlayer, activeObjective);
        }
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.talkToNPC.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%NAME%", npc.getName() != null ? npc.getName() : npc.getID().getEitherAsString()));
    }
}
