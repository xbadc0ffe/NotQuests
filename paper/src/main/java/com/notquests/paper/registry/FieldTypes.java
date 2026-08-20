package com.notquests.paper.registry;

import java.util.HashMap;
import java.util.Locale;
import java.time.Duration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.commands.arguments.ActionList;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.commands.arguments.variables.NumberVariableArgument;
import com.notquests.paper.commands.framework.NQArgumentType;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQSuggestionProvider;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.managers.npc.NQNPC;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.actions.Action;
import com.notquests.paper.conditions.Condition;

import static com.notquests.paper.commands.arguments.CommandArgument.commandArgument;
import static com.notquests.paper.commands.arguments.ConditionArgument.conditionArgument;
import static com.notquests.paper.commands.arguments.ConversationArgument.conversationArgument;
import static com.notquests.paper.commands.arguments.EnchantmentArgument.enchantmentArgument;
import static com.notquests.paper.commands.arguments.EntityTypeArgument.entityTypeArgument;
import static com.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;
import static com.notquests.paper.commands.arguments.MultiActionsArgument.multiActionsArgument;
import static com.notquests.paper.commands.arguments.QuestArgument.questArgument;

/** Public field factories for definition-based objectives, actions, conditions, and variables. */
public final class FieldTypes {
    private FieldTypes() {}

    public static FieldType<String> text() {
        return new FieldType<>(
                (main, name) -> NQArguments.stringArgument(),
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "text",
                "");
    }

    public static FieldType<String> text(final NQSuggestionProvider suggestions) {
        return new FieldType<>(
                (main, name) -> new NQArgumentType<>() {
                    @Override
                    public String convert(final String input) {
                        return input;
                    }

                    @Override
                    protected java.util.List<String> suggest(
                            final com.mojang.brigadier.context.CommandContext<?> context,
                            final String remaining) {
                        return suggestions.suggest(null, remaining);
                    }
                },
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "text",
                "");
    }

    public static FieldType<String> greedyText() {
        return new FieldType<>(
                (main, name) -> NQArguments.greedyStringArgument(),
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "text",
                "");
    }

