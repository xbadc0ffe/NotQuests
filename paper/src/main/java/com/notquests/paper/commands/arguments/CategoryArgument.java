package com.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArgumentType;
import com.notquests.paper.managers.data.Category;

import java.util.ArrayList;
import java.util.List;

/** Native-framework port of {@code CategoryParser}: resolves a {@link Category} by name. */
public final class CategoryArgument extends NQArgumentType<Category> {
    private final NotQuests main;

    public CategoryArgument(final NotQuests main) {
        this.main = main;
    }

    public static CategoryArgument categoryArgument(final NotQuests main) {
        return new CategoryArgument(main);
    }

    @Override
    public String valueTypeName() {
        return "category name";
    }

    @Override
    public Category convert(final String input) throws CommandSyntaxException {
        for (final Category category : main.getDataManager().getCategories()) {
            if (category.getCategoryName().equalsIgnoreCase(input)) {
                return category;
            }
        }
        throw fail("No Category found: " + input);
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        for (final Category category : main.getDataManager().getCategories()) {
            entries.add(category.getCategoryName());
        }
        return entries;
    }
}
