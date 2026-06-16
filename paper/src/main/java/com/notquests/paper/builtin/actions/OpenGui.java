package com.notquests.paper.builtin.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.gui.GuiContext;
import com.notquests.paper.managers.FlagParser;
import com.notquests.paper.managers.npc.NQNPCID;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class OpenGui {
    private static final String GUI_NAME = "guiName";
    private static final String PLAYER = "player";
    private static final String QUEST = "quest";
    private static final String NPC = "npc";
    private static final String CATEGORY = "category";

    private OpenGui() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("OpenGui")
                .displayName("Open GUI")
                .description("Opens a NotQuests GUI for the target player.")
                .field(GUI_NAME, FieldTypes.text().config("specifics.guiName"), "Name of the NotQuests GUI file to open.")
                .flag(PLAYER, FieldTypes.text(), "Optional player name who should see the GUI.")
                .flag(QUEST, FieldTypes.text(), "Optional quest identifier used as GUI context.")
                .flag(NPC, FieldTypes.integer(-1), "Optional Citizens NPC id used as GUI context.")
                .flag(CATEGORY, FieldTypes.text(), "Optional quest category identifier used as GUI context.")
                .singleLine((action, arguments) -> {
                    action.setValue(GUI_NAME, arguments.get(0));
                    final var flags = FlagParser.parseFlags(String.join(" ", arguments));
                    action.setValue(PLAYER, flags.getOrDefault(PLAYER, ""));
                    action.setValue(QUEST, flags.getOrDefault(QUEST, ""));
                    final Object npc = flags.get(NPC);
                    action.setValue(NPC, npc == null ? -1 : parseNpcId(main, npc.toString()));
                    action.setValue(CATEGORY, flags.getOrDefault(CATEGORY, ""));
                })
                .execute((action, questPlayer, objects) -> {
                    final Player player = targetPlayer(action.text(PLAYER), questPlayer);
                    if (player == null) {
                        main.getLogManager().warn("Tried to execute OpenGui action without a valid target player.");
                        return;
                    }
                    final GuiContext context = guiContext(main, action, player);
                    final Runnable open = () -> main.getGuiService().showGui(action.text(GUI_NAME), player, context);
                    if (Bukkit.isPrimaryThread()) {
                        open.run();
                    } else {
                        Bukkit.getScheduler().runTask(main.getMain(), open);
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Opens GUI: " + action.text(GUI_NAME))
                .register();
    }

    private static Player targetPlayer(
            final String targetPlayerName, final com.notquests.paper.structs.QuestPlayer questPlayer) {
        if (targetPlayerName != null && !targetPlayerName.isBlank()) {
            return Bukkit.getPlayerExact(targetPlayerName);
        }
        return questPlayer == null ? null : questPlayer.getPlayer();
    }

    private static GuiContext guiContext(
            final NotQuests main,
            final com.notquests.paper.registry.ActionDataContext action,
            final Player player) {
        final GuiContext context = new GuiContext();
        context.setPlayer(player);
        if (!action.text(QUEST).isBlank()) {
            context.setQuest(main.getQuestManager().getQuest(action.text(QUEST)));
        }
        if (!action.text(CATEGORY).isBlank()) {
            context.setCategory(main.getDataManager().getCategory(action.text(CATEGORY)));
        }
        final int npcId = action.integer(NPC, -1);
        if (npcId >= 0) {
            context.setNqnpc(main.getNPCManager().getOrCreateNQNpc("Citizens", NQNPCID.fromInteger(npcId)));
        }
        return context;
    }

    private static int parseNpcId(final NotQuests main, final String value) {
        if (value.contains("%")) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException exception) {
            main.getLogManager().debug("OpenGui action NPC id '" + value + "' is not a Citizens integer id; skipping NPC context.");
            return -1;
        }
    }
}
