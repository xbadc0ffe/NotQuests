package rocks.gravili.notquests.paper.structs.triggers.types;

import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

public class DeathTrigger extends Trigger {

    public DeathTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addTriggerBuilder) {
        manager.command(addTriggerBuilder
                .required("amount", NQArguments.integerArgument(), NQDescription.of("Amount of deaths needed for the Trigger to trigger."))
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .commandDescription(NQDescription.of("Triggers when a the Player dies."))
                .handler(
                        (context) -> {
                            DeathTrigger deathTrigger = new DeathTrigger(main);

                            main.getTriggerManager().addTrigger(deathTrigger, context);
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
