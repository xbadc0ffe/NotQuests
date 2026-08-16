package com.notquests.paper.builtin.actions;

import java.util.List;
import org.betonquest.betonquest.api.QuestException;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class BetonQuestFireInlineEvent {
    private static final String ACTION = "action";

    private BetonQuestFireInlineEvent() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("BetonQuestFireInlineEvent")
                .displayName("BetonQuest Fire Inline Event")
                .description("Runs an inline BetonQuest action instruction.")
                .field(ACTION, FieldTypes.greedyText().config("specifics.event"), "Inline BetonQuest action instruction to run.")
                .commands((type, builder, actionFor) -> main.getCommandManager().getNQCommandManager().command(builder
                        .required(
                                ACTION,
                                NQArguments.greedyStringArgument(),
                                NQDescription.of("Inline BetonQuest action instruction to run."),
                                (context, input) -> input.contains(" ") ? List.of() : betonQuestManager(main).actionTypes())
                        .handler(context -> {
                            final var action = type.createAction();
                            action.setValue(ACTION, context.get(ACTION));
                            main.getActionCatalog().addAction(action, context, actionFor);
                        })))
                .singleLine((action, arguments) -> action.setValue(ACTION, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    try {
                        betonQuestManager(main).runInlineAction(questPlayer, action.text(ACTION));
                    } catch (final QuestException exception) {
                        main.getLogManager().warn(
                                "Tried to execute BetonQuestFireInlineEvent action, but BetonQuest could not run '"
                                        + action.text(ACTION)
                                        + "': "
                                        + exception.getMessage());
                    }
                })
                .actionDescription((action, questPlayer, objects) ->
                        "Executes inline BetonQuest action: " + action.text(ACTION))
                .register();
    }

    private static BetonQuestManager betonQuestManager(final NotQuests main) {
        return main.getIntegrationsManager().getBetonQuestManager();
    }
}
