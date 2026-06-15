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

public class WorldLeaveTrigger extends Trigger {

    private String worldToLeaveName;

    public WorldLeaveTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addTriggerBuilder) {
        manager.command(addTriggerBuilder
                .required("world to leave", NQArguments.stringArgument(), NQDescription.of("Name of the world which needs to be left"), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("ALL");
                    for (final World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                    return completions;
                })

                .required("amount", NQArguments.integerArgument(), NQDescription.of("Amount of times the world needs to be left."))
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .commandDescription(NQDescription.of("Triggers when the player leaves a specific world."))
                .handler(
                        (context) -> {
                            final String worldToLeaveName = context.get("world to leave");

                            WorldLeaveTrigger worldLeaveTrigger = new WorldLeaveTrigger(main);
                            worldLeaveTrigger.setWorldToLeaveName(worldToLeaveName);

                            main.getTriggerManager().addTrigger(worldLeaveTrigger, context);
                        }));
    }

    public final String getWorldToLeaveName() {
        return worldToLeaveName;
    }

    public void setWorldToLeaveName(final String worldToLeaveName) {
        this.worldToLeaveName = worldToLeaveName;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.worldToLeave", getWorldToLeaveName());
    }

    @Override
    public String getTriggerDescription() {
        return "World to leave: <WHITE>" + getWorldToLeaveName();
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.worldToLeaveName = configuration.getString(initialPath + ".specifics.worldToLeave", "ALL");
    }
}
