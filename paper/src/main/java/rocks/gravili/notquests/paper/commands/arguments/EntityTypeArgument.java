package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code EntityTypeParser}: validates an entity-type / mob name (optionally
 * including MythicMobs faction names) against the known completions, or the literal {@code ANY}.
 */
public final class EntityTypeArgument extends NQArgumentType<String> {
    private final NotQuests main;
    private final boolean mythicMobsFactions;

    public EntityTypeArgument(final NotQuests main, final boolean mythicMobsFactions) {
        this.main = main;
        this.mythicMobsFactions = mythicMobsFactions;
    }

    public static EntityTypeArgument entityTypeArgument(final NotQuests main) {
        return new EntityTypeArgument(main, false);
    }

    public static EntityTypeArgument entityTypeArgument(final NotQuests main, final boolean mythicMobsFactions) {
        return new EntityTypeArgument(main, mythicMobsFactions);
    }

    @Override
    public String valueTypeName() {
        return mythicMobsFactions ? "entity type, any, or MythicMobs faction" : "entity type or any";
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        if (mythicMobsFactions
                && main.getIntegrationsManager().isMythicMobsEnabled()
                && main.getIntegrationsManager().getMythicMobsManager() != null) {
            if (main.getIntegrationsManager().getMythicMobsManager().getFactionNames("mmfaction:").contains(input)) {
                return input;
            }
        }

        if (!main.getDataManager().standardEntityTypeCompletions.contains(input)
                && !input.equalsIgnoreCase("ANY")) {
            throw fail("Entity type '" + input + "' does not exist!");
        }

        return input;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        // NOTE: stream().toList() is immutable; wrap in ArrayList so the .add()/.addAll() below work.
        final List<String> completions = new ArrayList<>(main.getDataManager().standardEntityTypeCompletions);
        completions.add("any");

        // Add extra Mythic Mobs completions, if enabled
        if (mythicMobsFactions
                && main.getIntegrationsManager().isMythicMobsEnabled()
                && main.getIntegrationsManager().getMythicMobsManager() != null) {
            completions.addAll(main.getIntegrationsManager().getMythicMobsManager().getFactionNames("mmfaction:"));
        }
        return completions;
    }
}
