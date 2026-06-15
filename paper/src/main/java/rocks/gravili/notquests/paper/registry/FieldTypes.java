package rocks.gravili.notquests.paper.registry;

import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;

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

    public static FieldType<String> greedyText() {
        return new FieldType<>(
                (main, name) -> NQArguments.greedyStringArgument(),
                (main, configuration, path, fallback) -> configuration.getString(path, fallback),
                "text",
                "");
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
                (main, configuration, path, fallback) -> {
                    final ItemStackSelection selection = new ItemStackSelection(main);
                    selection.loadFromFileConfiguration(configuration, path);
                    return selection;
                },
                "item selection",
                null);
    }
}
