package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Entity;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbyEntityCountVariable extends Variable<Integer> {
    public NearbyEntityCountVariable(final NotQuests main) {
        super(main);

        addRequiredString(StringVariableValueParser.of("entityType", null, (context, input) -> {
            final List<String> suggestions = new ArrayList<>(main.getDataManager().standardEntityTypeCompletions);
            suggestions.add("any");
            return suggestions;
        }));
        addRequiredNumber(NumberVariableValueParser.of("radius", null));
    }

    @Override
    public Integer getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return 0;
        }

        final String entityType = getRequiredStringValue("entityType");
        final double radius = Math.max(0, getRequiredNumberValue("radius", questPlayer));
        int count = 0;
        for (final Entity entity : questPlayer.getPlayer().getWorld().getNearbyEntities(
                questPlayer.getPlayer().getLocation(), radius, radius, radius)) {
            if (entity.equals(questPlayer.getPlayer())) {
                continue;
            }
            if (entityType.equalsIgnoreCase("any")
                    || entity.getType().name().equalsIgnoreCase(entityType)
                    || entity.getType().getKey().getKey().equalsIgnoreCase(entityType.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean setValueInternally(final Integer newValue, final QuestPlayer questPlayer, final Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        return List.of("0", "1", "3", "5", "10", "25");
    }

    @Override
    public String getPlural() {
        return "Nearby Entity Counts";
    }

    @Override
    public String getSingular() {
        return "Nearby Entity Count";
    }
}
