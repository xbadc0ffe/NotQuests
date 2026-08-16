package com.notquests.paper.builtin.actions;

import org.betonquest.betonquest.api.QuestException;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class BetonQuestFireEvent {
    private static final String PACKAGE = "package";
    private static final String ACTION = "action";

    private BetonQuestFireEvent() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("BetonQuestFireEvent")
                .displayName("BetonQuest Fire Event")
                .description("Runs a named BetonQuest action from a BetonQuest package.")
                .field(PACKAGE, FieldTypes.text().config("specifics.packageName"), "BetonQuest package containing the action to run.")
                .field(ACTION, FieldTypes.text().config("specifics.eventName"), "BetonQuest action name to run.")
                .commands((type, builder, actionFor) -> main.getCommandManager().getNQCommandManager().command(builder
                        .required(
                                PACKAGE,
                                NQArguments.stringArgument(),
                                NQDescription.of("BetonQuest package containing the action to run."),
                                (context, input) -> betonQuestManager(main).packageNames())
                        .required(
                                ACTION,
                                NQArguments.stringArgument(),
                                NQDescription.of("BetonQuest action name to run."),
                                (context, input) -> betonQuestManager(main).actionNames(context.get(PACKAGE)))
                        .handler(context -> {
                            final var action = type.createAction();
                            action.setValue(PACKAGE, context.get(PACKAGE));
                            action.setValue(ACTION, context.get(ACTION));
                            main.getActionCatalog().addAction(action, context, actionFor);
                        })))
                .singleLine((action, arguments) -> {
                    if (arguments.size() >= 2) {
                        action.setValue(PACKAGE, arguments.get(0));
                        action.setValue(ACTION, arguments.get(1));
                    }
                })
                .execute((action, questPlayer, objects) -> {
                    try {
                        betonQuestManager(main).runAction(questPlayer, action.text(PACKAGE), action.text(ACTION));
                    } catch (final QuestException exception) {
                        main.getLogManager().warn(
                                "Tried to execute BetonQuestFireEvent action, but BetonQuest could not run "
                                        + action.text(PACKAGE)
                                        + "."
                                        + action.text(ACTION)
                                        + ": "
                                        + exception.getMessage());
                    }
                })
                .actionDescription((action, questPlayer, objects) ->
                        "Executes BetonQuest action: " + action.text(PACKAGE) + "." + action.text(ACTION))
                .register();
    }

    private static BetonQuestManager betonQuestManager(final NotQuests main) {
        return main.getIntegrationsManager().getBetonQuestManager();
    }
}
