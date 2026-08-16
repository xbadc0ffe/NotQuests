package com.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandManager;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.BaseCommand;

public class SaveCommand extends BaseCommand {
    public SaveCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("Saves the NotQuests configuration file."))
                .literal("save", NQDescription.of("Saves NotQuests data to disk."))
                .handler((context) -> {
                    notQuests.getDataManager().saveData();
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<success>NotQuests configuration and player data has been saved"));
                }));
    }
}
