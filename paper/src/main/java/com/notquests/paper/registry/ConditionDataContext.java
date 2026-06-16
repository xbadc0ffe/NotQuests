package com.notquests.paper.registry;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.TimeZone;
import com.notquests.paper.commands.arguments.ActionList;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.structs.Quest;

public record ConditionDataContext(DefinedCondition condition) {
    public String text(final String name) {
        return condition.text(name);
    }

    public int integer(final String name) {
        return condition.value(name, 0);
    }

    public int integer(final String name, final int fallback) {
        return condition.value(name, fallback);
    }

    public boolean flag(final String name) {
        return Boolean.TRUE.equals(condition.value(name, Boolean.class));
    }

    public Quest quest(final String name) {
        return condition.value(name, Quest.class);
    }

    public LocalDateTime dateTime(final String name) {
        return condition.value(name, LocalDateTime.class);
    }

    public TimeZone timeZone(final String name) {
        return condition.value(name, TimeZone.class);
    }

    public ItemStackSelection itemSelection(final String name) {
        return condition.value(name, ItemStackSelection.class);
    }

    public ActionList actionList(final String name) {
        return condition.value(name, ActionList.class);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, String> stringMap(final String name) {
        final Object value = condition.value(name, Object.class);
        return value instanceof HashMap<?, ?> map ? (HashMap<String, String>) map : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, NumberExpression> expressionMap(final String name) {
        final Object value = condition.value(name, Object.class);
        return value instanceof HashMap<?, ?> map ? (HashMap<String, NumberExpression>) map : new HashMap<>();
    }
}
