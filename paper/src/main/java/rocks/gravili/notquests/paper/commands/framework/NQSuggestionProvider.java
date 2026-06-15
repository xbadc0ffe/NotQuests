package rocks.gravili.notquests.paper.commands.framework;

import java.util.List;

/**
 * Supplies tab-completions for an argument. Our replacement for Cloud's {@code SuggestionProvider}.
 * Returned values are filtered by the partial token automatically.
 */
@FunctionalInterface
public interface NQSuggestionProvider {
    List<String> suggest(NQCommandContext context, String input);
}
