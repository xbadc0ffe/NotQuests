package com.notquests.paper.commands.arguments.variables;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQSuggestionProvider;
import com.notquests.paper.variables.Variable;

@Getter
public class ItemStackListVariableValueParser<C> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    private NQSuggestionProvider suggestionProvider;

    protected ItemStackListVariableValueParser(String identifier, Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static <C> @NonNull ItemStackListVariableValueParser<C> of(String identifier, Variable<?> variable, NQSuggestionProvider suggestionProvider) {
        ItemStackListVariableValueParser<C> parser = new ItemStackListVariableValueParser<>(identifier, variable);
        parser.suggestionProvider = suggestionProvider;
        return parser;
    }

    public static <C> @NonNull ItemStackListVariableValueParser<C> of(String identifier, Variable<?> variable) {
        return new ItemStackListVariableValueParser<>(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }
}
