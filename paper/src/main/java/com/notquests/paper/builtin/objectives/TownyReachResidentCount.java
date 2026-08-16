package com.notquests.paper.builtin.objectives;

import java.util.Map;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.structs.ActiveObjective;

public final class TownyReachResidentCount {
    private static final String TYPE = "TownyReachResidentCount";
    private static final String AMOUNT = "amount";
    private static final String DO_NOT_COUNT_PREVIOUS_RESIDENTS = "doNotCountPreviousResidents";

    private TownyReachResidentCount() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return;
        }
        objectives.objective(TYPE)
                .displayName("Reach Towny Resident Count")
                .description("Counts when the player's Towny town reaches a target resident count.")
                .field(
                        AMOUNT,
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Minimum resident count the player's town must reach.")
                .flag(
                        DO_NOT_COUNT_PREVIOUS_RESIDENTS,
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.countPreviousResidents"),
                        "Only count residents added after this objective unlocks; existing residents do not count.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.townyReachResidentCount.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%AMOUNT%",
                                        ""
                                                + (activeObjective != null
                                                        ? activeObjective.getProgressNeeded()
                                                        : objective.text(AMOUNT)))))
                .onUnlock((objective, activeObjective, startup) -> addExistingResidentCount(main, objective, activeObjective))
                .register();
    }

    public static boolean isTownyReachResidentCount(final ActiveObjective activeObjective) {
        return activeObjective.getObjective() instanceof final DefinedObjective objective && objective.isType(TYPE);
    }

    public static void addExistingResidentCount(
            final NotQuests main,
            final DefinedObjective objective,
            final ActiveObjective activeObjective) {
        if (activeObjective.getCurrentProgress() != 0
                || !main.getIntegrationsManager().isTownyEnabled()
                || Boolean.TRUE.equals(objective.value(DO_NOT_COUNT_PREVIOUS_RESIDENTS, Boolean.class))) {
            return;
        }
        final Player player = activeObjective.getQuestPlayer().getPlayer();
        if (player == null) {
            return;
        }
        final Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
        if (resident == null) {
            return;
        }
        final Town town = resident.getTownOrNull();
        if (town != null) {
            activeObjective.addProgress(town.getNumResidents());
        }
    }
}
