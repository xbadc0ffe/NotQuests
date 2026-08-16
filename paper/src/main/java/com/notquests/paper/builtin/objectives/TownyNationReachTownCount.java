package com.notquests.paper.builtin.objectives;

import java.util.Map;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.ActiveObjective;

public final class TownyNationReachTownCount {
    private static final String TYPE = "TownyNationReachTownCount";
    private static final String AMOUNT = "amount";
    private static final String DO_NOT_COUNT_PREVIOUS_TOWNS = "doNotCountPreviousTowns";

    private TownyNationReachTownCount() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return;
        }
        objectives.objective(TYPE)
                .displayName("Reach Towny Nation Town Count")
                .description("Counts when the player's Towny nation reaches a target town count.")
                .field(
                        AMOUNT,
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Minimum town count the player's nation must reach.")
                .flag(
                        DO_NOT_COUNT_PREVIOUS_TOWNS,
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.countPreviousTowns"),
                        "Only count towns added after this objective unlocks; existing towns do not count.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.townyNationReachTownCount.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%AMOUNT%",
                                        ""
                                                + (activeObjective != null
                                                        ? activeObjective.getProgressNeeded()
                                                        : objective.text(AMOUNT)))))
                .onUnlock((objective, activeObjective, startup) -> addExistingTownCount(main, objective, activeObjective))
                .register();
    }

    public static boolean isTownyNationReachTownCount(final ActiveObjective activeObjective) {
        return activeObjective.getObjective() instanceof final DefinedObjective objective && objective.isType(TYPE);
    }

    public static void addExistingTownCount(
            final NotQuests main,
            final DefinedObjective objective,
            final ActiveObjective activeObjective) {
        if (activeObjective.getCurrentProgress() != 0
                || !main.getIntegrationsManager().isTownyEnabled()
                || Boolean.TRUE.equals(objective.value(DO_NOT_COUNT_PREVIOUS_TOWNS, Boolean.class))) {
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
        if (town == null) {
            return;
        }
        final Nation nation = town.getNationOrNull();
        if (nation != null) {
            activeObjective.addProgress(nation.getNumTowns());
        }
    }
}
