package rocks.gravili.notquests.paper.structs.variables.tags;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.managers.tags.TagType;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;

public class DoubleTagVariable extends Variable<Double> {

    public DoubleTagVariable(final NotQuests main) {
        super(main);

        addRequiredString(StringVariableValueParser.of("TagName", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            for (final Tag tag : main.getTagManager().getTags()) {
                if (tag.getTagType() == TagType.DOUBLE) {
                    suggestions.add(tag.getTagName());
                }
            }
            return suggestions;
        }));
        setCanSetValue(true);
    }

    @Override
    public Double getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return 0d;
        }

        final String tagName = getRequiredStringValue("TagName");
        final Tag tag = main.getTagManager().getTag(tagName);
        if (tag == null) {
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag does not exist.");
            return 0d;
        }
        if (tag.getTagType() != TagType.DOUBLE) {
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag is no double tag.");
            return 0d;
        }

        final Object value = questPlayer.getTagValue(tagName);

        if (value instanceof final Double doubleValue) {
            return doubleValue;
        } else {
            return 0d;
        }

    }

    @Override
    public boolean setValueInternally(final Double newValue, final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return false;
        }

        final String tagName = getRequiredStringValue("TagName");
        final Tag tag = main.getTagManager().getTag(tagName);
        if (tag == null) {
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag does not exist.");
            return false;
        }
        if (tag.getTagType() != TagType.DOUBLE) {
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag is no double tag.");
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
