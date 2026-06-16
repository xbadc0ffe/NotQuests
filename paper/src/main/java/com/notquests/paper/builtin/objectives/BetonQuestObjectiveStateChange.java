package com.notquests.paper.builtin.objectives;

import java.util.Locale;
import java.util.Map;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.quest.objective.ObjectiveState;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveType;
import com.notquests.paper.structs.ActiveObjective;

public final class BetonQuestObjectiveStateChange {
    private static final String TYPE = "BetonQuestObjectiveStateChange";
    private static final String PACKAGE_NAME = "packageName";
    private static final String OBJECTIVE_NAME = "objectiveName";
    private static final String OBJECTIVE_STATE = "objectiveState";

    private BetonQuestObjectiveStateChange() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        if (!main.getIntegrationsManager().isBetonQuestEnabled()) {
            return;
        }
        objectives.objective(TYPE)
                .displayName("BetonQuest Objective State Change")
                .description("Counts when a BetonQuest objective changes to a configured state.")
                .field(
                        PACKAGE_NAME,
                        FieldTypes.text().config("specifics.packageName"),
                        "BetonQuest package that contains the objective to watch.")
                .field(
                        OBJECTIVE_NAME,
                        FieldTypes.text().config("specifics.objectiveName"),
                        "BetonQuest objective whose state change should count.")
                .field(
                        OBJECTIVE_STATE,
                        FieldTypes.text().config("specifics.objectiveState"),
                        "BetonQuest objective state that should count as progress.")
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
                        "package",
                        NQArguments.stringArgument(),
                        NQDescription.of("BetonQuest package that contains the objective to watch."),
                        (context, input) -> betonQuestManager(main).packageNames())
                .required(
                        "objective",
                        NQArguments.stringArgument(),
                        NQDescription.of("BetonQuest objective whose state change should count."),
                        (context, input) -> betonQuestManager(main).objectiveNames(context.get("package")))
                .required(
                        "objectiveState",
                        NQArguments.stringArgument(),
                        NQDescription.of("BetonQuest objective state that should count as progress."),
                        (context, input) -> betonQuestManager(main).objectiveStates())
                .handler(context -> addObjective(main, type, context, level)));
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level) {
        final String packageName = context.get("package");
        final String objectiveName = context.get("objective");
        final String stateName = context.get("objectiveState");
        final ObjectiveState state;
        try {
            betonQuestManager(main).objectiveIdentifier(packageName, objectiveName);
            state = ObjectiveState.valueOf(stateName.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            main.sendMessage(
                    context.sender(),
                    "<error>Error: BetonQuest objective state <highlight>"
                            + stateName
                            + "</highlight> does not exist.");
            return;
        } catch (final QuestException exception) {
            main.sendMessage(
                    context.sender(),
                    "<error>Error: BetonQuest objective <highlight>"
                            + packageName
                            + "."
                            + objectiveName
                            + "</highlight> does not exist.");
            return;
        }

        final DefinedObjective objective = type.createObjective();
        objective.setProgressNeededExpression("1");
        objective.setValue(PACKAGE_NAME, packageName);
        objective.setValue(OBJECTIVE_NAME, objectiveName);
        objective.setValue(OBJECTIVE_STATE, state.name());
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    private static BetonQuestManager betonQuestManager(final NotQuests main) {
        return main.getIntegrationsManager().getBetonQuestManager();
    }

    public static boolean matchesStateChange(
            final ActiveObjective activeObjective,
            final ObjectiveState state,
            final String objectiveFullId) {
        if (!(activeObjective.getObjective() instanceof final DefinedObjective objective) || !objective.isType(TYPE)) {
            return false;
        }
        return objective.text(OBJECTIVE_STATE).equalsIgnoreCase(state.name())
                && (objective.text(PACKAGE_NAME) + "." + objective.text(OBJECTIVE_NAME))
                        .equalsIgnoreCase(objectiveFullId);
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.BetonQuestCompleteObjective.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%BETONQUESTOBJECTIVENAME%", objective.text(OBJECTIVE_NAME)));
    }
}
