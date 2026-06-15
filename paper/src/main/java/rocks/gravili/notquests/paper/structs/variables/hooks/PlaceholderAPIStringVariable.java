package rocks.gravili.notquests.paper.structs.variables.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderAPIStringVariable extends Variable<String> {

    public PlaceholderAPIStringVariable(NotQuests main) {
        super(main);
        addRequiredString(StringVariableValueParser.of("Placeholder", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            for (String identifier : PlaceholderAPI.getRegisteredIdentifiers()) {
                suggestions.add("%" + identifier + "_");
            }

            return suggestions;
        }));
    }

    @Override
    public String getValueInternally(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return PlaceholderAPI.setPlaceholders(
                    questPlayer.getPlayer(), getRequiredStringValue("Placeholder"));
        } else {
            return "";
        }
    }

    @Override
    public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return getRequiredStringValue("Placeholder");
    }

    @Override
    public String getSingular() {
        return getRequiredStringValue("Placeholder");
    }
}
