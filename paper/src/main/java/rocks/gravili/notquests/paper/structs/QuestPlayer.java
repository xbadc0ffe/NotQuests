/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.structs;


import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.NotQuestColors;
import rocks.gravili.notquests.paper.events.notquests.QuestCompletedEvent;
import rocks.gravili.notquests.paper.events.notquests.QuestFinishAcceptEvent;
import rocks.gravili.notquests.paper.events.notquests.QuestPointsChangeEvent;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.Condition.ConditionResult;
import rocks.gravili.notquests.paper.structs.objectives.ConditionObjective;
import rocks.gravili.notquests.paper.structs.objectives.NumberVariableObjective;
import rocks.gravili.notquests.paper.structs.objectives.OtherQuestObjective;
import rocks.gravili.notquests.paper.structs.triggers.ActiveTrigger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The QuestPlayer Object is initialized for every player, once they join the server - loading its data from the database.
 * It contains all kinds of player data for that player, like active quests, completed quests and quest points.
 * Completed Quests are saved too, for things like Quest History (Future feature), or handling the maxAccepts per quest /
 * the quest cooldown.
 *
 * @author Alessio Gravili
 */
public class QuestPlayer {

    private final String profile;
    static final int BEAM_MAX_DISTANCE = 88;
    static final int END_GATEWAY_BEAM_MIN_Y = 192;

    private final NotQuests main;

    private final UUID uuid;


    private final CopyOnWriteArrayList<ActiveQuest> activeQuests;
    // Thread-safe: these are iterated by the async save while the main thread may add to them
    // (quest complete/fail), which would otherwise throw ConcurrentModificationException mid-save.
    private final CopyOnWriteArrayList<ActiveQuest> questsToComplete;
    private final CopyOnWriteArrayList<ActiveQuest> questsToRemove;
    private final CopyOnWriteArrayList<CompletedQuest> completedQuests; //has to accept multiple entries of the same value

    private final CopyOnWriteArrayList<FailedQuest> failedQuests; //has to accept multiple entries of the same value

    private final HashMap<String, Location> locationsAndBeacons, activeLocationAndBeams;
    //Tags — ConcurrentHashMap: mutated on the main thread (gameplay) AND iterated by the async
    //tag save/load. setTagValue treats a null value as "remove" (ConcurrentHashMap forbids nulls).
    private final Map<String, Object> tags;
    private long questPoints;
    private ActiveObjective trackingObjective;
    private BossBar bossBar;
    private BossBar locationCompassBossBar;
    private int lastBossBarActiveTimeInSeconds = 0;
    private boolean hasActiveConditionObjectives = false;
    private boolean hasActiveVariableObjectives = false;

    private Player player;

    private boolean currentlyLoading = true;



    private boolean finishedLoadingGeneralData = false;
    private boolean finishedLoadingTags = false;

    private final ArrayList<Consumer<ActiveObjective>> queuedObjectivesToCheck = new ArrayList<>();


    public QuestPlayer(final NotQuests main, final UUID uuid, final String profile) {
        this.main = main;
        this.uuid = uuid;
        this.profile = profile;

        activeQuests = new CopyOnWriteArrayList<>();
        questsToComplete = new CopyOnWriteArrayList<>();
        questsToRemove = new CopyOnWriteArrayList<>();
        completedQuests = new CopyOnWriteArrayList<>();
        failedQuests = new CopyOnWriteArrayList<>();

        locationsAndBeacons = new HashMap<>();
        activeLocationAndBeams = new HashMap<>();

        tags = new ConcurrentHashMap<>();
    }

    public final String getProfile(){
        return profile;
    }

    public boolean isHasActiveConditionObjectives(){
        return hasActiveConditionObjectives;
    }

    public boolean isHasActiveVariableObjectives(){
        return hasActiveVariableObjectives;
    }

    public void setHasActiveConditionObjectives(final boolean hasActiveConditionObjectives){
        this.hasActiveConditionObjectives = hasActiveConditionObjectives;
    }

    public void setHasActiveVariableObjectives(final boolean hasActiveVariableObjectives){
        this.hasActiveVariableObjectives = hasActiveVariableObjectives;
    }

    public final Object getTagValue(final String tagIdentifier) {
        return tags.get(tagIdentifier.toLowerCase(Locale.ROOT));
    }

    public void setTagValue(final String tagIdentifier, final Object newValue) {
        final String key = tagIdentifier.toLowerCase(Locale.ROOT);
        if (newValue == null) {
            tags.remove(key); // a null value means "remove the tag" (and ConcurrentHashMap forbids null values)
        } else {
            tags.put(key, newValue);
        }
    }

    public final Map<String, Object> getTags(){
        return tags;
    }

    public ActiveObjective getTrackingObjective() {
        return trackingObjective;
    }

    public void setTrackingObjective(ActiveObjective trackingObjective) {
        this.trackingObjective = trackingObjective;
        sendObjectiveProgress(trackingObjective);
        if(trackingObjective.getObjective().isShowLocation() && trackingObjective.getObjective().getLocation() != null){
            trackBeacon(trackingObjective.getObjectiveID() + "", trackingObjective.getObjective().getLocation());
        }
    }

    public void trackBeacon(final String name, final Location location) {
        clearBeacons();
        if (name == null || location == null) {
            return;
        }
        getLocationsAndBeacons().put(name, location.clone());
        updateBeaconLocations(getPlayer(), true);
    }

    public void showTemporaryBeacon(final String name, final Location location, final long durationTicks) {
        final Player player = getPlayer();
        if (player == null || name == null || location == null) {
            return;
        }

        final Map<String, Location> previousLocations = cloneLocations(getLocationsAndBeacons());
        clearBeacons();
        getLocationsAndBeacons().put(name, location.clone());
        updateBeaconLocations(player, true);

        Bukkit.getScheduler().runTaskLater(main.getMain(), () -> {
            if (getLocationsAndBeacons().size() != 1 || !getLocationsAndBeacons().containsKey(name)) {
                return;
            }
            clearBeacons();
            getLocationsAndBeacons().putAll(cloneLocations(previousLocations));
            updateBeaconLocations(getPlayer(), true);
        }, durationTicks);
    }

