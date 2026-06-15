package rocks.gravili.notquests.paper.structs.triggers.types;

import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

public class CompleteTrigger extends Trigger {

    public CompleteTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addTriggerBuilder) {
        manager.command(addTriggerBuilder
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .commandDescription(NQDescription.of("Triggers when a Quest or an Objective is completed"))
                .handler(
                        (context) -> {
                            CompleteTrigger completeTrigger = new CompleteTrigger(main);

                            main.getTriggerManager().addTrigger(completeTrigger, context);
                        }));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
    }

    @Override
    public String getTriggerDescription() {
        return null;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
    }
}
