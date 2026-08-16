package com.notquests.paper.builtin.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.conditions.support.VariableConditionSupport;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.conditions.ConditionCatalog;
import com.notquests.paper.registry.ConditionType;
import com.notquests.paper.registry.DefinedCondition;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.conditions.ConditionFor;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import static com.notquests.paper.commands.arguments.variables.ItemStackListVariableArgument.itemStackListVariableArgument;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_BOOLEANS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_NUMBERS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_STRINGS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.OPERATOR;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.VARIABLE_NAME;

public final class ItemStackListVariableCondition {
    private static final String ITEM_STACK = "itemStack";

    private ItemStackListVariableCondition() {}

    public static void register(final NotQuests main, final ConditionCatalog conditions) {
        conditions.condition("ItemStackList")
                .displayName("Item Stack List Variable")
                .description("Compares an item-list variable with a required item stack.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "Item-list variable to check.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "Item-list comparison operator to use.")
                .field(ITEM_STACK, FieldTypes.storedItemStack().config("specifics.itemStack"), "Required item stack.")
                .field(ADDITIONAL_STRINGS, FieldTypes.stringMap().config("specifics.additionalStrings"), "Extra text arguments passed to the variable.")
                .field(ADDITIONAL_NUMBERS, FieldTypes.numberExpressionMap().config("specifics.additionalNumbers"), "Extra number-expression arguments passed to the variable.")
                .field(ADDITIONAL_BOOLEANS, FieldTypes.numberExpressionMap().config("specifics.additionalBooleans"), "Extra boolean-expression arguments passed to the variable.")
                .commands((type, builder, conditionFor) -> registerCommands(main, type, builder, conditionFor))
                .singleLine((condition, arguments) -> {
                    condition.setValue(VARIABLE_NAME, arguments.get(0));
                    condition.setValue(OPERATOR, arguments.get(1));
                    condition.setValue(ITEM_STACK, new ItemStack(parseStoredMaterial(main, arguments.get(2)), Integer.parseInt(arguments.get(3))));
                    final Variable<?> variable = main.getVariableCatalog().getVariableFromString(arguments.get(0));
                    if (variable != null && variable.getVariableDataType() == VariableDataType.ITEMSTACKLIST) {
                        VariableConditionSupport.setAdditionalArgumentsFromSingleLine(main, condition, arguments, variable, 4);
                    }
                })
                .check((condition, questPlayer) -> check(main, condition.condition(), questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(main, condition.condition()))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ConditionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final ConditionFor conditionFor) {
        for (final String variableString : main.getVariableCatalog().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableString);
            if (!VariableConditionSupport.shouldRegister(main, variableString, VariableDataType.ITEMSTACKLIST)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to compare the " + variableString + " item-list variable with the supplied item expression."),
                            (context, input) -> List.of("equals", "contains"))
                    .required(
                            "expression",
                            itemStackListVariableArgument("expression", variable),
                            NQDescription.of("Item or item-list expression to compare with the current " + variableString + " value."))
                    .required(
                            "amount",
                            NQArguments.integerArgument(),
                            NQDescription.of("Required stack amount for each item in this item-list condition."))
                    .handler(context -> {
                        final String expression = context.get("expression");
                        final int amount = context.get("amount");
                        final ItemStack itemStack;
                        if (expression.equalsIgnoreCase("hand")) {
                            if (!(context.sender() instanceof final Player player)) {
                                context.sender().sendMessage(main.parse("<error>This must be run by a player."));
                                return;
                            }
                            itemStack = player.getInventory().getItemInMainHand().clone();
                            itemStack.setAmount(amount);
                        } else {
                            if (expression.equalsIgnoreCase("any")) {
                                context.sender().sendMessage(main.parse("<error>You cannot use <highlight>'any'</highlight> here!"));
                                return;
                            }
                            itemStack = new ItemStack(parseStoredMaterial(main, expression), amount);
                        }
                        final DefinedCondition condition = type.createCondition();
                        condition.setValue(VARIABLE_NAME, variable.getVariableType());
                        condition.setValue(OPERATOR, context.get(OPERATOR));
                        condition.setValue(ITEM_STACK, itemStack);
                        VariableConditionSupport.setCommandValues(
                                main, condition, variable, context, context.get(OPERATOR), itemStack);
                        condition.setValue(ITEM_STACK, itemStack);
                        main.getConditionCatalog().addCondition(condition, context, conditionFor);
                    }));
        }
    }

    private static String check(
            final NotQuests main, final DefinedCondition condition, final QuestPlayer questPlayer) {
        final Variable<?> variable = VariableConditionSupport.variable(main, condition);
        if (variable == null) {
            return "<ERROR>Error: variable <highlight>" + condition.text(VARIABLE_NAME) + "</highlight> not found. Report this to the Server owner.";
        }
        final ItemStack required = condition.value(ITEM_STACK, ItemStack.class);
        if (required == null) {
            return "<ERROR>Error: item-list condition has no required item stack.";
        }
        VariableConditionSupport.applyAdditionalArguments(variable, condition);
        final Object value = variable.getValue(questPlayer);
        if (value == null) {
            return "<YELLOW>You don't have any " + variable.getPlural() + "!";
        }
        final ItemStack[] current = asItemArray(value);
        final ArrayList<ItemStack> present = new ArrayList<>();
        for (final ItemStack item : current) {
            if (item != null) {
                present.add(item);
            }
        }
        return switch (condition.text(OPERATOR)) {
            case "equals" -> equalsRequired(present, required)
                    ? ""
                    : "<YELLOW>The " + variable.getPlural() + " need to contain ONLY: <highlight>" + main.getMiniMessage().serialize(required.displayName()) + " x " + required.getAmount() + "</highlight>.";
            case "contains" -> containsRequired(present, required)
                    ? ""
                    : "<YELLOW>The " + variable.getPlural() + " need to contain: <highlight>" + main.getMiniMessage().serialize(required.displayName()) + " x " + required.getAmount() + "</highlight>.";
            default -> "<ERROR>Error: variable operator <highlight>" + condition.text(OPERATOR) + "</highlight> is invalid. Report this to the Server owner.";
        };
    }

    private static String description(final NotQuests main, final DefinedCondition condition) {
        final ItemStack itemStack = condition.value(ITEM_STACK, ItemStack.class);
        final String itemName = itemStack == null ? "unknown item" : main.getMiniMessage().serialize(itemStack.displayName());
        return switch (condition.text(OPERATOR)) {
            case "equals" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needs to be equal " + itemName + "</GRAY>";
            case "contains" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needs to contain " + itemName + "</GRAY>";
            default -> "<GRAY>Error: invalid expression.</GRAY>";
        };
    }

    private static ItemStack[] asItemArray(final Object value) {
        if (value instanceof ItemStack[] array) {
            return array;
        }
        if (value instanceof ArrayList<?> list) {
            return list.toArray(new ItemStack[0]);
        }
        return (ItemStack[]) value;
    }

    private static boolean equalsRequired(final ArrayList<ItemStack> present, final ItemStack required) {
        int amountNeeded = required.getAmount();
        for (final ItemStack itemStack : present) {
            if (!itemStack.isSimilar(required)) {
                return false;
            }
            amountNeeded -= itemStack.getAmount();
        }
        return amountNeeded == 0;
    }

    private static boolean containsRequired(final ArrayList<ItemStack> present, final ItemStack required) {
        int amountNeeded = required.getAmount();
        for (final ItemStack itemStack : present) {
            if (itemStack.isSimilar(required)) {
                amountNeeded -= itemStack.getAmount();
            }
        }
        return amountNeeded <= 0;
    }

    private static Material parseStoredMaterial(final NotQuests main, final String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (final RuntimeException exception) {
            main.getLogManager().warn(
                    "Invalid ItemStackList condition material '" + materialName
                            + "'. Falling back to STONE for this server version.");
            return Material.STONE;
        }
    }
}
