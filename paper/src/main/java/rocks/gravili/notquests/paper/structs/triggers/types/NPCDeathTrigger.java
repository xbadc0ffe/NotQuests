package rocks.gravili.notquests.paper.structs.triggers.types;

import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.List;

public class NPCDeathTrigger extends Trigger { //TODO: Add support for other NPC systems

    private int npcToDieID = -1;

    public NPCDeathTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addTriggerBuilder) {
        manager.command(
                addTriggerBuilder
                        .required("NPC", NQArguments.integerArgument(), NQDescription.of("ID of the Citizens NPC the player has to escort."), (context, input) -> {
                            final List<String> completions = new ArrayList<>();
                            for (final int npcID : main.getIntegrationsManager().getCitizensManager().getAllNPCIDs()) {
                                completions.add(String.valueOf(npcID));
                            }
                            return completions;
                        })
                        .required("amount", NQArguments.integerArgument(), NQDescription.of("Amount of times the NPC needs to die."))
                        .flag(main.getCommandManager().applyOn)
                        .flag(main.getCommandManager().triggerWorldString)
                        .commandDescription(NQDescription.of("Triggers when specified Citizens NPC dies."))
                        .handler(
                                (context) -> {
                                    final int npcToDieID = context.get("NPC");

                                    NPCDeathTrigger npcDeathTrigger = new NPCDeathTrigger(main);
                                    npcDeathTrigger.setNpcToDieID(npcToDieID);

                                    main.getTriggerManager().addTrigger(npcDeathTrigger, context);
                                }));
    }

    public final int getNpcToDieID() {
        return npcToDieID;
    }

    public void setNpcToDieID(final int npcToDieID) {
        this.npcToDieID = npcToDieID;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.npcToDie", getNpcToDieID());
    }

    @Override
    public String getTriggerDescription() {
        return "NPC to die ID: <WHITE>" + getNpcToDieID();
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.npcToDieID = configuration.getInt(initialPath + ".specifics.npcToDie");
    }
}
