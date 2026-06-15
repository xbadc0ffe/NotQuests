package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.conversation.Conversation;

import java.util.ArrayList;
import java.util.List;

/** Native-framework port of {@code ConversationParser}: resolves a {@link Conversation} by identifier. */
public final class ConversationArgument extends NQArgumentType<Conversation> {
    private final NotQuests main;

    public ConversationArgument(final NotQuests main) {
        this.main = main;
    }

    public static ConversationArgument conversationArgument(final NotQuests main) {
        return new ConversationArgument(main);
    }

    @Override
    public String valueTypeName() {
        return "conversation name";
    }

    @Override
    public Conversation convert(final String input) throws CommandSyntaxException {
        for (final Conversation conversation : main.getConversationManager().getAllConversations()) {
            if (conversation.getIdentifier().equalsIgnoreCase(input)) {
                return conversation;
            }
        }
        throw fail("No Conversation found: " + input);
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        for (final Conversation conversation : main.getConversationManager().getAllConversations()) {
            entries.add(conversation.getIdentifier());
        }
        return entries;
    }
}
