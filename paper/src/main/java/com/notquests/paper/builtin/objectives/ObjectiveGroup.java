package com.notquests.paper.builtin.objectives;

import java.util.Map;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.objectives.Objective;

public final class ObjectiveGroup {
    private ObjectiveGroup() {}

    public static final String TYPE = "Objective";
    private static final String HOLDER_NAME = "objectiveHolderName";

    public static boolean isObjectiveGroup(final Objective objective) {
        return objective instanceof DefinedObjective defined && defined.isType(TYPE);
    }

    public static String holderName(final Objective objective) {
        return objective instanceof DefinedObjective defined && defined.isType(TYPE)
                ? defined.text(HOLDER_NAME)
                : "";
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective(TYPE)
                .displayName("Objective Group")
                .description("Creates a named parent objective that can contain sub-objectives.")
                .field(
                        HOLDER_NAME,
                        FieldTypes.greedyText().config("specifics.objectiveHolderName"),
                        "Display name of this objective group.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.objective.base",
                                questPlayer,
                                activeObjective,
                                Map.of("%OBJECTIVEHOLDERNAME%", objective.text(HOLDER_NAME))))
                .onUnlock((objective, activeObjective, loading) -> completeIfEmpty(activeObjective, loading))
                .register();
    }

    public static void completeIfEmpty(final ActiveObjective activeObjective, final boolean loading) {
        if (!activeObjective.getActiveObjectives().isEmpty()) {
            return;
        }
        activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        activeObjective.getActiveObjectiveHolder().removeCompletedObjectives(!loading);
        activeObjective.getQuestPlayer().removeCompletedQuests();
    }
}
