package com.notquests.paper.builtin.objectives;

import java.util.Map;
import com.notquests.paper.NotQuests;
import com.notquests.paper.managers.npc.NQNPC;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.CompletedQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.objectives.Objective;

public final class OtherQuest {
    private OtherQuest() {}

    public static final String TYPE = "OtherQuest";
    private static final String OTHER_QUEST = "otherQuest";
    private static final String COUNT_PREVIOUS = "countPreviouslyCompletedQuests";

    public static boolean isOtherQuest(final Objective objective) {
        return objective instanceof DefinedObjective defined && defined.isType(TYPE);
    }

    public static boolean targetsQuest(final Objective objective, final Quest quest) {
        if (!(objective instanceof DefinedObjective defined) || !defined.isType(TYPE) || quest == null) {
            return false;
        }
        final Quest target = defined.value(OTHER_QUEST, Quest.class);
        return quest.equals(target);
    }

    public static void addProgressIfTargets(final ActiveObjective activeObjective, final Quest completedQuest) {
        if (targetsQuest(activeObjective.getObjective(), completedQuest)) {
            activeObjective.addProgress(1, (NQNPC) null);
        }
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective(TYPE)
                .displayName("Other Quest")
                .description("Counts completions of another quest.")
                .field(
                        OTHER_QUEST,
                        FieldTypes.quest().config("specifics.otherQuestName"),
                        "Quest the player must complete.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of times the other quest must be completed.")
                .flag(
                        COUNT_PREVIOUS,
                        FieldTypes.presenceFlag().config("specifics.countPreviousCompletions"),
                        "Also count matching quest completions the player already had before this objective unlocked.")
                .taskDescription((objective, questPlayer, activeObjective) -> {
                    final Quest quest = objective.value(OTHER_QUEST, Quest.class);
                    return main.getLanguageManager()
                            .getString(
                                    "chat.objectives.taskDescription.otherQuest.base",
                                    questPlayer,
                                    activeObjective,
                                    Map.of("%OTHERQUESTNAME%", quest == null ? "" : quest.getDisplayNameOrIdentifier()));
                })
                .onUnlock((objective, activeObjective, loading) -> {
                    if (!Boolean.TRUE.equals(objective.value(COUNT_PREVIOUS, Boolean.class))) {
                        return;
                    }
                    final Quest target = objective.value(OTHER_QUEST, Quest.class);
                    if (target == null) {
                        return;
                    }
                    for (final CompletedQuest completedQuest : activeObjective.getQuestPlayer().getCompletedQuests()) {
                        if (completedQuest.getQuest().equals(target)) {
                            activeObjective.addProgress(1, (NQNPC) null);
                        }
                    }
                })
                .register();
    }
}
