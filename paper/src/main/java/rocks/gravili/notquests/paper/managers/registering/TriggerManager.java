package rocks.gravili.notquests.paper.managers.registering;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;

import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;
import rocks.gravili.notquests.paper.structs.triggers.types.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

public class TriggerManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Trigger>> triggers;

    public TriggerManager(final NotQuests main) {
        this.main = main;
        triggers = new HashMap<>();

        registerDefaultTriggers();
    }

    public void registerDefaultTriggers() {
        main.getLogManager().info("Registering triggers...");

        triggers.clear();
        registerTrigger("BEGIN", BeginTrigger.class);
        registerTrigger("COMPLETE", CompleteTrigger.class);
        registerTrigger("DEATH", DeathTrigger.class);
        registerTrigger("DISCONNECT", DisconnectTrigger.class);
        registerTrigger("FAIL", FailTrigger.class);
        registerTrigger("NPCDEATH", NPCDeathTrigger.class);
        registerTrigger("WORLDENTER", WorldEnterTrigger.class);
        registerTrigger("WORLDLEAVE", WorldLeaveTrigger.class);
    }

    public void registerTrigger(final String identifier, final Class<? extends Trigger> trigger) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering trigger <highlight>" + identifier);
        }
        triggers.put(identifier, trigger);

        try {
            Method commandHandler =
                    trigger.getMethod(
                            "handleCommands", main.getClass(), NQCommandManager.class, NQCommandBuilder.class);
            commandHandler.invoke(
                    trigger,
                    main,
                    main.getCommandManager().getNQCommandManager(),
                    main.getCommandManager()
                            .getAdminEditAddTriggerCommandBuilder()
                            .literal(identifier, NQDescription.of(triggerLiteralDescription(identifier)))
                            .commandDescription(NQDescription.of("Creates a new " + identifier + " trigger")));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static String triggerLiteralDescription(final String identifier) {
        return switch (identifier) {
            case "BEGIN" -> "Runs the selected action when the quest starts.";
            case "COMPLETE" -> "Runs the selected action when the quest is completed.";
            case "DEATH" -> "Runs the selected action when the quest player dies.";
            case "DISCONNECT" -> "Runs the selected action when the quest player disconnects.";
            case "FAIL" -> "Runs the selected action when the quest fails.";
            case "NPCDEATH" -> "Runs the selected action when a tracked NPC dies.";
            case "WORLDENTER" -> "Runs the selected action when the quest player enters the configured world.";
            case "WORLDLEAVE" -> "Runs the selected action when the quest player leaves the configured world.";
            default -> "Selects the " + identifier + " trigger type.";
        };
    }

    public final Class<? extends Trigger> getTriggerClass(@NotNull final String type) {
        return triggers.get(type);
    }

    public final String getTriggerType(final Class<? extends Trigger> trigger) {
        for (final String triggerType : triggers.keySet()) {
            if (triggers.get(triggerType).equals(trigger)) {
                return triggerType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Trigger>> getTriggersAndIdentifiers() {
        return triggers;
    }

    public final Collection<Class<? extends Trigger>> getTriggers() {
        return triggers.values();
    }

    public final Collection<String> getTriggerIdentifiers() {
        return triggers.keySet();
    }

    public void addTrigger(Trigger trigger, NQCommandContext context) {
        Quest quest = context.getOrDefault("quest", null);

        final Action action = context.get("action");

        int applyOn = 0;
        if (context.flags().contains(main.getCommandManager().applyOn)) {
            applyOn = context.flags().getValue(main.getCommandManager().applyOn, 0);
        }
        final String worldString =
                context.flags().getValue(main.getCommandManager().triggerWorldString, "ALL");

        int amount = 1;
        final Integer amountArgument = context.get("amount");
        if (amountArgument != null) {
            amount = amountArgument;
        }

        if (quest != null) {
            trigger.setQuest(quest);
            trigger.setAction(action);
            trigger.setApplyOn(applyOn);
            trigger.setWorldName(worldString);
            trigger.setTriggerID(quest.getFreeTriggerID());
            trigger.setAmountNeeded(amount);

            quest.addTrigger(trigger, true);

            context.sender().sendMessage(main.parse("<success>"
                    + getTriggerType(trigger.getClass())
                    + " Trigger successfully added to Quest <highlight>"
                    + quest.getIdentifier()
                    + "</highlight>!"));
        }
    }
}
