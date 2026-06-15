package rocks.gravili.notquests.paper.commands.arguments.variables;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQSuggestionProvider;
import rocks.gravili.notquests.paper.structs.variables.Variable;

@Getter
public class ListVariableValueParser<C> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    private NQSuggestionProvider suggestionProvider;

    protected ListVariableValueParser(String identifier, Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static <C> @NonNull ListVariableValueParser<C> of(String identifier, Variable<?> variable, NQSuggestionProvider suggestionProvider) {
        ListVariableValueParser<C> parser = new ListVariableValueParser<>(identifier, variable);
        parser.suggestionProvider = suggestionProvider;
        return parser;
    }

    public static <C> @NonNull ListVariableValueParser<C> of(String identifier, Variable<?> variable) {
        return new ListVariableValueParser<>(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }
}
