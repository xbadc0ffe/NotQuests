package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveEventContext;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.QuestPlayer;

public final class Enchant {
    private Enchant() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("Enchant")
                .displayName("Enchant")
                .description("Counts matching items enchanted with a specific enchantment.")
                .field(
                        "enchantment",
                        FieldTypes.enchantment().config("specifics.enchantment"),
                        "Enchantment that must be applied to the item.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Items or NotQuests custom items that count when enchanted.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching enchantments required.")
                .flag(
                        "min",
                        FieldTypes.optionalNumberExpression().config("specifics.minLevelExpression"),
                        "Minimum enchantment level that counts.")
                .flag(
                        "max",
                        FieldTypes.optionalNumberExpression().config("specifics.maxLevelExpression"),
                        "Maximum enchantment level that counts.")
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .on(EnchantItemEvent.class, (event, objective) -> {
                    final ItemStack item = event.getItem();
                    final ItemStackSelection selection = objective.itemSelection("materials");
                    if (selection == null || !selection.checkIfIsIncluded(item)) {
                        return;
                    }

                    final Enchantment target = objective.value("enchantment", Enchantment.class);
                    if (target == null) {
                        return;
                    }

                    final int minLevel = level(main, objective.text("min"), objective, 0);
                    final int maxLevel = level(main, objective.text("max"), objective, 100);
                    for (final Map.Entry<Enchantment, Integer> enchantment : event.getEnchantsToAdd().entrySet()) {
                        final int level = enchantment.getValue();
                        if (enchantment.getKey().equals(target) && minLevel <= level && maxLevel >= level) {
                            objective.addProgress(1);
                            return;
                        }
                    }
                })
                .register();
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final QuestPlayer questPlayer,
            final ActiveObjective activeObjective) {
        final Enchantment enchantment = objective.value("enchantment", Enchantment.class);
        String enchantmentString = enchantment != null ? ("<lang:" + enchantment.getKey().getKey() + ">") : "";

        final int minLevel = level(main, objective.text("min"), questPlayer, 0);
        final int maxLevel = level(main, objective.text("max"), questPlayer, 100);
        if (minLevel != 0 && maxLevel != 100) {
            enchantmentString += " (" + minLevel + "-" + maxLevel + ")";
        } else if (minLevel != 0) {
            enchantmentString += " (> " + (minLevel - 1) + ")";
        } else if (maxLevel != 100) {
            enchantmentString += " (< " + (maxLevel + 1) + ")";
        }

        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.enchant.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%ITEMTOENCHANTTYPE%",
                                objective.itemSelection("materials").getAllMaterialsListedTranslated("main"),
                                "%ITEMTOENCHANTNAME%",
                                "",
                                "%(%",
                                "",
                                "%)%",
                                "",
                                "%ENCHANTMENT%",
                                enchantmentString));
    }

    private static int level(
            final NotQuests main,
            final String expression,
            final ObjectiveEventContext objective,
            final int fallback) {
        return level(main, expression, objective.questPlayer(), fallback);
    }

    private static int level(
            final NotQuests main,
            final String expression,
            final QuestPlayer questPlayer,
            final int fallback) {
        if (expression == null || expression.isBlank()) {
            return fallback;
        }
        return (int) new NumberExpression(main, expression).calculateValue(questPlayer);
    }
}
