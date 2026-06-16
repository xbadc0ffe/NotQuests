package com.notquests.paper.builtin.actions;

import java.util.Locale;
import com.notquests.paper.NotQuests;
import com.notquests.paper.conversation.Conversation;
import com.notquests.paper.conversation.ConversationPlayer;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class StartConversation {
    private static final String CONVERSATION = "conversation";
    private static final String END_PREVIOUS = "endPrevious";

    private StartConversation() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("StartConversation")
                .displayName("Start Conversation")
                .description("Starts a NotQuests conversation for the target player.")
                .field(
                        CONVERSATION,
                        FieldTypes.conversationName().config("specifics.conversation"),
                        "Conversation that should start for the target player.")
                .flag(
                        END_PREVIOUS,
                        FieldTypes.presenceFlag().config("specifics.endPrevious"),
                        "Ends the player's currently open conversation before starting this one.")
                .singleLine((action, arguments) -> {
                    action.setValue(CONVERSATION, arguments.get(0));
                    action.setValue(END_PREVIOUS, String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--endprevious"));
                })
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null) {
                        return;
                    }
                    final Conversation conversation =
                            main.getConversationManager().getConversation(action.text(CONVERSATION));
                    if (conversation == null) {
                        main.getLogManager().warn(
                                "Tried to execute StartConversation action with unknown conversation: "
                                        + action.text(CONVERSATION));
                        return;
                    }
                    final ConversationPlayer openConversation =
                            main.getConversationManager().getOpenConversation(questPlayer.getUniqueId());
                    if (action.flag(END_PREVIOUS) && openConversation != null) {
                        main.getConversationManager().stopConversation(openConversation);
                    }
                    main.getConversationManager().playConversation(questPlayer, conversation, null);
                })
                .actionDescription((action, questPlayer, objects) -> "Starts conversation: " + action.text(CONVERSATION))
                .register();
    }
}
