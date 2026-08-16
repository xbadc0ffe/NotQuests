package com.notquests.paper.builtin.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.trait.FollowTrait;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.managers.npc.NQNPC;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveType;
import com.notquests.paper.structs.ActiveObjective;

import static com.notquests.paper.commands.arguments.LocationArgument.locationArgument;

public final class EscortNPC {
    private static final String TYPE = "EscortNPC";
    private static final String NPC_TO_ESCORT_ID = "npcToEscortId";
    private static final String DESTINATION_NPC_ID = "destinationNpcId";
    private static final String SPAWN_LOCATION = "spawnLocation";

    private EscortNPC() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            return;
        }
        objectives.objective(TYPE)
                .displayName("Escort NPC")
                .description("Counts when the player escorts one Citizens NPC to another Citizens NPC.")
                .field(
                        NPC_TO_ESCORT_ID,
                        FieldTypes.storedInteger(-1).config("specifics.NPCToEscortID"),
                        "Citizens NPC id of the NPC the player must escort.")
                .field(
                        DESTINATION_NPC_ID,
                        FieldTypes.storedInteger(-1).config("specifics.destinationNPCID"),
                        "Citizens NPC id of the destination NPC.")
                .field(
                        SPAWN_LOCATION,
                        FieldTypes.storedLocation().config("specifics.spawnLocation"),
                        "Optional location where the escorted NPC should spawn before following the player.")
                .commands((type, builder, level) -> registerCommands(main, type, builder, level))
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .onUnlock((objective, activeObjective, startup) -> startEscort(main, objective, activeObjective))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandBuilder builder,
            final int level) {
        final NQFlag spawnLocation = NQFlag.builder(
                        SPAWN_LOCATION,
                        NQDescription.of("Location where the escorted NPC should spawn before the objective starts."))
                .withArgument(locationArgument())
                .build();
        main.getCommandManager().getNQCommandManager().command(builder
                .required(
                        "NPC to escort",
                        NQArguments.integerArgument(),
                        NQDescription.of("Citizens NPC id of the NPC the player must escort."),
                        (context, input) -> npcIdSuggestions(main, -1))
                .required(
                        "Destination NPC",
                        NQArguments.integerArgument(),
                        NQDescription.of("Citizens NPC id of the destination NPC."),
                        (context, input) -> npcIdSuggestions(main, context.get("NPC to escort")))
                .flag(spawnLocation)
                .handler(context -> addObjective(main, type, context, level, spawnLocation)));
    }

    private static List<String> npcIdSuggestions(final NotQuests main, final int excludedId) {
        final List<String> completions = new ArrayList<>();
        for (final int npcID : main.getIntegrationsManager().getCitizensManager().getAllNPCIDs()) {
            if (npcID != excludedId) {
                completions.add(String.valueOf(npcID));
            }
        }
        return completions;
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level,
            final NQFlag spawnLocationFlag) {
        final int npcToEscortId = context.get("NPC to escort");
        final int destinationNpcId = context.get("Destination NPC");
        if (npcToEscortId == destinationNpcId) {
            context.sender().sendMessage(main.parse("<error>Error: The escort NPC and destination NPC must be different."));
            return;
        }

        final DefinedObjective objective = type.createObjective();
        objective.setProgressNeededExpression("1");
        objective.setValue(NPC_TO_ESCORT_ID, npcToEscortId);
        objective.setValue(DESTINATION_NPC_ID, destinationNpcId);
        objective.setValue(SPAWN_LOCATION, context.flags().getValue(spawnLocationFlag, null));
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    public static boolean isEscortNPC(final ActiveObjective activeObjective) {
        return activeObjective.getObjective() instanceof final DefinedObjective objective && objective.isType(TYPE);
    }

    public static Integer npcToEscortId(final ActiveObjective activeObjective) {
        if (!(activeObjective.getObjective() instanceof final DefinedObjective objective) || !objective.isType(TYPE)) {
            return null;
        }
        return objective.value(NPC_TO_ESCORT_ID, Integer.class);
    }

    public static boolean tryCompleteAtDestination(
            final NotQuests main,
            final ActiveObjective activeObjective,
            final Player player,
            final int clickedNpcId,
            final NQNPC clickedNqNpc) {
        if (!(activeObjective.getObjective() instanceof final DefinedObjective objective) || !objective.isType(TYPE)) {
            return false;
        }
        final Integer destinationNpcId = objective.value(DESTINATION_NPC_ID, Integer.class);
        if (destinationNpcId == null || destinationNpcId != clickedNpcId) {
            return false;
        }
        final Integer npcToEscortId = objective.value(NPC_TO_ESCORT_ID, Integer.class);
        if (npcToEscortId == null) {
            return false;
        }
        final NPC npcToEscort = CitizensAPI.getNPCRegistry().getById(npcToEscortId);
        if (npcToEscort == null) {
            return false;
        }
        if (!npcToEscort.isSpawned() || npcToEscort.getEntity().getLocation().distance(player.getLocation()) >= 6) {
            player.sendMessage(main.parse("<RED>The NPC you have to escort is not close enough to you!"));
            return false;
        }

        activeObjective.addProgress(1, clickedNqNpc);
        final String npcName = main.getMiniMessage()
                .serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(npcToEscort.getName()));
        player.sendMessage(main.parse("<GREEN>You have successfully delivered the NPC <highlight>" + npcName));
        removeFollowTrait(npcToEscort);
        npcToEscort.despawn();
        return true;
    }

    public static void stopEscortIfMatching(final NotQuests main, final ActiveObjective activeObjective) {
        if (isEscortNPC(activeObjective)
                && main.getIntegrationsManager().isCitizensEnabled()
                && main.getIntegrationsManager().getCitizensManager() != null) {
            main.getIntegrationsManager().getCitizensManager().handleEscortObjective(activeObjective);
        }
    }

    private static void startEscort(
            final NotQuests main,
            final DefinedObjective objective,
            final ActiveObjective activeObjective) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            return;
        }
        final Integer npcToEscortId = objective.value(NPC_TO_ESCORT_ID, Integer.class);
        final Integer destinationNpcId = objective.value(DESTINATION_NPC_ID, Integer.class);
        if (npcToEscortId == null || destinationNpcId == null) {
            return;
        }
        main.getIntegrationsManager()
                .getCitizensManager()
                .startEscortObjective(
                        npcToEscortId,
                        destinationNpcId,
                        objective.value(SPAWN_LOCATION, Location.class),
                        activeObjective.getActiveObjectiveHolder());
    }

    private static void removeFollowTrait(final NPC npc) {
        FollowTrait followerTrait = null;
        for (final Trait trait : npc.getTraits()) {
            if (trait.getName().toLowerCase(Locale.ROOT).contains("follow")) {
                followerTrait = (FollowTrait) trait;
            }
        }
        if (followerTrait != null) {
            npc.removeTrait(followerTrait.getClass());
        }
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final ActiveObjective activeObjective) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            return "    <RED>Error: Citizens plugin not installed. Contact an admin.";
        }
        final Integer npcToEscortId = objective.value(NPC_TO_ESCORT_ID, Integer.class);
        final Integer destinationNpcId = objective.value(DESTINATION_NPC_ID, Integer.class);
        final NPC npc = npcToEscortId == null ? null : CitizensAPI.getNPCRegistry().getById(npcToEscortId);
        final NPC destination = destinationNpcId == null ? null : CitizensAPI.getNPCRegistry().getById(destinationNpcId);
        if (npc == null || destination == null) {
            return "    <GRAY>The target or destination NPC is currently not available!";
        }

        final String npcName = main.getMiniMessage()
                .serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(npc.getName().replace("§", "&")));
        final String destinationName = main.getMiniMessage()
                .serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(destination.getName()));
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.escortNPC.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%NPCNAME%", npcName, "%DESTINATIONNPCNAME%", destinationName));
    }
}
