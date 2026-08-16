package com.notquests.paper.builtin.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.event.entity.CreatureSpawnEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.NumberVariableArgument;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveType;
import com.notquests.paper.structs.ActiveObjective;

public final class KillEliteMobs {
    private static final String TYPE = "KillEliteMobs";
    private static final String AMOUNT = "amount";
    private static final String MOB_NAME = "mobname";
    private static final String MINIMUM_LEVEL = "minimumLevel";
    private static final String MAXIMUM_LEVEL = "maximumLevel";
    private static final String SPAWN_REASON = "spawnReason";
    private static final String MINIMUM_DAMAGE_PERCENTAGE = "minimumDamagePercentage";

    private KillEliteMobs() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        if (!main.getIntegrationsManager().isEliteMobsEnabled()) {
            return;
        }
        objectives.objective(TYPE)
                .displayName("Kill EliteMobs")
                .description("Counts EliteMobs kills matching optional mob name, level, spawn reason, and damage-share filters.")
                .field(
                        AMOUNT,
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Number of matching EliteMobs the player must kill.")
                .field(
                        MOB_NAME,
                        FieldTypes.text().config("specifics.eliteMobToKill"),
                        "EliteMobs mob name that counts for this objective, or blank for any EliteMob.")
                .field(
                        MINIMUM_LEVEL,
                        FieldTypes.storedInteger(-1).config("specifics.minimumLevel"),
                        "Minimum EliteMobs level that counts, or -1 for no minimum.")
                .field(
                        MAXIMUM_LEVEL,
                        FieldTypes.storedInteger(-1).config("specifics.maximumLevel"),
                        "Maximum EliteMobs level that counts, or -1 for no maximum.")
                .field(
                        SPAWN_REASON,
                        FieldTypes.text().config("specifics.spawnReason"),
                        "Bukkit spawn reason that counts, or blank for any spawn reason.")
                .field(
                        MINIMUM_DAMAGE_PERCENTAGE,
                        FieldTypes.storedInteger(-1).config("specifics.minimumDamagePercentage"),
                        "Minimum percent of total damage the player must deal, or -1 for no minimum.")
                .commands((type, builder, level) -> registerCommands(main, type, builder, level))
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandBuilder builder,
            final int level) {
        final NQFlag mobName = NQFlag.builder(MOB_NAME, NQDescription.of("EliteMobs mob name that counts for this objective, or any."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final List<String> completions = new ArrayList<>();
                    completions.add("any");
                    completions.addAll(main.getDataManager().standardEliteMobNamesCompletions);
                    return completions;
                })
                .build();
        final NQFlag minimumLevel = NQFlag.builder(MINIMUM_LEVEL, NQDescription.of("Minimum EliteMobs level that counts for this objective."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> main.getDataManager().numberPositiveCompletions)
                .build();
        final NQFlag maximumLevel = NQFlag.builder(MAXIMUM_LEVEL, NQDescription.of("Maximum EliteMobs level that counts for this objective."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> main.getDataManager().numberPositiveCompletions)
                .build();
        final NQFlag spawnReason = NQFlag.builder(SPAWN_REASON, NQDescription.of("Bukkit spawn reason that counts for this EliteMobs kill objective, or any."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final List<String> completions = new ArrayList<>();
                    completions.add("any");
                    for (final CreatureSpawnEvent.SpawnReason value : CreatureSpawnEvent.SpawnReason.values()) {
                        completions.add(value.toString());
                    }
                    return completions;
                })
                .build();
        final NQFlag minimumDamagePercentage = NQFlag.builder(
                        MINIMUM_DAMAGE_PERCENTAGE,
                        NQDescription.of("Minimum percent of total damage the player must deal for the kill to count."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final List<String> completions = new ArrayList<>();
                    for (int i = 50; i <= 100; i++) {
                        completions.add(String.valueOf(i));
                    }
                    return completions;
                })
                .build();

        main.getCommandManager().getNQCommandManager().command(builder
                .required(
                        AMOUNT,
                        NumberVariableArgument.numberVariableArgument(AMOUNT, null, false),
                        NQDescription.of("Number of matching EliteMobs the player must kill."))
                .flag(mobName)
                .flag(minimumLevel)
                .flag(maximumLevel)
                .flag(spawnReason)
                .flag(minimumDamagePercentage)
                .handler(context -> addObjective(
                        main,
                        type,
                        context,
                        level,
                        mobName,
                        minimumLevel,
                        maximumLevel,
                        spawnReason,
                        minimumDamagePercentage)));
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level,
            final NQFlag mobNameFlag,
            final NQFlag minimumLevelFlag,
            final NQFlag maximumLevelFlag,
            final NQFlag spawnReasonFlag,
            final NQFlag minimumDamagePercentageFlag) {
        final String amount = context.get(AMOUNT);
        final DefinedObjective objective = type.createObjective();
        objective.setValue(AMOUNT, amount);
        objective.setProgressNeededExpression(amount);
        objective.setValue(MOB_NAME, normalizeAny(context.flags().getValue(mobNameFlag, "")));
        objective.setValue(MINIMUM_LEVEL, parseOptionalInteger(context.flags().getValue(minimumLevelFlag, "any")));
        objective.setValue(MAXIMUM_LEVEL, parseOptionalInteger(context.flags().getValue(maximumLevelFlag, "any")));
        objective.setValue(SPAWN_REASON, normalizeAny(context.flags().getValue(spawnReasonFlag, "")));
        objective.setValue(
                MINIMUM_DAMAGE_PERCENTAGE,
                parseOptionalInteger(context.flags().getValue(minimumDamagePercentageFlag, "any").replace("%", "")));
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    public static boolean countsEliteMobKill(
            final ActiveObjective activeObjective,
            final String eliteMobName,
            final int eliteMobLevel,
            final double playerDamage,
            final double maxHealth,
            final String spawnReason) {
        if (!(activeObjective.getObjective() instanceof final DefinedObjective objective) || !objective.isType(TYPE)) {
            return false;
        }
        final String configuredName = objective.text(MOB_NAME);
        if (!configuredName.isBlank()) {
            for (final String namePart : configuredName.toLowerCase(Locale.ROOT).split(" ")) {
                if (!eliteMobName.toLowerCase(Locale.ROOT).contains(namePart)) {
                    return false;
                }
            }
        }
        final Integer minimumLevel = objective.value(MINIMUM_LEVEL, Integer.class);
        if (minimumLevel != null && minimumLevel >= 0 && eliteMobLevel < minimumLevel) {
            return false;
        }
        final Integer maximumLevel = objective.value(MAXIMUM_LEVEL, Integer.class);
        if (maximumLevel != null && maximumLevel >= 0 && eliteMobLevel > maximumLevel) {
            return false;
        }
        final Integer minimumDamagePercentage = objective.value(MINIMUM_DAMAGE_PERCENTAGE, Integer.class);
        if (minimumDamagePercentage != null
                && minimumDamagePercentage >= 0
                && maxHealth > 0
                && (playerDamage / maxHealth) * 100 < minimumDamagePercentage) {
            return false;
        }
        final String configuredSpawnReason = objective.text(SPAWN_REASON);
        return configuredSpawnReason.isBlank() || configuredSpawnReason.equalsIgnoreCase(spawnReason);
    }

    private static String normalizeAny(final String value) {
        if (value == null || value.equalsIgnoreCase("any")) {
            return "";
        }
        return value.replace("_", " ");
    }

    private static int parseOptionalInteger(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException ignored) {
            return -1;
        }
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final ActiveObjective activeObjective) {
        String description;
        final String mobName = objective.text(MOB_NAME);
        if (!mobName.isBlank()) {
            description = main.getLanguageManager()
                    .getString(
                            "chat.objectives.taskDescription.killEliteMobs.base",
                            questPlayer,
                            activeObjective,
                            Map.of("%ELITEMOBNAME%", mobName));
        } else {
            description = main.getLanguageManager()
                    .getString("chat.objectives.taskDescription.killEliteMobs.any", questPlayer, activeObjective);
        }

        final Integer minimumLevel = objective.value(MINIMUM_LEVEL, Integer.class);
        final Integer maximumLevel = objective.value(MAXIMUM_LEVEL, Integer.class);
        if (minimumLevel != null && minimumLevel != -1) {
            if (maximumLevel != null && maximumLevel != -1) {
                description += "\n        <GRAY>Level: <WHITE>" + minimumLevel + "-" + maximumLevel;
            } else {
                description += "\n        <GRAY>Minimum Level: <WHITE>" + minimumLevel;
            }
        } else if (maximumLevel != null && maximumLevel != -1) {
            description += "\n        <GRAY>Maximum Level: <WHITE>" + maximumLevel;
        }
        final String spawnReason = objective.text(SPAWN_REASON);
        if (!spawnReason.isBlank()) {
            description += "\n        <GRAY>Spawned from: <WHITE>" + spawnReason;
        }
        final Integer minimumDamagePercentage = objective.value(MINIMUM_DAMAGE_PERCENTAGE, Integer.class);
        if (minimumDamagePercentage != null && minimumDamagePercentage != -1) {
            description += "\n        <GRAY>Inflict minimum damage: <WHITE>" + minimumDamagePercentage + "%";
        }
        return description;
    }
}
