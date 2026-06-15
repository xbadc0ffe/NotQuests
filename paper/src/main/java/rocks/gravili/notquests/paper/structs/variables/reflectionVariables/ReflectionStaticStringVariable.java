package rocks.gravili.notquests.paper.structs.variables.reflectionVariables;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ReflectionStaticStringVariable extends Variable<String> {
    public ReflectionStaticStringVariable(NotQuests main) {
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
    public String getValueInternally(QuestPlayer questPlayer, Object... objects) {
        final String classPath = getRequiredStringValue("Class Path");
        final String fieldName = getRequiredStringValue("Field Name");

        try {
            Class<?> foundClass = Class.forName(classPath);

            Field field = foundClass.getDeclaredField(fieldName);
            field.setAccessible(true);

            return (String) field.get(null);
        } catch (Exception e) {
            main.getLogManager().warn("Reflection in ReflectionStaticStringVariable failed. Error: " + e.getMessage());
        }


        return null;
    }

    @Override
    public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
        final String classPath = getRequiredStringValue("Class Path");
        final String fieldName = getRequiredStringValue("Field Name");

        try {
            Class<?> foundClass = Class.forName(classPath);

            Field field = foundClass.getDeclaredField(fieldName);
            field.setAccessible(true);

            field.set(null, newValue);
            return true;
        } catch (Exception e) {
            main.getLogManager().warn("Reflection in ReflectionStaticStringVariable failed. Error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "String from static reflection";
    }

    @Override
    public String getSingular() {
        return "String from static reflection";
    }
}
