package rocks.gravili.notquests.paper.structs.objectives;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.damage.DamageType;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class DieObjective extends Objective {
    private String damageType = "";

    public DieObjective(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder addObjectiveBuilder,
            final int level) {
        final NQFlag causeFlag = NQFlag.builder(
                        "cause",
                        NQDescription.of("Optional death damage type that must cause the death, such as fall, lava, drown, or player_attack."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> damageTypeSuggestions())
                .build();

        manager.command(addObjectiveBuilder
                .required("amount", numberVariableArgument("amount", null, false), NQDescription.of("Amount of times the player needs to die."))
                .flag(causeFlag)
                .handler(context -> {
                    final DieObjective objective = new DieObjective(main);
                    objective.setProgressNeededExpression(context.get("amount"));
                    objective.setDamageType(context.flags().getValue("cause", ""));
                    main.getObjectiveManager().addObjective(objective, context, level);
                }));
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(final String damageType) {
        this.damageType = damageType == null ? "" : damageType;
    }

    public boolean countsDamageType(final String deathDamageType) {
        return damageType.isBlank() || damageType.equalsIgnoreCase(deathDamageType);
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer,
            final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString("chat.objectives.taskDescription.die.base", questPlayer, activeObjective)
                .replace("%DAMAGETYPE%", damageType.isBlank() ? "any" : damageType);
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        if (!damageType.isBlank()) {
            configuration.set(initialPath + ".specifics.damageType", damageType);
        }
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        damageType = configuration.getString(initialPath + ".specifics.damageType", "");
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }

    private static List<String> damageTypeSuggestions() {
        final List<String> completions = new ArrayList<>();
        final var damageTypeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE);
        for (final DamageType type : damageTypeRegistry) {
            completions.add(damageTypeRegistry.getKeyOrThrow(type).getKey());
        }
        return completions;
    }
}
