package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PermissionVariable extends Variable<Boolean> {
    public PermissionVariable(NotQuests main) {
        super(main);
        if (main.getIntegrationsManager().isLuckpermsEnabled()) {
            setCanSetValue(true);
        }

        addRequiredString(StringVariableValueParser.of("Permission", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            suggestions.add("<Enter Permission node>");
            return suggestions;
        }));
    }

    @Override
    public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
        return questPlayer != null
                && questPlayer.getPlayer().hasPermission(getRequiredStringValue("Permission"));
    }

    @Override
    public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isLuckpermsEnabled()) {
            return false;
        }

        if (newValue) {
            main.getIntegrationsManager()
                    .getLuckPermsManager()
                    .givePermission(questPlayer.getUniqueId(), getRequiredStringValue("Permission"));
        } else {
            main.getIntegrationsManager()
                    .getLuckPermsManager()
                    .denyPermission(questPlayer.getUniqueId(), getRequiredStringValue("Permission"));
        }

        return true;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Permissions";
    }

    @Override
    public String getSingular() {
        return "Permission";
    }
}
