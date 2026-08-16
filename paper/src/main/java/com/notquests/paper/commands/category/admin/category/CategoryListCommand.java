package com.notquests.paper.commands.category.admin.category;

import net.kyori.adventure.text.Component;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.BaseCommand;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandManager;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.data.Category;

public class CategoryListCommand extends BaseCommand {
    public CategoryListCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("Lists all categories."))
                .literal("categories", NQDescription.of("Manages quest categories."))
                .literal("list", NQDescription.of("Lists every quest category."))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<highlight>All categories:"));
                    int counter = 1;
                    for (final Category category : notQuests.getDataManager().getCategories()) {
                        context.sender().sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> <main>" + category.getCategoryFullName()));
                        counter++;
                    }
                }));
    }
}
