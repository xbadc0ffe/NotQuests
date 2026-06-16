package com.notquests.paper.events.hooks;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.notquests.paper.NotQuests;
import com.notquests.paper.conversation.Conversation;
import com.notquests.paper.conversation.ConversationManager;
import com.notquests.paper.managers.npc.ConversationFocus;
import com.notquests.paper.managers.npc.NQNPC;
import com.notquests.paper.managers.npc.NQNPCID;
import com.notquests.paper.builtin.objectives.DeliverItems;
import com.notquests.paper.builtin.objectives.EscortNPC;
import com.notquests.paper.builtin.objectives.TalkToNPC;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.triggers.ActiveTrigger;
import com.notquests.paper.registry.DefinedTrigger;
import com.notquests.paper.builtin.triggers.NPCDeath;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class CitizensEvents implements Listener {
    private final NotQuests main;

    public CitizensEvents(final NotQuests main) {
        this.main = main;
        main.getLogManager().info("Initialized CitizensEvents");
    }


    @EventHandler
    private void onNPCDeathEvent(NPCDeathEvent event) {
        final NPC npc = event.getNPC();

        for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getActiveQuestPlayers()) {
            if (!questPlayer.getActiveQuests().isEmpty()) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        if (activeTrigger.getTrigger() instanceof final DefinedTrigger trigger
                                && trigger.definition().id().equals("NPCDEATH")) {
                            if (trigger.value(NPCDeath.NPC, -1) == npc.getId()) {
                                if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not Objective

                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        final Player player = Bukkit.getPlayer(questPlayer.getUniqueId());
                                        if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        }
                                    }
                                }

                            } else if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                                final ActiveObjective activeObjective = activeQuest.getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
                                if (activeObjective != null && activeObjective.isUnlocked()) {

                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        final Player player = Bukkit.getPlayer(questPlayer.getUniqueId());
                                        if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        }
                                    }


                                }

                            }


                        }
                    }


                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onNPCClickEvent(NPCRightClickEvent event) { //Disconnect objectives
        final NPC npc = event.getNPC();
        final NQNPC nqNPC = main.getNPCManager().getOrCreateNQNpc("Citizens", NQNPCID.fromInteger(npc.getId()));
        final Player player = event.getClicker();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

        if(nqNPC == null){
            questPlayer.sendDebugMessage("Error: NQNpc is null");
            return;
        }


        questPlayer.sendDebugMessage("Clicked on Citizens NPC!");
        //Handle special items first
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (player.hasPermission("notquests.admin.armorstandeditingitems") && heldItem.getType() != Material.AIR && heldItem.getItemMeta() != null) {
            final PersistentDataContainer container = heldItem.getItemMeta().getPersistentDataContainer();

            final NamespacedKey specialActionItemKey = new NamespacedKey(main.getMain(), "notquests-nqnpc-selector-with-action");

            if (container.has(specialActionItemKey, PersistentDataType.INTEGER)) {
                int id = container.get(specialActionItemKey, PersistentDataType.INTEGER); //Not null, because we check for it in container.has()

                main.getNPCManager().executeNPCSelectionAction(nqNPC, id);
            }
        }




        final AtomicBoolean handledObjective = new AtomicBoolean(false);
        questPlayer.sendDebugMessage("Right-clicked NPC event: " + npc.getId() + "." );
        final String clickedNpcName = main.getMiniMessage().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(npc.getName().replace("§","&")));

        questPlayer.queueObjectiveCheck(activeObjective -> {
            if (DeliverItems.tryDeliverToNpc(main, activeObjective, player, nqNPC, clickedNpcName)) {
                handledObjective.set(true);
            }
        });
        questPlayer.queueObjectiveCheck(activeObjective -> {
            if (TalkToNPC.tryTalkToNpc(main, activeObjective, player, nqNPC, clickedNpcName)) {
                handledObjective.set(true);
            }
        });
        questPlayer.queueObjectiveCheck(activeObjective -> {
            if (EscortNPC.tryCompleteAtDestination(main, activeObjective, player, npc.getId(), nqNPC)) {
                handledObjective.set(true);
            }
        });
        questPlayer.queueObjectiveCheck(activeObjective -> {
            //Eventually trigger CompletionNPC Objective Completion if the objective is not set to complete automatically (so, if getCompletionNPCID() is not -1)
            if (activeObjective.getObjective().getCompletionNPC() != null) {
                activeObjective.addProgress(0, nqNPC);
            }
        });
        questPlayer.checkQueuedObjectives();


        //Return if another action already happened
        if (handledObjective.get()) {
            questPlayer.sendDebugMessage("Returning because of handled objective");
            return;
        }

        //Quest Preview
        main.getQuestManager().sendQuestsPreviewOfQuestShownNPCs(nqNPC, questPlayer);

        //Conversations
        ConversationManager manager = main.getConversationManager();
        if(manager != null){
            final Conversation foundConversation = main.getConversationManager().getConversationForNPC(nqNPC);
            if (foundConversation != null) {
                // Cancel NPC's movement
                npc.getNavigator().cancelNavigation();
                npc.getNavigator().setPaused(true);
                manager.getActiveConversationsOfNPCWithPlayerCache().putIfAbsent(npc.getId(), new ArrayList<>());
                manager.getActiveConversationsOfNPCWithPlayerCache().get(npc.getId()).add(player.getUniqueId());
                new BukkitRunnable(){
                    public void run() {
                        if (!manager.getActiveConversationsOfNPCWithPlayerCache().containsKey(nqNPC.getID().getIntegerID())) {
                            npc.getNavigator().setPaused(false);
                            this.cancel();
                        }
                    }
                }.runTaskTimer(this.main.getMain(), 0L, 30L);
                // Try to cancel player's movement
                player.getLocation().setDirection(new Vector(0, 0, 0));
                player.setVelocity(new Vector(0, 0, 0));
                manager.playConversation(questPlayer, foundConversation, nqNPC);
                if (main.getDataManager().getConfiguration().isCitizensFocusingEnabled())
                    new ConversationFocus(main, player, npc.getEntity(), foundConversation).runTaskTimer(main.getMain(), 0, 2);
            }
        }
    }

    @EventHandler
    private void onCitizensEnable(CitizensEnableEvent e) {
        main.getLogManager().info("Processing Citizens Enable Event...");
        main.getIntegrationsManager().getCitizensManager().registerQuestGiverTrait();


    }

    @EventHandler
    private void onCitizensReload(CitizensReloadEvent e) {
        main.getLogManager().info("Processing Citizens Reload Event...");

        main.getIntegrationsManager().getCitizensManager().registerQuestGiverTrait();

    }
}
