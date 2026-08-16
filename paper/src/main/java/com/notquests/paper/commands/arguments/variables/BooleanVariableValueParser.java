package com.notquests.paper.commands.arguments.variables;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQSuggestionProvider;
import com.notquests.paper.variables.Variable;

@Getter
public class BooleanVariableValueParser<C> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    private NQSuggestionProvider suggestionProvider;

    protected BooleanVariableValueParser(String identifier, Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static <C> @NonNull BooleanVariableValueParser<C> of(String identifier, Variable<?> variable, NQSuggestionProvider suggestionProvider) {
        BooleanVariableValueParser<C> parser = new BooleanVariableValueParser<>(identifier, variable);
        parser.suggestionProvider = suggestionProvider;
        return parser;
    }

    public static <C> @NonNull BooleanVariableValueParser<C> of(String identifier, Variable<?> variable) {
        return new BooleanVariableValueParser<>(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }
}
