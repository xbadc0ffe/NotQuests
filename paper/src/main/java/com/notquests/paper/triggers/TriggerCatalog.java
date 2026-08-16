package com.notquests.paper.triggers;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQCommandBuilder;

import com.notquests.paper.NotQuests;
import com.notquests.paper.registry.DefinedTrigger;
import com.notquests.paper.registry.TriggerType;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.actions.Action;
import com.notquests.paper.triggers.Trigger;
import com.notquests.paper.builtin.triggers.Begin;
import com.notquests.paper.builtin.triggers.Complete;
import com.notquests.paper.builtin.triggers.Death;
import com.notquests.paper.builtin.triggers.Disconnect;
import com.notquests.paper.builtin.triggers.Fail;
import com.notquests.paper.builtin.triggers.NPCDeath;
import com.notquests.paper.builtin.triggers.WorldEnter;
import com.notquests.paper.builtin.triggers.WorldLeave;

import java.util.Collection;
import java.util.HashMap;

public class TriggerCatalog {
    private final NotQuests main;

    private final HashMap<String, TriggerType> triggerTypes;

    public TriggerCatalog(final NotQuests main) {
        this.main = main;
        triggerTypes = new HashMap<>();

        registerDefaultTriggers();
    }

    public void registerDefaultTriggers() {
        main.getLogManager().info("Registering triggers...");

        triggerTypes.clear();
        Begin.register(main, this);
        Complete.register(main, this);
        Death.register(main, this);
        Disconnect.register(main, this);
        Fail.register(main, this);
        NPCDeath.register(main, this);
        WorldEnter.register(main, this);
        WorldLeave.register(main, this);
    }

    public TriggerType.Builder trigger(final String identifier) {
        return new TriggerType.Builder(main, this, identifier);
    }

    public void registerTrigger(final TriggerType trigger) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering trigger <highlight>" + trigger.id());
        }
        triggerTypes.put(trigger.id(), trigger);
        trigger.registerCommands(main.getCommandManager()
                .getAdminEditAddTriggerCommandBuilder()
                .literal(trigger.id(), NQDescription.of(trigger.description()))
                .commandDescription(NQDescription.of("Creates a new " + trigger.displayName() + " trigger")));
    }

    public final Trigger createTrigger(final String type) {
        final TriggerType triggerType = triggerTypes.get(type);
        return triggerType == null ? null : triggerType.createTrigger();
    }

    public final String getTriggerType(final Trigger trigger) {
        if (trigger instanceof final DefinedTrigger definedTrigger) {
            return definedTrigger.definition().id();
        }
        return null;
    }

    public final HashMap<String, TriggerType> getTriggerTypesAndIdentifiers() {
        return triggerTypes;
    }

    public final Collection<TriggerType> getTriggers() {
        return triggerTypes.values();
    }

    public final Collection<String> getTriggerIdentifiers() {
        return triggerTypes.keySet();
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
                    + getTriggerType(trigger)
                    + " Trigger successfully added to Quest <highlight>"
                    + quest.getIdentifier()
                    + "</highlight>!"));
        }
    }
}
