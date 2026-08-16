package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.actions.support.VariableActionSupport;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_BOOLEANS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_NUMBERS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_STRINGS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.OPERATOR;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.VARIABLE_NAME;
import static com.notquests.paper.commands.arguments.variables.ItemStackListVariableArgument.itemStackListVariableArgument;

public final class ItemStackListVariableAction {
    private static final String ITEM_STACK = "itemStack";
    private static final String AMOUNT = "amount";

    private ItemStackListVariableAction() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("ItemStackList")
                .displayName("Item Stack List Variable Action")
                .description("Changes a settable item-stack-list variable.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "Item-stack-list variable to change.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "How to change the variable value.")
                .field(ITEM_STACK, FieldTypes.storedItemStack().config("specifics.itemStack"), "Item stack used by this variable action.")
                .field(ADDITIONAL_STRINGS, FieldTypes.stringMap().config("specifics.additionalStrings"), "Extra text arguments passed to the variable.")
                .field(ADDITIONAL_NUMBERS, FieldTypes.numberExpressionMap().config("specifics.additionalNumbers"), "Extra number-expression arguments passed to the variable.")
                .field(ADDITIONAL_BOOLEANS, FieldTypes.numberExpressionMap().config("specifics.additionalBooleans"), "Extra boolean-expression arguments passed to the variable.")
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine((action, arguments) -> deserialize(main, action, arguments))
                .execute((action, questPlayer, objects) -> {
                    final Variable<?> variable = VariableActionSupport.variable(main, action.action());
                    if (variable == null) {
                        main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable <highlight>" + action.text(VARIABLE_NAME) + "</highlight> not found. Report this to the Server owner.");
                        return;
                    }
                    VariableActionSupport.applyAdditionalArguments(variable, action.action());
                    final Object currentValueObject = variable.getValue(questPlayer, objects);
                    if (currentValueObject == null) {
                        main.getLogManager().warn("Cannot execute ItemStackList action because current value is null.");
                        return;
                    }
                    final ItemStack itemStack = action.action().value(ITEM_STACK, ItemStack.class);
                    if (itemStack == null) {
                        main.getLogManager().warn("Cannot execute ItemStackList action because item stack is null.");
                        return;
                    }
                    final ItemStack[] nextValue = nextValue(main, variable, action.text(OPERATOR), itemStack);
                    setItemStackListValue(main, variable, currentValueObject, nextValue, questPlayer, objects);
                })
                .actionDescription((action, questPlayer, objects) -> {
                    final ItemStack itemStack = action.action().value(ITEM_STACK, ItemStack.class);
                    return action.text(VARIABLE_NAME)
                            + ": "
                            + (itemStack == null ? "unknown item" : main.getMiniMessage().serialize(itemStack.displayName()));
                })
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        for (final String variableString : main.getVariableCatalog().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableString);
            if (!VariableActionSupport.shouldRegister(main, variableString, VariableDataType.ITEMSTACKLIST)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to change the " + variableString + " item-list variable: set, add, remove, or clear."),
                            (context, input) -> List.of("set", "add", "remove", "clear"))
                    .required(
                            "expression",
                            itemStackListVariableArgument("expression", variable),
                            NQDescription.of("Item or item-list expression used by this " + variableString + " action."))
                    .required(
                            AMOUNT,
                            NQArguments.integerArgument(),
                            NQDescription.of("Stack amount to apply for each item in this item-list action."))
                    .handler(context -> {
                        final String expression = context.get("expression");
                        final int amount = context.get(AMOUNT);
                        final ItemStack itemStack = itemStack(main, context.sender(), expression, amount);
                        if (itemStack == null) {
                            return;
                        }
                        final DefinedAction action = type.createAction();
                        VariableActionSupport.setCommandValues(main, action, variable, context, context.get(OPERATOR), expression);
                        action.setValue(ITEM_STACK, itemStack);
                        main.getActionCatalog().addAction(action, context, actionFor);
                    }));
        }
    }

    private static void deserialize(
            final NotQuests main, final DefinedAction action, final ArrayList<String> arguments) {
        action.setValue(VARIABLE_NAME, arguments.get(0));
        action.setValue(OPERATOR, arguments.get(1));
        action.setValue(ITEM_STACK, new ItemStack(parseStoredMaterial(main, arguments.get(2)), Integer.parseInt(arguments.get(3))));
        final Variable<?> variable = main.getVariableCatalog().getVariableFromString(arguments.get(0));
        if (variable != null && variable.isCanSetValue() && variable.getVariableDataType() == VariableDataType.ITEMSTACKLIST) {
            VariableActionSupport.setAdditionalArgumentsFromSingleLine(main, action, arguments, variable, 4);
        }
    }

    private static ItemStack itemStack(
            final NotQuests main,
            final org.bukkit.command.CommandSender sender,
            final String expression,
            final int amount) {
        if (expression.equalsIgnoreCase("hand")) {
            if (sender instanceof final Player player) {
                final ItemStack itemStack = player.getInventory().getItemInMainHand().clone();
                itemStack.setAmount(amount);
                return itemStack;
            }
            sender.sendMessage(main.parse("<error>This must be run by a player."));
            return null;
        }
        if (expression.equalsIgnoreCase("any")) {
            sender.sendMessage(main.parse("<error>You cannot use <highlight>'any'</highlight> here!"));
            return null;
        }
        return new ItemStack(parseStoredMaterial(main, expression), amount);
    }

    private static ItemStack[] nextValue(
            final NotQuests main, final Variable<?> variable, final String operator, final ItemStack itemStack) {
        if (operator.equalsIgnoreCase("clear")) {
            variable.addAdditionalBooleanArgument("clear", NumberExpression.ofStatic(main, 1));
            return new ItemStack[0];
        }
        if (operator.equalsIgnoreCase("set")) {
            variable.addAdditionalBooleanArgument("set", NumberExpression.ofStatic(main, 1));
            return splitStacks(itemStack);
        }
        if (operator.equalsIgnoreCase("add")) {
            variable.addAdditionalBooleanArgument("add", NumberExpression.ofStatic(main, 1));
            return splitStacks(itemStack);
        }
        if (operator.equalsIgnoreCase("remove")) {
            variable.addAdditionalBooleanArgument("remove", NumberExpression.ofStatic(main, 1));
            return splitStacks(itemStack);
        }
        return new ItemStack[0];
    }

    private static ItemStack[] splitStacks(final ItemStack itemStack) {
        final ArrayList<ItemStack> values = new ArrayList<>();
        int amountLeft = itemStack.getAmount();
        while (amountLeft > itemStack.getMaxStackSize()) {
            final ItemStack clone = itemStack.clone();
            clone.setAmount(clone.getMaxStackSize());
            values.add(clone);
            amountLeft -= clone.getMaxStackSize();
        }
        final ItemStack clone = itemStack.clone();
        clone.setAmount(amountLeft);
        values.add(clone);
        return values.toArray(new ItemStack[0]);
    }

    @SuppressWarnings("unchecked")
    private static void setItemStackListValue(
            final NotQuests main,
            final Variable<?> variable,
            final Object currentValueObject,
            final ItemStack[] nextValue,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final Object... objects) {
        if (currentValueObject instanceof ItemStack[]) {
            ((Variable<ItemStack[]>) variable).setValue(nextValue, questPlayer, objects);
        } else if (currentValueObject instanceof ArrayList<?>) {
            ((Variable<ArrayList<ItemStack>>) variable).setValue(new ArrayList<>(Arrays.asList(nextValue)), questPlayer, objects);
        } else {
            main.getLogManager().warn("Cannot execute ItemStackList action because value type "
                    + currentValueObject.getClass().getName()
                    + " is invalid.");
        }
    }

    private static Material parseStoredMaterial(final NotQuests main, final String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (final RuntimeException exception) {
            main.getLogManager().warn(
                    "Invalid ItemStackList action material '" + materialName
                            + "'. Falling back to STONE for this server version.");
            return Material.STONE;
        }
    }
}
