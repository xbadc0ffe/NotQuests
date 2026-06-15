package rocks.gravili.notquests.paper.structs.triggers.types;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.List;

public class WorldEnterTrigger extends Trigger {

    private String worldToEnterName;

    public WorldEnterTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addTriggerBuilder) {
        manager.command(addTriggerBuilder
                .required("world to enter", NQArguments.stringArgument(), NQDescription.of("Name of the world which needs to be entered"), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("ALL");

                    for (final World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                    return completions;
                })
                .required("amount", NQArguments.integerArgument(), NQDescription.of("Amount of times the world needs to be entered."))
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .commandDescription(NQDescription.of("Triggers when the player enters a specific world."))
                .handler(
                        (context) -> {
                            final String worldToEnterName = context.get("world to enter");

                            WorldEnterTrigger worldEnterTrigger = new WorldEnterTrigger(main);
                            worldEnterTrigger.setWorldToEnterName(worldToEnterName);

                            main.getTriggerManager().addTrigger(worldEnterTrigger, context);
                        }));
    }

    public final String getWorldToEnterName() {
        return worldToEnterName;
    }

    public void setWorldToEnterName(final String worldToEnterName) {
        this.worldToEnterName = worldToEnterName;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.worldToEnter", getWorldToEnterName());
    }

    @Override
    public String getTriggerDescription() {
        return "World to enter: <WHITE>" + getWorldToEnterName();
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.worldToEnterName = configuration.getString(initialPath + ".specifics.worldToEnter", "ALL");
    }
}