    public void disableTrackingObjective(ActiveObjective activeObjective) {
        if(getTrackingObjective() != null && getTrackingObjective().equals(activeObjective)){
            //getPlayer().sendMessage("Removing 1!");
            clearBeacons();
        }
    }

    public HashMap<String, Location> getLocationsAndBeacons(){
        return locationsAndBeacons;
    }

    public HashMap<String, Location> getActiveLocationsAndBeacons(){
        return activeLocationAndBeams;
    }

    public void clearBeacons(){
        clearActiveBeacons();
        getLocationsAndBeacons().clear();
        hideLocationCompass(getPlayer());
    }

    public void clearActiveBeacons(){
        clearActiveBeacons(getPlayer());
    }

    private void clearActiveBeacons(final Player player) {
        if (player != null) {
            for(Location location : getActiveLocationsAndBeacons().values()){
                scheduleBeaconRemovalAt(location, player);
            }
        }

        getActiveLocationsAndBeacons().clear();
    }

    public void scheduleBeaconRemovalAt(final Location location, final Player player){
        if (player == null || location == null || location.getWorld() == null) {
            return;
        }

        if(isBeaconMode()){
            sendRealBlock(player, location);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    sendRealBlock(player, location.clone().add(x, -1, z));
                }
            }
            return;
        }

        sendRealBlock(player, location);
    }

    public final boolean updateBeaconLocations(final Player player){
        return updateBeaconLocations(player, false);
    }

    public void updateLocationCompass(final Player player) {
        if (!main.getConfiguration().isVisualObjectiveTrackingLocationCompassEnabled()) {
            hideLocationCompass(player);
            return;
        }
        if (player == null || locationsAndBeacons.isEmpty()) {
            hideLocationCompass(player);
            return;
        }

        Location target = null;
        for (final Location location : locationsAndBeacons.values()) {
            if (location != null) {
                target = location;
                break;
            }
        }
        if (target == null || target.getWorld() == null || player.getWorld() == null) {
            hideLocationCompass(player);
            return;
        }

        if (!target.getWorld().getUID().equals(player.getWorld().getUID())) {
            showLocationCompass(
                    player,
                    main.parse("<warn>Objective marker is in world <highlight>" + target.getWorld().getName() + "</highlight>"),
                    0.0f,
                    BossBar.Color.RED);
            return;
        }

        final Location playerLocation = player.getLocation();
        final double distance = target.distance(playerLocation);
        final double directionDelta = wrappedDegrees(yawTo(playerLocation, target) - playerLocation.getYaw());
        final String direction = compassDirection(directionDelta);
        final String objectiveLabel =
                trackingObjective == null ? "Objective Marker" : trackingObjective.getObjective().getDisplayNameOrIdentifier();
        showLocationCompass(
                player,
                main.parse("<highlight>" + direction + "</highlight> <positive>" + Math.round(distance)
                        + "m</positive> <unimportant>-</unimportant> <main>" + objectiveLabel + "</main>"),
                compassProgress(directionDelta),
                compassColor(directionDelta));
    }

    public void hideLocationCompass(final Player player) {
        if (locationCompassBossBar == null) {
            return;
        }
        if (player != null) {
            player.hideBossBar(locationCompassBossBar);
        }
        locationCompassBossBar = null;
    }

    public final boolean updateBeaconLocations(final Player player, final boolean force){

        boolean toReturn = false;
        if(player == null){
            getActiveLocationsAndBeacons().clear();
            return false;
        }
        if(locationsAndBeacons.isEmpty()){
            clearActiveBeacons(player);
            //player.sendMessage("Nothing to process!");
            return false;
        }
        final Location playerLocation = player.getLocation();
        final Set<String> renderedLocations = new HashSet<>();
        for(final String locationName : locationsAndBeacons.keySet()){
            sendDebugMessage("Processing " + locationName);

            final Location finalLocation = locationsAndBeacons.get(locationName);

            if(finalLocation == null || finalLocation.getWorld() == null || !finalLocation.getWorld().getUID().equals(player.getWorld().getUID())){
                continue;
            }
            Location lowestDistanceLocation = playerLocation.clone();

            if(finalLocation.distanceSquared(playerLocation) > BEAM_MAX_DISTANCE * BEAM_MAX_DISTANCE){


                //New Beacon Location should be cur player location + maxDistance blocks in direction of newChunkLocation - playerLocation
                org.bukkit.util.Vector normalizedDistanceBetweenPlayerAndNewChunk = finalLocation.toVector().subtract(playerLocation.toVector()).normalize();
                lowestDistanceLocation = playerLocation.clone().add(normalizedDistanceBetweenPlayerAndNewChunk.multiply(BEAM_MAX_DISTANCE));
                if(isBeaconMode()){
                    lowestDistanceLocation.setY(lowestDistanceLocation.getWorld().getHighestBlockYAt(lowestDistanceLocation.getBlockX(), lowestDistanceLocation.getBlockZ()));
                }else{
                    if(playerLocation.getY() > END_GATEWAY_BEAM_MIN_Y){
                        lowestDistanceLocation.setY(playerLocation.getY());
                    }else {
                        lowestDistanceLocation.setY(END_GATEWAY_BEAM_MIN_Y);
                    }
                }


            }else{
                lowestDistanceLocation = finalLocation.clone();

                toReturn = true;
            }




            lowestDistanceLocation = blockLocation(lowestDistanceLocation);
            final Location activeLocation = activeLocationAndBeams.get(locationName);
            if (!force && sameBlockLocation(activeLocation, lowestDistanceLocation)) {
                renderedLocations.add(locationName);
                continue;
            }
            if (activeLocation != null) {
                scheduleBeaconRemovalAt(activeLocation, player);
            }

            if(isBeaconMode()){
                BlockState beaconBlockState = lowestDistanceLocation.getBlock().getState();
                beaconBlockState.setType(Material.BEACON);

                BlockState ironBlockState = lowestDistanceLocation.getBlock().getState();
                ironBlockState.setType(Material.IRON_BLOCK);

                player.sendBlockChange(lowestDistanceLocation, beaconBlockState.getBlockData());
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        player.sendBlockChange(lowestDistanceLocation.clone().add(x, -1, z), ironBlockState.getBlockData());
                    }
                }

                activeLocationAndBeams.put(locationName, lowestDistanceLocation.clone());
            }else{
                BlockState beaconBlockState = lowestDistanceLocation.getBlock().getState();
                beaconBlockState.setType(Material.END_GATEWAY);

                player.sendBlockChange(lowestDistanceLocation, beaconBlockState.getBlockData());

                activeLocationAndBeams.put(locationName, lowestDistanceLocation.clone());
            }

            renderedLocations.add(locationName);

        }

        final Iterator<Map.Entry<String, Location>> activeIterator = activeLocationAndBeams.entrySet().iterator();
        while (activeIterator.hasNext()) {
            final Map.Entry<String, Location> activeEntry = activeIterator.next();
            if (!renderedLocations.contains(activeEntry.getKey())) {
                scheduleBeaconRemovalAt(activeEntry.getValue(), player);
                activeIterator.remove();
            }
        }

        return toReturn;


    }

    static boolean sameBlockLocation(final Location first, final Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().getUID().equals(second.getWorld().getUID())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private static Location blockLocation(final Location location) {
        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private static Map<String, Location> cloneLocations(final Map<String, Location> locations) {
        final Map<String, Location> clones = new HashMap<>();
        for (final Map.Entry<String, Location> entry : locations.entrySet()) {
            if (entry.getValue() != null) {
                clones.put(entry.getKey(), entry.getValue().clone());
            }
        }
        return clones;
    }

    static double yawTo(final Location from, final Location to) {
        final double dx = to.getX() - from.getX();
        final double dz = to.getZ() - from.getZ();
        final double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        return Math.abs(yaw) < 0.000001 ? 0.0 : yaw;
    }

    static double wrappedDegrees(final double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        }
        if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    static String compassDirection(final double directionDelta) {
        final double absolute = Math.abs(directionDelta);
        if (absolute <= 12.0) {
            return "^";
        }
        if (absolute >= 165.0) {
            return "behind";
        }
        if (directionDelta < 0.0) {
            return absolute >= 90.0 ? "<<" : "<";
        }
        return absolute >= 90.0 ? ">>" : ">";
    }

    static float compassProgress(final double directionDelta) {
        return (float) Math.max(0.0, 1.0 - (Math.abs(directionDelta) / 180.0));
    }

    static BossBar.Color compassColor(final double directionDelta) {
        final double absolute = Math.abs(directionDelta);
        if (absolute <= 15.0) {
            return BossBar.Color.GREEN;
        }
        if (absolute <= 75.0) {
            return BossBar.Color.YELLOW;
        }
        return BossBar.Color.RED;
    }

    private void showLocationCompass(
            final Player player,
            final Component title,
            final float progress,
            final BossBar.Color color) {
        if (locationCompassBossBar == null) {
            locationCompassBossBar = BossBar.bossBar(title, progress, color, BossBar.Overlay.PROGRESS);
            player.showBossBar(locationCompassBossBar);
            return;
        }
        locationCompassBossBar.name(title);
        locationCompassBossBar.progress(progress);
        locationCompassBossBar.color(color);
        player.showBossBar(locationCompassBossBar);
    }

    private boolean isBeaconMode() {
        return "beacon".equals(main.getConfiguration().getBeamMode());
    }

    private static void sendRealBlock(final Player player, final Location location) {
        player.sendBlockChange(location, location.getBlock().getBlockData());
    }

    public final String getCooldownFormatted(final Quest quest) {

        long mostRecentCompleteTime = 0;


        for (final CompletedQuest completedQuest : getCompletedQuests()) {
            if (completedQuest.getQuest().equals(quest)) {
                if (completedQuest.getTimeCompleted() > mostRecentCompleteTime) {
                    mostRecentCompleteTime = completedQuest.getTimeCompleted();
                }
            }
        }

        final long completeTimeDifference = System.currentTimeMillis() - mostRecentCompleteTime;
        final long completeTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(completeTimeDifference);


        final long timeToWaitInMinutes = quest.getAcceptCooldownComplete() - completeTimeDifferenceMinutes;
        final double timeToWaitInHours = Math.round((timeToWaitInMinutes / 60f) * 10) / 10.0;
        final double timeToWaitInDays = Math.round((timeToWaitInHours / 24f) * 10) / 10.0;

        final String prefix = main.getLanguageManager().getString("placeholders.questcooldownleftformatted.prefix", this, quest);
        //Cooldown:
        if (completeTimeDifferenceMinutes >= quest.getAcceptCooldownComplete()) {
            return prefix + main.getLanguageManager().getString("placeholders.questcooldownleftformatted.no-cooldown", this, quest);
        } else {
            if (timeToWaitInMinutes < 60) {
                if (timeToWaitInMinutes == 1) {
                    return prefix + main.getLanguageManager().getString("placeholders.questcooldownleftformatted.minute", this, quest);
                } else {
                    return prefix + main.getLanguageManager().getString("placeholders.questcooldownleftformatted.minutes", this, quest, Map.of(
                            "%MINUTES%", "" + timeToWaitInMinutes
                    ));

                }
            } else {
                if (timeToWaitInHours < 24) {
                    if (timeToWaitInHours == 1) {
                        return prefix + main.getLanguageManager().getString("placeholders.questcooldownleftformatted.hour", this, quest);
                    } else {
                        return main.getLanguageManager().getString("placeholders.questcooldownleftformatted.hours", this, quest, Map.of(
                                "%HOURS%", "" + timeToWaitInHours
                        ));
                    }
                } else {
                    if (timeToWaitInDays == 1) {
                        return prefix + main.getLanguageManager().getString("placeholders.questcooldownleftformatted.day", this, quest);

                    } else {
                        return main.getLanguageManager().getString("placeholders.questcooldownleftformatted.days", this, quest, Map.of(
                                "%DAYS%", "" + timeToWaitInDays
                        ));
                    }
                }
            }
        }
    }

    public String addActiveQuest(final ActiveQuest activeQuest, final boolean triggerAcceptQuestTrigger, final boolean sendQuestInfo) {
        if (main.getDataManager().isDisabled()) {
            return "Plugin is disabled due to misconfiguration - you currently cannot take any new quests anymore";
        }

        //Configuration Option: general.max-active-quests-per-player
        if (main.getConfiguration().getMaxActiveQuestsPerPlayer() != -1 && activeQuests.size() >= main.getConfiguration().getMaxActiveQuestsPerPlayer()) {
            return main.getLanguageManager().getString("chat.reached-max-active-quests-per-player-limit", getPlayer(), this, activeQuest, Map.of(
                    "%MAXACTIVEQUESTSPERPLAYER%", "" + main.getConfiguration().getMaxActiveQuestsPerPlayer()
            ));
        }

        for (ActiveQuest currentActiveQuest : activeQuests) {
            if (currentActiveQuest.getQuestIdentifier().equals(activeQuest.getQuestIdentifier())) {
                return main.getLanguageManager().getString("chat.quest-already-accepted", getPlayer());
            }
        }
        int completedAmount = 0;
        long mostRecentCompleteTime = 0;

        int failedAmount = 0;
        long mostRecentFailTime = 0;

        int acceptedAmount = 0;
        for (final CompletedQuest completedQuest : getCompletedQuests()) {
            if (completedQuest.getQuestIdentifier().equals(activeQuest.getQuestIdentifier())) {
                completedAmount += 1;
                acceptedAmount += 1;
                if (completedQuest.getTimeCompleted() > mostRecentCompleteTime) {
                    mostRecentCompleteTime = completedQuest.getTimeCompleted();
                }
            }
        }
        for (final FailedQuest failedQuest : getFailedQuests()) {
            if (failedQuest.getQuestIdentifier().equals(activeQuest.getQuestIdentifier())) {
                failedAmount += 1;
                acceptedAmount += 1;
                if (failedQuest.getTimeFailed() > mostRecentFailTime) {
                    mostRecentFailTime = failedQuest.getTimeFailed();
                }
            }
        }
        for (final ActiveQuest activeQuest2 : getActiveQuests()) {
            if (activeQuest.getQuestIdentifier().equals(activeQuest2.getQuestIdentifier())) {
                acceptedAmount += 1;
            }
        }

        final long completeTimeDifference = System.currentTimeMillis() - mostRecentCompleteTime;
        final long completeTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(completeTimeDifference);


        final long timeToWaitInMinutes = activeQuest.getQuest().getAcceptCooldownComplete() - completeTimeDifferenceMinutes;
        final double timeToWaitInHours = Math.round((timeToWaitInMinutes / 60f) * 10) / 10.0;
        final double timeToWaitInDays = Math.round((timeToWaitInHours / 24f) * 10) / 10.0;


        if(activeQuest.getQuest().getMaxCompletions() > -1 && completedAmount >= activeQuest.getQuest().getMaxCompletions()){
            return main.getLanguageManager().getString("chat.reached-max-completions-limit", getPlayer(), this, activeQuest, Map.of(
                    "%MAXCOMPLETIONS%", ""+activeQuest.getQuest().getMaxCompletions(),
                    "%COMPLETEDAMOUNT%", ""+completedAmount
            ));
        }
        if(activeQuest.getQuest().getMaxAccepts() > -1 && acceptedAmount >= activeQuest.getQuest().getMaxAccepts()){
            return main.getLanguageManager().getString("chat.reached-max-accepts-limit", getPlayer(), this, activeQuest, Map.of(
                    "%MAXACCEPTS%", ""+activeQuest.getQuest().getMaxAccepts(),
                    "%ACCEPTEDAMOUNT%", ""+acceptedAmount
            ));
        }
        if(activeQuest.getQuest().getMaxFails() > -1 && failedAmount >= activeQuest.getQuest().getMaxFails()){
            return main.getLanguageManager().getString("chat.reached-max-fails-limit", getPlayer(), this, activeQuest, Map.of(
                    "%MAXFAILS%", ""+activeQuest.getQuest().getMaxFails(),
                    "%FAILEDAMOUNT%", ""+failedAmount
            ));
        }

        //Cooldown:
        if (completeTimeDifferenceMinutes >= activeQuest.getQuest().getAcceptCooldownComplete()) {

            //Requirements
            final StringBuilder requirementsStillNeeded = new StringBuilder();
            boolean allRequirementsFulfilled = true;

            if (getPlayer() == null) {
                requirementsStillNeeded.append("\n").append(main.getLanguageManager().getString("chat.add-active-quest-player-object-not-found", (QuestPlayer) null, this, activeQuest));
            }

            for (final Condition condition : activeQuest.getQuest().getRequirements()) {
                final ConditionResult check = condition.check(this);
                if (!check.fulfilled()) {
                    if(!condition.isHidden(this)) {
                        requirementsStillNeeded.append("\n").append(check.message());
                    }
                    allRequirementsFulfilled = false;
                }
            }


            if (!allRequirementsFulfilled) {
                return main.getLanguageManager().getString("chat.quest-not-all-requirements-fulfilled", getPlayer()) + requirementsStillNeeded;
            }





            finishAddingQuest(activeQuest, triggerAcceptQuestTrigger, false);
            if (sendQuestInfo) {
                final Player player = getPlayer();
                if (player != null) {

                    if(!activeQuest.getQuest().getObjectives().isEmpty()){
                        main.sendMessage(player, main.getLanguageManager().getString("chat.objectives-label-after-quest-accepting", player));
                    }

                    main.getQuestManager().sendActiveObjectivesAndProgress(this, activeQuest, 0);

                    if (main.getConfiguration().visualTitleQuestSuccessfullyAccepted_enabled) {

                        player.showTitle(
                                Title.title(main.parse(main.getLanguageManager().getString("titles.quest-accepted.title", player)),
                                        main.parse(main.getLanguageManager().getString("titles.quest-accepted.subtitle", player, this, activeQuest)),
                                        Title.Times.times(Duration.ofMillis(2), Duration.ofSeconds(3), Duration.ofMillis(8))
                                ));
                    }


                    player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 100, 2);


                    if (!activeQuest.getQuest().getObjectiveHolderDescription().isBlank()) {
                        main.sendMessage(player, main.getLanguageManager().getString("chat.quest-description", player, activeQuest));
                    } else {
                        main.sendMessage(player, main.getLanguageManager().getString("chat.missing-quest-description", player));
                    }

                    main.sendMessage(player, main.getLanguageManager().getString("chat.quest-successfully-accepted", player, activeQuest));


                }

            }


            return "accepted";
        } else {
            if (timeToWaitInMinutes < 60) {
                if(timeToWaitInMinutes == 1){
                    return main.getLanguageManager().getString("chat.quest-on-cooldown.minute", getPlayer(), this, activeQuest);
                }else{
                    return main.getLanguageManager().getString("chat.quest-on-cooldown.minutes", getPlayer(), this, activeQuest, Map.of(
                            "%MINUTES%", ""+timeToWaitInMinutes
                    ));
                }
            } else {
                if (timeToWaitInHours < 24) {
                    if (timeToWaitInHours == 1) {
                        return main.getLanguageManager().getString("chat.quest-on-cooldown.hour", getPlayer(), this, activeQuest);
                    } else {
                        return main.getLanguageManager().getString("chat.quest-on-cooldown.hours", getPlayer(), this, activeQuest, Map.of(
                                "%HOURS%", ""+timeToWaitInHours
                        ));
                    }
                } else {
                    if (timeToWaitInDays == 1) {
                        return main.getLanguageManager().getString("chat.quest-on-cooldown.day", getPlayer(), this, activeQuest);

                    } else {
                        return main.getLanguageManager().getString("chat.quest-on-cooldown.days", getPlayer(), this, activeQuest, Map.of(
                                "%DAYS%", ""+timeToWaitInDays
                        ));
                    }
                }
            }
        }


    }


    private void finishAddingQuest(final ActiveQuest activeQuest, final boolean triggerAcceptQuestTrigger, final boolean sendUpdateObjectivesUnlocked) {

        QuestFinishAcceptEvent questFinishAcceptEvent = new QuestFinishAcceptEvent(this, activeQuest, triggerAcceptQuestTrigger);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                Bukkit.getPluginManager().callEvent(questFinishAcceptEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(questFinishAcceptEvent);
        }

        if (questFinishAcceptEvent.isCancelled()) {
            return;
        }


        activeQuests.add(activeQuest);

        activeQuest.updateObjectivesUnlocked(sendUpdateObjectivesUnlocked, triggerAcceptQuestTrigger);


    }

    public void forceAddActiveQuestSilent(final ActiveQuest activeQuest, final boolean triggerAcceptQuestTrigger) { //ignores max amount, cooldown and requirements
        for (ActiveQuest activeQuest1 : activeQuests) {
            if (activeQuest1.getQuest().getIdentifier().equals(activeQuest.getQuest().getIdentifier())) {
                return;
            }
        }
        finishAddingQuest(activeQuest, triggerAcceptQuestTrigger, false);
    }
    public String forceAddActiveQuest(final ActiveQuest quest, final boolean triggerAcceptQuestTrigger) { //ignores max amount, cooldown and requirements
        for (ActiveQuest activeQuest : activeQuests) {
            if (activeQuest.getQuest().equals(quest.getQuest())) {
                return main.getLanguageManager().getString("chat.quest-already-accepted", getPlayer(), this);
            }
        }
        finishAddingQuest(quest, triggerAcceptQuestTrigger, false);
        return main.getLanguageManager().getString("chat.force-add-active-quest-accepted", getPlayer(), this);
    }

    public final UUID getUniqueId() {
        return uuid;
    }

    public final CopyOnWriteArrayList<ActiveQuest> getActiveQuests() {
        return activeQuests;
    }

    /*public void updateQuestStatus(){
        for(ActiveQuest activeQuest : activeQuests){
            activeQuest.updateQuestStatus();
            if(activeQuest.isCompleted()){
                giveReward(activeQuest.getQuest());
                questsToRemove.add(activeQuest);
            }
        }
        activeQuests.removeAll(questsToRemove);
        for(ActiveQuest activeQuest2 : questsToRemove) {
            completedQuests.add(new CompletedQuest(activeQuest2.getQuest(), this));
        }

        questsToRemove.clear();
    }*/

    public void giveReward(final Quest quest) {
        if (main.getDataManager().isDisabled()) {
            return;
        }
        sendDebugMessage("QuestPlayer.giveReward(). Quest: " + quest.getIdentifier());


        final Player player = getPlayer();


        String fullRewardString = "";

        int counterWithRewardNames = 0;
        for (Action action : quest.getRewards()) {
            main.getActionManager().executeActionWithConditions(action, this, player, true, quest);
            if(main.getConfiguration().showRewardsAfterQuestCompletion){
                if(!action.getActionName().isBlank()){
                    counterWithRewardNames++;
                    if(counterWithRewardNames == 1){
                        fullRewardString += main.getLanguageManager().getString("chat.quest-completed-rewards-prefix", player, this, quest, action);
                    }
                    fullRewardString += "\n"+ main.getLanguageManager().getString("chat.quest-completed-rewards-rewardformat", player, this, quest, action, Map.of(
                            "%reward%", action.getActionName()
                    ));
                }
            }
        }
        if(counterWithRewardNames > 0){
            fullRewardString += "\n" + main.getLanguageManager().getString("chat.quest-completed-rewards-suffix", player, this, quest);
        }

        if (player != null) {
            if(fullRewardString.isBlank()){
                main.sendMessage(player, main.getLanguageManager().getString("chat.quest-completed-and-rewards-given", getPlayer(), quest)
                );
            }else{
                main.sendMessage(player, main.getLanguageManager().getString("chat.quest-completed-and-rewards-given", getPlayer(), quest)
                        + "<RESET>" + fullRewardString
                );
            }

        }

    }

    public void sendMessage(final String message) {
        final Player player = getPlayer();
        if (player != null) {
            player.sendMessage(main.parse(message));
        }
    }

    public void sendDebugMessage(String message, final Object... interpolatedStrings) {
        if (!main.getQuestManager().isDebugEnabledPlayer(this.uuid)) {
            return;
        }
        final Player player = getPlayer();
        if (player != null) {
            if(interpolatedStrings.length > 0){
                message = message.formatted((Object[]) interpolatedStrings);
            }
            player.sendMessage(main.parse(NotQuestColors.debugTitleGradient + "[NotQuests Debug]</gradient> " + NotQuestColors.debugGradient + message + "</gradient>"));
        }
    }

    public final List<CompletedQuest> getCompletedQuests() {
        return completedQuests;
    }
    public final List<FailedQuest> getFailedQuests() {
        return failedQuests;
    }


    /**
     * Shows the quest-completed title + sound. showTitle/playSound/getLocation are main-thread-only,
     * but quest completion can be driven from an async context (e.g. the async chat/conversation
     * thread), so this hops to the main thread when needed.
     */
    private void showQuestCompletedVisuals(final ActiveQuest activeQuest) {
        final Player player = getPlayer();
        if (player == null) {
            return;
        }
        final Runnable visuals = () -> {
            if (main.getConfiguration().visualTitleQuestCompleted_enabled) {
                player.showTitle(
                        Title.title(main.parse(main.getLanguageManager().getString("titles.quest-completed.title", player)),
                                main.parse(main.getLanguageManager().getString("titles.quest-completed.subtitle", player, this, activeQuest)),
                                Title.Times.times(Duration.ofMillis(2), Duration.ofSeconds(3), Duration.ofMillis(8))
                        ));
            }
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 100, 40);
        };
        if (Bukkit.isPrimaryThread()) {
            visuals.run();
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), visuals);
        }
    }

    public void forceActiveQuestCompleted(final ActiveQuest activeQuest) {
        sendDebugMessage("ForceActiveQuestCompleted...");
        QuestCompletedEvent questCompletedEvent = new QuestCompletedEvent(this, activeQuest, true);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                Bukkit.getPluginManager().callEvent(questCompletedEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(questCompletedEvent);
        }

        if (questCompletedEvent.isCancelled()) {
            return;
        }

        activeQuest.getCompletedObjectives().addAll(activeQuest.getActiveObjectives());
        activeQuest.getActiveObjectives().clear();

        questsToComplete.add(activeQuest);

        completedQuests.add(new CompletedQuest(activeQuest.getQuest(), this));

        showQuestCompletedVisuals(activeQuest);


        for (final ActiveQuest activeQuest2 : activeQuests) {
            for (final ActiveObjective objective : activeQuest2.getActiveObjectives()) {
                if (objective.getObjective() instanceof final OtherQuestObjective otherQuestObjective) {
                    if (otherQuestObjective.getOtherQuest().equals(activeQuest.getQuest())) {
                        objective.addProgress(1, (NQNPC)null);
                    }
                }
            }
        }
        removeCompletedQuests();
        //activeQuests.removeAll(questsToComplete);

        giveReward(activeQuest.getQuest());

    }

    public void notifyActiveQuestCompleted(final ActiveQuest activeQuest) {
        QuestCompletedEvent questCompletedEvent = new QuestCompletedEvent(this, activeQuest, false);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                Bukkit.getPluginManager().callEvent(questCompletedEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(questCompletedEvent);
        }

        if (questCompletedEvent.isCancelled()) {
            return;
        }

        if (activeQuest.isCompleted()) {
            //Add to completed Quests list. This list will then be used in removeCompletedQuests() to remove all its contests also from the activeQuests lists
            //(Without a ConcurrentModificationException)
            questsToComplete.add(activeQuest);
            //We can safely (without ConcurrentModificationException) add it to the CompletedQuests list already without having to remove it from activeQuests
            completedQuests.add(new CompletedQuest(activeQuest.getQuest(), this));

            //Give Quest completion reward & show Quest completion title
            giveReward(activeQuest.getQuest());
            showQuestCompletedVisuals(activeQuest);

        }

        //Handle OtherQuest Objectives for other Quests
        for (final ActiveQuest activeQuest2 : activeQuests) {
            for (final ActiveObjective objective : activeQuest2.getActiveObjectives()) {
                if (objective.getObjective() instanceof final OtherQuestObjective otherQuestObjective) {
                    if (otherQuestObjective.getOtherQuest().equals(activeQuest.getQuest())) {
                        objective.addProgress(1);
                    }
                }
            }
        }
    }

    public void setQuestPoints(long newQuestPoints, final boolean notifyPlayer) {
        if (main.getDataManager().isDisabled()) {
            return;
        }
        if (newQuestPoints < 0) { //Prevent questPoints from going below 0
            newQuestPoints = 0;
        }
        QuestPointsChangeEvent questPointsChangeEvent = new QuestPointsChangeEvent(this, newQuestPoints);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                Bukkit.getPluginManager().callEvent(questPointsChangeEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(questPointsChangeEvent);
        }


        if (!questPointsChangeEvent.isCancelled()) {
            this.questPoints = questPointsChangeEvent.getNewQuestPointsAmount();


            if (notifyPlayer) {
                final Player player = getPlayer();
                if (player != null) {
                    player.sendMessage(main.parse(
                            main.getLanguageManager().getString("chat.questpoints.notify-when-changed.set", player, this, Map.of(
                                    "%NEWQUESTPOINTSAMOUNT%", ""+newQuestPoints
                            ))
                    ));
                }
            }
        }
    }

    public void addQuestPoints(final long questPointsToAdd, final boolean notifyPlayer) {
        setQuestPoints(getQuestPoints() + questPointsToAdd, false);
        if (notifyPlayer) {
            final Player player = getPlayer();
            if (player != null) {
                player.sendMessage(main.parse(
                        main.getLanguageManager().getString("chat.questpoints.notify-when-changed.add", player, this, Map.of(
                                "%QUESTPOINTSTOADD%", "" + questPointsToAdd
                        ))
                ));
            }
        }
    }

    public void removeQuestPoints(final long questPointsToRemove, final boolean notifyPlayer) {
        setQuestPoints(getQuestPoints() - questPointsToRemove, false);
        if (notifyPlayer) {
            final Player player = getPlayer();
            if (player != null) {
                player.sendMessage(main.parse(
                        main.getLanguageManager().getString("chat.questpoints.notify-when-changed.remove", player, this, Map.of(
                                "%QUESTPOINTSTOREMOVE%", ""+questPointsToRemove
                        ))
                ));
            }
        }
    }




    public final long getQuestPoints() {
        return questPoints;
    }

    public void removeCompletedQuests() {
        if(main.getDataManager().isDisabled()){
            return;
        }
        if (questsToComplete.isEmpty()) {
            return;
        }

        sendDebugMessage("Executing removeCompletedQuests");


        activeQuests.removeAll(questsToComplete);

        questsToComplete.clear();
    }

    public void addCompletedQuest(final CompletedQuest completedQuest) {
        completedQuests.add(completedQuest);
    }
    public void addFailedQuest(final FailedQuest failedQuest) {
        failedQuests.add(failedQuest);
    }

    public void failQuest(final ActiveQuest activeQuestToFail) {
        final ArrayList<ActiveQuest> activeQuestsCopy = new ArrayList<>(activeQuests);
        for (final ActiveQuest foundActiveQuest : activeQuestsCopy) {
            if (activeQuestToFail.equals(foundActiveQuest)) {

                foundActiveQuest.fail();
                questsToRemove.add(foundActiveQuest);
                final Player player = getPlayer();

                failedQuests.add(new FailedQuest(foundActiveQuest.getQuest(), this));


                if (player != null) {
                    if (main.getConfiguration().visualTitleQuestFailed_enabled) {
                        player.showTitle(
                                Title.title(main.parse(main.getLanguageManager().getString("titles.quest-failed.title", player)),
                                        main.parse(main.getLanguageManager().getString("titles.quest-failed.subtitle", player, this, activeQuestToFail)),
                                        Title.Times.times(Duration.ofMillis(2), Duration.ofSeconds(3), Duration.ofMillis(8))
                                ));
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_DEATH, SoundCategory.MASTER, 100, 1);
                }
            }
        }
        activeQuests.removeAll(questsToRemove);
        activeQuestsCopy.removeAll(questsToComplete);

        questsToComplete.clear();


    }

    public final boolean hasAcceptedQuest(final Quest quest) {
        for (final ActiveQuest activeQuest : activeQuests) {
            if (activeQuest.getQuestIdentifier().equalsIgnoreCase(quest.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasCompletedQuest(final Quest quest) {
        for (final CompletedQuest completedQuest : completedQuests) {
            if (completedQuest.getQuestIdentifier() .equalsIgnoreCase(quest.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasCompletedQuest(final String questName) {
        for (final CompletedQuest completedQuest : completedQuests) {
            if (completedQuest.getQuestIdentifier() .equalsIgnoreCase(questName)) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasFailedQuest(final Quest quest) {
        for (final FailedQuest failedQuest : failedQuests) {
            if (failedQuest.getQuestIdentifier() .equalsIgnoreCase(quest.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasFailedQuest(final String questName) {
        for (final FailedQuest failedQuest : failedQuests) {
            if (failedQuest.getQuestIdentifier() .equalsIgnoreCase(questName)) {
                return true;
            }
        }
        return false;
    }


    public final Player getPlayer(){
        if(player != null){
            return player;
        }else{
            this.player = Bukkit.getPlayer(uuid);
            return player;
        }
    }

    public final ActiveQuest getActiveQuest(final Quest quest) {
        for(final ActiveQuest activeQuest : activeQuests){
            if(activeQuest.getQuest().equals(quest)){
                return activeQuest;
            }
        }
        return null;
    }


    public final BossBar getBossBar() {
        return bossBar;
    }

    public void sendObjectiveProgress(final ActiveObjective activeObjective) {
        final Player player = getPlayer();
        if (player == null) {
            return;
        }
        if (main.getConfiguration().isVisualObjectiveTrackingShowProgressInActionBar()) {
            if (activeObjective.getProgressNeeded() == 1) {
                getPlayer().sendActionBar(main.parse(
                        main.getLanguageManager().getString("objective-tracking.actionbar-progress-update.only-one-max-progress", getPlayer(), this, activeObjective, activeObjective.getActiveObjectiveHolder())
                ));
            } else {
                getPlayer().sendActionBar(main.parse(
                        main.getLanguageManager().getString("objective-tracking.actionbar-progress-update.default", getPlayer(), this, activeObjective, activeObjective.getActiveObjectiveHolder())
                ));
            }
        }
        if(main.getConfiguration().isVisualObjectiveTrackingShowProgressInBossBar()){
            float progress = (float)activeObjective.getCurrentProgress() / (float)activeObjective.getProgressNeeded();
            if(progress >= 1.0f && !main.getConfiguration().isVisualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted()){
                if(bossBar != null){
                    player.hideBossBar(bossBar);
                    bossBar = null;
                    lastBossBarActiveTimeInSeconds = 0;
                }
                return; //Hide bossbar once it reached 100%
            }

            if(progress < 0.0f){
                progress = 0;
            }else if(progress > 1.0f){
                progress = 1.0f;
            }

            final String languageString = activeObjective.getProgressNeeded() == 1 ? "objective-tracking.bossbar-progress-update.only-one-max-progress" : "objective-tracking.bossbar-progress-update.default";
            if (bossBar != null) {
                bossBar.name(main.getLanguageManager().getComponent(languageString, getPlayer(), this, activeObjective.getActiveObjectiveHolder(), activeObjective));
                bossBar.progress(progress);
                lastBossBarActiveTimeInSeconds = 0;
            } else {
                bossBar = BossBar.bossBar(main.getLanguageManager().getComponent(languageString, getPlayer(), this, activeObjective.getActiveObjectiveHolder(), activeObjective ),
                        progress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
                player.showBossBar(bossBar);
                lastBossBarActiveTimeInSeconds = 0;
            }

        }
    }

    public void increaseBossBarTimeByOneSecond(){
        lastBossBarActiveTimeInSeconds++;
        if(main.getConfiguration().getVisualObjectiveTrackingBossBarTimer() >0 && lastBossBarActiveTimeInSeconds >= main.getConfiguration().getVisualObjectiveTrackingBossBarTimer()){
            getPlayer().hideBossBar(bossBar);
            bossBar = null;
            lastBossBarActiveTimeInSeconds = 0;
        }
    }

    public void updateConditionObjectives(final Player player) {
        //sendDebugMessage("updateConditionObjectives was called...");
        if (!isHasActiveConditionObjectives() && !isHasActiveVariableObjectives()) {
            //sendDebugMessage("   No active objectives to update.");
            return;
        }
        for (final ActiveQuest activeQuest : getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.getObjective() instanceof final ConditionObjective conditionObjective) {
                    if (conditionObjective.isCheckOnlyWhenCorrespondingVariableValueChanged() || !activeObjective.isUnlocked()) {
                        continue;
                    }

                    final Condition condition = conditionObjective.getCondition();
                    if (condition == null) {
                        continue;
                    }
                    if (!condition.check(this).fulfilled()) {
                        continue;
                    }

                    activeObjective.addProgress(1);

                } else if(activeObjective.getObjective() instanceof final NumberVariableObjective numberVariableObjective) {
                    //sendDebugMessage("Found numbervariableobjective to update!");
                    if (numberVariableObjective.isCheckOnlyWhenCorrespondingVariableValueChanged() || !activeObjective.isUnlocked()) {
                        continue;
                    }
                    numberVariableObjective.updateProgress(activeObjective);
                }
            }
            activeQuest.removeCompletedObjectives(true);
        }
        removeCompletedQuests();
    }


    public void onQuit(final Player player){
        if (!getActiveQuests().isEmpty()) {
            for (final ActiveQuest activeQuest : getActiveQuests()) {

                for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                    if (activeTrigger.getTrigger().getTriggerType().equals("DISCONNECT")) {

                        main.getQuestEvents().handleGeneralTrigger(this, activeTrigger);
                    }
                }
            }
        }
    }

    public void onQuitAsync(final Player player){
        bossBar = null;
        hideLocationCompass(player);
        main.getTagManager().onQuit(this, player);
    }

    public void onJoin(final Player player){
        this.player = player;
    }

    public void onJoinAsync(final Player player){
        this.player = player;
        main.getTagManager().onJoin(this, player);
    }

    public final boolean isCurrentlyLoading() {
        return currentlyLoading;
    }

    public void setCurrentlyLoading(final boolean currentlyLoading) {
        this.currentlyLoading = currentlyLoading;
    }

    public final boolean isFinishedLoadingGeneralData() {
        return finishedLoadingGeneralData;
    }

    public final void setFinishedLoadingGeneralData(boolean finishedLoadingGeneralData) {
        this.finishedLoadingGeneralData = finishedLoadingGeneralData;
    }

    public final boolean isFinishedLoadingTags() {
        return finishedLoadingTags;
    }

    public final void setFinishedLoadingTags(boolean finishedLoadingTags) {
        this.finishedLoadingTags = finishedLoadingTags;
    }


    public void queueObjectiveCheck(final Consumer<ActiveObjective> runForEachObjective){
        if(getActiveQuests().isEmpty()){
            return;
        }
        queuedObjectivesToCheck.add(runForEachObjective);
    }

    public void checkForActiveObjective(final ActiveObjective activeObjective){
        if(!activeObjective.isUnlocked()){
            return;
        }
        for(final Consumer<ActiveObjective> runForEachObjective : queuedObjectivesToCheck){
            runForEachObjective.accept(activeObjective);
        }
        if(!activeObjective.getActiveObjectives().isEmpty()){
            for(final ActiveObjective childActiveObjective : activeObjective.getActiveObjectives()){
                checkForActiveObjective(childActiveObjective);
            }
            activeObjective.removeCompletedObjectives(true);
        }
    }
    public void checkQueuedObjectives(){
        if(queuedObjectivesToCheck.isEmpty()){
            return;
        }
        sendDebugMessage("Checking queued objectives...");
        for (final ActiveQuest activeQuest : getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                checkForActiveObjective(activeObjective);

            }
            activeQuest.removeCompletedObjectives(true);
        }
        removeCompletedQuests();

        queuedObjectivesToCheck.clear();
    }
}
