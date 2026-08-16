package com.notquests.paper.events;


import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.conversation.ConversationLine;
import com.notquests.paper.conversation.ConversationPlayer;
import com.notquests.paper.builtin.objectives.Harvest;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.objectives.*;
import com.notquests.paper.triggers.ActiveTrigger;
import com.notquests.paper.registry.DefinedTrigger;
import com.notquests.paper.builtin.triggers.WorldEnter;
import com.notquests.paper.builtin.triggers.WorldLeave;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;



public class QuestEvents implements Listener {
    private final NotQuests main;

    private final Set<String> playerPlacedHarvestBlocks;

    int beaconCounter = 0;
    int objectiveUnlockConditionCheckCounter = 0;
    int conditionObjectiveCounter = 0;


    public QuestEvents(NotQuests main) {
        this.main = main;
        playerPlacedHarvestBlocks = new HashSet<>();


        Bukkit.getScheduler().scheduleSyncRepeatingTask(main.getMain(), () -> { //Main Loop
            if(main.getDataManager().isDisabled()){
                return;
            }
            final boolean updateBeacons;
            beaconCounter++;
            if(beaconCounter >= 4){
                beaconCounter = 0;
                updateBeacons = true;
            }
            else{
                updateBeacons = false;
            }

            final boolean updateConditionObjectives;
            conditionObjectiveCounter++;
            if(conditionObjectiveCounter >= 2){
                updateConditionObjectives = true;
                conditionObjectiveCounter = 0;
            }
            else{
                updateConditionObjectives = false;
            }

            final boolean updateObjectiveUnlockConditions;
            if(main.getConfiguration().getObjectiveUnlockConditionsCheckRegularInterval() > 0) {
                objectiveUnlockConditionCheckCounter++;
                if(objectiveUnlockConditionCheckCounter >= main.getConfiguration().getObjectiveUnlockConditionsCheckRegularInterval()){
                    objectiveUnlockConditionCheckCounter = 0;
                    updateObjectiveUnlockConditions = true;
                } else {
                    updateObjectiveUnlockConditions = false;
                }
            }else {
                updateObjectiveUnlockConditions = false;
            }



            for(final Player player : Bukkit.getOnlinePlayers()) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if(questPlayer == null){
                    continue;
                }

                if(questPlayer.getBossBar() != null){
                    questPlayer.increaseBossBarTimeByOneSecond();
                }

                if(updateBeacons){
                    questPlayer.updateBeaconLocations(player);
                }
                questPlayer.updateLocationCompass(player);


                if(updateConditionObjectives){
                    questPlayer.updateConditionObjectives(player);
                }




                // Check unlock objectives
                if(updateObjectiveUnlockConditions) {
                    for(final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        activeQuest.updateObjectivesUnlocked(true, true);
                    }
                }



            }

        }, 0L, 20L); //0 Tick initial delay, 20 Tick (1 Second) between repeats

    }


    @EventHandler
    private void onChunkLoad(PlayerChunkLoadEvent e){
        if(main.getDataManager().isDisabled()){
            return;
        }
        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
        if(questPlayer == null){
            return;
        }

        //final Location playerLocation = player.getLocation();
        //int maxDistance = 110;

        if (!questPlayer.getLocationsAndBeacons().isEmpty()) {
            questPlayer.updateBeaconLocations(player, true);
        }
    }




    @EventHandler
    public void onPlayerConsumeItem(PlayerItemConsumeEvent e) {
        if (main.getConversationManager() == null)
            return;
        final ConversationPlayer currentOpenConversationPlayer = main.getConversationManager().getOpenConversation(e.getPlayer().getUniqueId());
        if (currentOpenConversationPlayer != null)
            e.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interactEvent(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
        if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
            return;
        }
        if (main.getConversationManager() != null) {
            final ConversationPlayer currentOpenConversationPlayer = main.getConversationManager().getOpenConversation(player.getUniqueId());
            if (currentOpenConversationPlayer != null) {
                e.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void playerChangeWorldEvent(PlayerChangedWorldEvent e) {
        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
        if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
            return;
        }
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

            for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                if (activeTrigger.getTrigger() instanceof final DefinedTrigger trigger
                        && trigger.definition().id().equals("WORLDENTER")) {
                    if (e.getPlayer().getWorld().getName().equals(trigger.text(WorldEnter.WORLD))) {
                        handleGeneralTrigger(questPlayer, activeTrigger);

                    }

                } else if (activeTrigger.getTrigger() instanceof final DefinedTrigger trigger
                        && trigger.definition().id().equals("WORLDLEAVE")) {
                    if (e.getFrom().getName().equals(trigger.text(WorldLeave.WORLD))) {
                        handleGeneralTrigger(questPlayer, activeTrigger);
                    }

                }
            }


        }
    }


    public boolean isPlayerPlacedHarvestBlock(final Block block) {
        return playerPlacedHarvestBlocks.contains(blockKey(block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent e) {
        playerPlacedHarvestBlocks.remove(blockKey(e.getBlock()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockPlace(BlockPlaceEvent e) {
        if (!e.isCancelled()) {
            if (Harvest.shouldTrackAsPlayerPlacedHarvestBlock(e.getBlock())) {
                playerPlacedHarvestBlocks.add(blockKey(e.getBlock()));
            }
        }

    }

    private static String blockKey(final Block block) {
        return block.getWorld().getUID()
                + ":"
                + block.getX()
                + ":"
                + block.getY()
                + ":"
                + block.getZ();
    }

    @EventHandler
    private void onEntityDeath(EntityDeathEvent e) { //KillMobs objectives & Death triggers

        //Death Triggers
        if (e.getEntity() instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());

            if (questPlayer != null && !questPlayer.getActiveQuests().isEmpty()) {
                for (int i = 0; i < questPlayer.getActiveQuests().size(); i++) {
                    final ActiveQuest activeQuest = questPlayer.getActiveQuests().get(i);
                    for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        if (activeTrigger.getTrigger().getTriggerType().equals("DEATH")) {
                            handleGeneralTrigger(questPlayer, activeTrigger);

                        }
                    }
                }

            }

            //Iterator<ActiveQuest> iter = questPlayer.getActiveQuests().iterator(); //Why was that needed?
        }

    }

    /**
     * This method handles the most commonly used type of trigger, which should simply add to the progress.
     * Apart from adding the progress, this method checks for the triggers applyOn and the triggers worldName
     *
     * @param questPlayer   is the QuestPlayer object, used to check the world of the player
     * @param activeTrigger is the trigger which we need in order to add progress to it
     */
    public void handleGeneralTrigger(final QuestPlayer questPlayer, final ActiveTrigger activeTrigger) {

        //Handle Trigger applyOn
        if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Trigger applies to a specific objective of the Quest and not the Quest itself
            //Get the active Objective for which the trigger applies to
            final ActiveObjective activeObjective = activeTrigger.getActiveQuest().getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
            //Return, if the active objective which the trigger needs doesn't exist or is not yet unlocked (so hidden)
            if (activeObjective == null || !activeObjective.isUnlocked()) {
                return;
            }
        }

        //Handle Trigger World Name
        if (!activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
            final Player qPlayer = Bukkit.getPlayer(questPlayer.getUniqueId());
            //If the player is not in the world which the Trigger needs, cancel.
            if (qPlayer == null || !qPlayer.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                return;
            }
        }

        //Finally, we can add to the trigger and check if it can trigger now if the progress is full
        activeTrigger.addAndCheckTrigger(activeTrigger.getActiveQuest());


    }

    @EventHandler(priority = EventPriority.MONITOR)
    protected void onPluginEnable(final PluginEnableEvent event) {
        main.getIntegrationsManager().onPluginEnable(event);
    }


    private final boolean handleConversation(final Player player, final int optionNumber) {
        if(main.getConversationManager() == null){
            return false;
        }
        final int optionIndex = optionNumber-1;
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
        if (main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId()) == null) {
            return false;
        }
        //Check if the player has an open conversation
        final ConversationPlayer conversationPlayer = main.getConversationManager().getOpenConversation(player.getUniqueId());
        if (conversationPlayer != null) {
            if(optionIndex < 0 || optionIndex >= conversationPlayer.getCurrentPlayerLines().size()){
                return false;
            }
            final ConversationLine foundCurrentPlayerLine = conversationPlayer.getCurrentPlayerLines().get(optionIndex);
            if(foundCurrentPlayerLine != null){
                conversationPlayer.chooseOption(foundCurrentPlayerLine);
                return true;
            }
        } else if(questPlayer != null) {
            questPlayer.sendDebugMessage("Tried to choose conversation option, but the conversationPlayer was not found! Active conversationPlayers count: <highlight>" + main.getConversationManager().getOpenConversations().size());
            questPlayer.sendDebugMessage("All active conversationPlayers: <highlight>" + main.getConversationManager().getOpenConversations().toString());
            questPlayer.sendDebugMessage("Current QuestPlayer Object: <highlight>" + questPlayer);
            questPlayer.sendDebugMessage("Current QuestPlayer: <highlight>" + questPlayer.getPlayer().getName());
        }
        return false;
    }

    @EventHandler
    private void onDisconnectEvent(PlayerQuitEvent e) { //Disconnect objectives
        if(main.getConfiguration().isSavePlayerDataOnQuit()){
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                    main.getQuestPlayerManager().saveSinglePlayerData(e.getPlayer());
                });
            }else{
                main.getQuestPlayerManager().saveSinglePlayerData(e.getPlayer());
            }
        }else{
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(e.getPlayer().getUniqueId());
            if (questPlayer != null) {
                if (Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                        questPlayer.onQuitAsync(e.getPlayer());
                    });
                    questPlayer.onQuit(e.getPlayer());
                }else{
                    Bukkit.getScheduler().runTask(main.getMain(), () -> {
                        questPlayer.onQuit(e.getPlayer());
                    });
                    questPlayer.onQuitAsync(e.getPlayer());
                }
            }
        }

    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if(main.getConfiguration().isLoadPlayerDataOnJoin()){
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                    main.getQuestPlayerManager().loadSinglePlayerData(e.getPlayer().getUniqueId());
                });
            }else{
                main.getQuestPlayerManager().loadSinglePlayerData(e.getPlayer().getUniqueId());
            }

            //no need to call onJoin here as it's called by loadSinglePlayerData automatically
        }else{
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(e.getPlayer().getUniqueId());

            if (questPlayer != null) {
                Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                    questPlayer.onJoinAsync(e.getPlayer());
                });
                questPlayer.onJoin(e.getPlayer());
            }
        }



    }


    @EventHandler
    public void asyncChatEvent(AsyncChatEvent e) {
        final Player playerWhoChatted = e.getPlayer();

        final Player player = e.getPlayer();

        if(main.getConversationManager() != null && main.getConfiguration().isConversationAllowAnswerNumberInChat()){
            final ConversationPlayer conversationPlayer = main.getConversationManager().getOpenConversation(player.getUniqueId());
            if(conversationPlayer != null){
                if (player.hasPermission("notquests.use")) {
                    final String plainMessage = PlainTextComponentSerializer.plainText().serialize(e.message());
                    try{
                        int parsed = Integer.parseInt(plainMessage.replace(".", ""));
                        if(handleConversation(player, parsed)){
                            e.setCancelled(true);
                            return;
                        }
                    }catch (Exception ignored){
                    }
                }
            }
        }


        for(final Audience audience : e.viewers()){
            if(audience instanceof final Player playerViewer){
                final Component adventureComponent = e.renderer().render(
                    playerWhoChatted,
                    playerWhoChatted.displayName(),
                    e.message(),
                    audience
                );

                main.getConversationManager()
                    .rememberNonConversationChatMessage(playerViewer.getUniqueId(), adventureComponent);
            }
        }



    }

}
