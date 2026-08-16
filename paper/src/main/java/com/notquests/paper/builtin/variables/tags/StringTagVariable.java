package com.notquests.paper.builtin.variables.tags;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.managers.tags.Tag;
import com.notquests.paper.managers.tags.TagType;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.variables.Variable;

import java.util.ArrayList;
import java.util.List;

public class StringTagVariable extends Variable<String> {

    public StringTagVariable(NotQuests main) {
        super(main);

        addRequiredString(StringVariableValueParser.of("TagName", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            for (final Tag tag : main.getTagManager().getTags()) {
                if (tag.getTagType() == TagType.STRING) {
                    suggestions.add(tag.getTagName());
                }
            }
            return suggestions;
        }));

        setCanSetValue(true);
    }

    @Override
    public final String getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return "";
        }

        final String tagName = getRequiredStringValue("TagName");
        final Tag tag = main.getTagManager().getTag(tagName);
        if (tag == null) {
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag does not exist.");
            return "";
        }
        if(tag.getTagType() != TagType.STRING){
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag is no string tag.");
            return "";
        }

        final Object value = questPlayer.getTagValue(tagName);

        if (value instanceof final String stringValue) {
            return stringValue;
        } else {
            return "";
        }

    }

    @Override
    public boolean setValueInternally(final String newValue, final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return false;
        }

        final String tagName = getRequiredStringValue("TagName");
        final Tag tag = main.getTagManager().getTag(tagName);
        if (tag == null) {
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag does not exist.");
            return false;
        }
        if(tag.getTagType() != TagType.STRING){
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag is no string tag.");
            return false;
        }


        questPlayer.setTagValue(tagName, newValue);

        return true;
    }


    @Override
    public final List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        return null;
    }

    @Override
    public final String getPlural() {
        return "Tags";
    }

    @Override
    public final String getSingular() {
        return "Tag";
    }
}
