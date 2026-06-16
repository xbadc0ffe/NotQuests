package com.notquests.paper.builtin.variables.reflection;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.variables.Variable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ReflectionStaticBooleanVariable extends Variable<Boolean> {
    public ReflectionStaticBooleanVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);

        addRequiredString(StringVariableValueParser.of("Class Path", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            suggestions.add("<Enter class path>");
            return suggestions;
        }));

        addRequiredString(StringVariableValueParser.of("Field", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            suggestions.add("<Enter field name>");
            return suggestions;
        }));
    }

    @Override
    public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
        final String classPath = getRequiredStringValue("Class Path");
        final String fieldName = getRequiredStringValue("Field Name");

        try {
            Class<?> foundClass = Class.forName(classPath);

            Field field = foundClass.getDeclaredField(fieldName);
            field.setAccessible(true);

            return field.getBoolean(null);
        } catch (Exception e) {
            main.getLogManager().warn("Reflection in ReflectionStaticBooleanVariable failed. Error: " + e.getMessage());
        }


        return null;
    }

    @Override
    public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
        final String classPath = getRequiredStringValue("Class Path");
        final String fieldName = getRequiredStringValue("Field Name");

        try {
            Class<?> foundClass = Class.forName(classPath);

            Field field = foundClass.getDeclaredField(fieldName);
            field.setAccessible(true);

            field.setBoolean(null, newValue);
            return true;
        } catch (Exception e) {
            main.getLogManager().warn("Reflection in ReflectionStaticBooleanVariable failed. Error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Boolean from static reflection";
    }

    @Override
    public String getSingular() {
        return "Boolean from static reflection";
    }
}
