package com.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArgumentType;
import com.notquests.paper.conversation.Conversation;
import com.notquests.paper.conversation.Speaker;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code SpeakerParser}: resolves a {@link Speaker} (by name) within the
 * {@link Conversation} carried by a prior positional argument named {@code conversationContext}.
 *
 * <p>NOTE: the Cloud parser read the conversation from the command context inside {@code parse}.
 * Brigadier does not hand {@code convert} a context, so resolution is delegated to
 * {@link #convert(CommandContext, String)}.
 */
public final class SpeakerArgument extends NQArgumentType<String> {
    private final NotQuests main;
    private final String conversationContext;

    public SpeakerArgument(final NotQuests main, final String conversationContext) {
        this.main = main;
        this.conversationContext = conversationContext;
    }

    public static SpeakerArgument speakerArgument(final NotQuests main, final String conversationContext) {
        return new SpeakerArgument(main, conversationContext);
    }

    @Override
    public String valueTypeName() {
        return "conversation speaker";
    }

    @Override
    public String convert(final String input) {
        return input; // raw speaker name; resolved by the handler via resolveSpeaker(conversation, name)
    }

    /** Finds a speaker by name within a conversation (the conversation comes from the handler's context). */
    public static Speaker resolveSpeaker(final Conversation conversation, final String name) {
        if (conversation == null || conversation.getSpeakers() == null) {
            return null;
        }
        for (final Speaker speaker : conversation.getSpeakers()) {
            if (speaker.getSpeakerName().equalsIgnoreCase(name)) {
                return speaker;
            }
        }
        return null;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        final Conversation conversation = (Conversation) context.getArgument(conversationContext, Object.class);
        if (conversation.getSpeakers() != null && conversation.getSpeakers().size() > 0) {
            final int speakerCount = conversation.getSpeakers().size();
            for (int i = 0; i < speakerCount; i++) {
                entries.add(conversation.getSpeakers().get(i).getSpeakerName());
            }
        }
        return entries;
    }
}