    public static FieldType<String> commandText() {
        return new FieldType<>(
                (main, name) -> commandArgument(main),
                new FieldType.ConfigCodec<>() {
                    @Override
                    public String load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final String fallback) {
                        return configuration.getString(path, fallback);
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        final String command = value == null ? "" : value.toString();
                        configuration.set(path, command.startsWith("/") ? command : "/" + command);
                    }
                },
                "command text",
                "");
    }

    public static FieldType<Location> storedLocation() {
        return new FieldType<>(
                null,
                (main, configuration, path, fallback) -> configuration.getLocation(path, fallback),
                "location",
                null);
    }

    public static FieldType<Double> storedNumber(final double fallback) {
        return new FieldType<>(
                null,
                (main, configuration, path, currentFallback) -> configuration.getDouble(path, currentFallback),
                "number",
                fallback);
    }

    public static FieldType<Integer> storedInteger(final int fallback) {
        return new FieldType<>(
                null,
                (main, configuration, path, currentFallback) -> configuration.getInt(path, currentFallback),
                "whole number",
                fallback);
    }

    public static FieldType<Quest> quest() {
        return new FieldType<>(
                (main, name) -> questArgument(main),
                new FieldType.ConfigCodec<>() {
                    @Override
                    public Quest load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Quest fallback) {
                        final String questName = configuration.getString(path);
                        return questName == null ? fallback : main.getQuestManager().getQuest(questName);
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(path, value instanceof Quest quest ? quest.getIdentifier() : null);
                    }
                },
                "quest",
                null);
    }

    public static FieldType<String> questName() {
        return new FieldType<>(
                (main, name) -> new NQArgumentType<>() {
                    @Override
                    public String valueTypeName() {
                        return "quest name";
                    }

                    @Override
                    public String convert(final String input)
                            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                        return questArgument(main).convert(input).getIdentifier();
                    }

                    @Override
                    protected java.util.List<String> suggest(
                            final com.mojang.brigadier.context.CommandContext<?> context,
                            final String remaining) {
                        return main.getQuestManager().getAllQuests().stream()
                                .map(Quest::getIdentifier)
                                .toList();
                    }
                },
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "quest name",
                "");
    }

    public static FieldType<String> conversationName() {
        return new FieldType<>(
                (main, name) -> new NQArgumentType<>() {
                    @Override
                    public String valueTypeName() {
                        return "conversation name";
                    }

                    @Override
                    public String convert(final String input)
                            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                        return conversationArgument(main).convert(input).getIdentifier();
                    }

                    @Override
                    protected java.util.List<String> suggest(
                            final com.mojang.brigadier.context.CommandContext<?> context,
                            final String remaining) {
                        return main.getConversationManager().getAllConversations().stream()
                                .map(com.notquests.paper.conversation.Conversation::getIdentifier)
                                .toList();
                    }
                },
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "conversation name",
                "");
    }

    public static FieldType<Condition> condition() {
        return new FieldType<>(
                (main, name) -> conditionArgument(main),
                new FieldType.ConfigCodec<>() {
                    @Override
                    public Condition load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Condition fallback) {
                        final String conditionName = configuration.getString(path);
                        return conditionName == null
                                ? fallback
                                : main.getSavedConditions().getCondition(conditionName);
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(
                                path,
                                value instanceof Condition condition ? condition.getConditionName() : null);
                    }
                },
                "condition",
                null);
    }

    public static FieldType<ActionList> actionList() {
        return new FieldType<>(
                (main, name) -> multiActionsArgument(main),
                new FieldType.ConfigCodec<>() {
                    @Override
                    public ActionList load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final ActionList fallback) {
                        final ActionList actions = new ActionList();
                        final java.util.List<String> names;
                        if (configuration.contains(path)) {
                            names = configuration.getStringList(path);
                        } else {
                            final String singular = configuration.getString(path.substring(0, path.lastIndexOf('.')) + ".action");
                            names = singular == null || singular.isBlank()
                                    ? java.util.List.of()
                                    : java.util.List.of(singular);
                        }
                        for (final String actionName : names) {
                            final Action action = main.getSavedActions().getAction(actionName);
                            if (action != null) {
                                actions.addValue(action);
                            } else {
                                main.getLogManager().warn("Action list references unknown action '" + actionName + "'.");
                            }
                        }
                        return actions;
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        if (!(value instanceof ActionList actionList)) {
                            configuration.set(path, java.util.List.of());
                            return;
                        }
                        configuration.set(
                                path,
                                actionList.getValues().stream().map(Action::getActionName).toList());
                    }
                },
                "comma-separated saved action names",
                new ActionList());
    }

    public static FieldType<String> entityType() {
        return new FieldType<>(
                (main, name) -> entityTypeArgument(main, false),
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "entity type",
                "any");
    }

    public static FieldType<String> numberExpression() {
        return numberExpression(true);
    }

    public static FieldType<String> numberExpression(final boolean greedy) {
        return new FieldType<>(
                (main, name) -> NumberVariableArgument.numberVariableArgument(name, null, greedy),
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "number expression",
                "1");
    }

    public static FieldType<String> optionalNumberExpression() {
        return new FieldType<>(
                (main, name) -> NumberVariableArgument.numberVariableArgument(name, null, true),
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "number expression",
                null);
    }

    public static FieldType<Integer> integer(final int fallback) {
        return new FieldType<>(
                (main, name) -> NQArguments.integerArgument(),
                (main, configuration, path, currentFallback) -> configuration.getInt(path, currentFallback),
                "whole number",
                fallback);
    }

    public static FieldType<Double> doubleNumber(final double fallback) {
        return new FieldType<>(
                (main, name) -> NQArguments.doubleArgument(),
                (main, configuration, path, currentFallback) -> configuration.getDouble(path, currentFallback),
                "number",
                fallback);
    }

    public static FieldType<Double> optionalDouble() {
        return new FieldType<>(
                (main, name) -> NQArguments.doubleArgument(),
                new FieldType.ConfigCodec<>() {
                    @Override
                    public Double load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Double fallback) {
                        return configuration.contains(path) ? configuration.getDouble(path) : null;
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(path, value);
                    }
                },
                "number",
                null);
    }

    public static FieldType<Duration> duration(final Duration fallback) {
        return new FieldType<>(
                (main, name) -> NQArguments.durationArgument(),
                new FieldType.ConfigCodec<>() {
                    @Override
                    public Duration load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Duration currentFallback) {
                        return Duration.ofMillis(configuration.getLong(path, currentFallback.toMillis()));
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(path, value instanceof Duration duration ? duration.toMillis() : fallback.toMillis());
                    }
                },
                "duration",
                fallback);
    }

    public static FieldType<Enchantment> enchantment() {
        return new FieldType<>(
                (main, name) -> enchantmentArgument(),
                new FieldType.ConfigCodec<>() {
                    @Override
                    public Enchantment load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Enchantment fallback) {
                        final String key = configuration.getString(path);
                        if (key == null || key.isBlank()) {
                            return fallback;
                        }
                        final Enchantment enchantment =
                                Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
                        return enchantment == null ? fallback : enchantment;
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(
                                path,
                                value instanceof Enchantment enchantment ? enchantment.getKey().getKey() : null);
                    }
                },
                "Minecraft enchantment",
                null);
    }

    public static FieldType<Boolean> presenceFlag() {
        return new FieldType<Boolean>(
                        null,
                        (main, configuration, path, fallback) -> configuration.getBoolean(path, fallback),
                        "boolean",
                        false)
                .presenceFlag();
    }

    public static FieldType<ItemStackSelection> itemSelection() {
        return new FieldType<>(
                (main, name) -> itemStackSelectionArgument(main),
                new FieldType.ConfigCodec<>() {
                    @Override
                    public ItemStackSelection load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final ItemStackSelection fallback) {
                        final ItemStackSelection selection = new ItemStackSelection(main);
                        selection.loadFromFileConfiguration(configuration, path);
                        return selection;
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(path, null);
                        if (value instanceof ItemStackSelection selection) {
                            selection.saveToFileConfiguration(configuration, path);
                        }
                    }
                },
                "item selection",
                null);
    }

    public static FieldType<ItemStack> storedItemStack() {
        return new FieldType<>(
                null,
                new FieldType.ConfigCodec<>() {
                    @Override
                    public ItemStack load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final ItemStack fallback) {
                        try {
                            final ItemStack stored = configuration.getItemStack(path, null);
                            if (stored != null) {
                                return stored;
                            }
                        } catch (final RuntimeException ignored) {
                            // fall through to clean warning and version-stable fallback
                        }
                        main.getLogManager().warn(
                                "Invalid item stack at '" + path
                                        + "'. Falling back to STONE for this server version.");
                        return new ItemStack(Material.STONE);
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(path, value instanceof ItemStack itemStack ? itemStack : null);
                    }
                },
                "item stack",
                null);
    }

    public static FieldType<NQNPC> npc() {
        return new FieldType<>(
                null,
                new FieldType.ConfigCodec<>() {
                    @Override
                    public NQNPC load(
                            final com.notquests.paper.NotQuests main,
                        final org.bukkit.configuration.file.FileConfiguration configuration,
                        final String path,
                        final NQNPC fallback) {
                        final NQNPC npc = NQNPC.fromConfig(main, configuration, path);
                        return npc == null ? fallback : npc;
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        if (value instanceof NQNPC npc) {
                            npc.saveToConfig(configuration, path);
                        } else {
                            configuration.set(path, null);
                        }
                    }
                },
                "NPC selector",
                null);
    }

    public static FieldType<HashMap<String, String>> stringMap() {
        return new FieldType<>(
                null,
                new FieldType.ConfigCodec<>() {
                    @Override
                    public HashMap<String, String> load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final HashMap<String, String> fallback) {
                        final HashMap<String, String> values = new HashMap<>();
                        final ConfigurationSection section = configuration.getConfigurationSection(path);
                        if (section == null) {
                            return values;
                        }
                        for (final String key : section.getKeys(false)) {
                            values.put(key, configuration.getString(path + "." + key, ""));
                        }
                        return values;
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(path, null);
                        if (!(value instanceof HashMap<?, ?> map)) {
                            return;
                        }
                        for (final java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null) {
                                configuration.set(path + "." + entry.getKey(), entry.getValue().toString());
                            }
                        }
                    }
                },
                "text map",
                new HashMap<>());
    }

    public static FieldType<HashMap<String, NumberExpression>> numberExpressionMap() {
        return new FieldType<>(
                null,
                new FieldType.ConfigCodec<>() {
                    @Override
                    public HashMap<String, NumberExpression> load(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final HashMap<String, NumberExpression> fallback) {
                        final HashMap<String, NumberExpression> values = new HashMap<>();
                        final ConfigurationSection section = configuration.getConfigurationSection(path);
                        if (section == null) {
                            return values;
                        }
                        for (final String key : section.getKeys(false)) {
                            values.put(key, new NumberExpression(main, configuration.getString(path + "." + key, "0")));
                        }
                        return values;
                    }

                    @Override
                    public void save(
                            final com.notquests.paper.NotQuests main,
                            final org.bukkit.configuration.file.FileConfiguration configuration,
                            final String path,
                            final Object value) {
                        configuration.set(path, null);
                        if (!(value instanceof HashMap<?, ?> map)) {
                            return;
                        }
                        for (final java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() instanceof NumberExpression expression) {
                                configuration.set(path + "." + entry.getKey(), expression.getRawExpression());
                            }
                        }
                    }
                },
                "number expression map",
                new HashMap<>());
    }
}
