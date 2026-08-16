package com.notquests.paper.builtin.triggers;

import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.TriggerCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class NPCDeath {
    public static final String NPC = "NPC";

    private NPCDeath() {}

    public static void register(final NotQuests main, final TriggerCatalog triggers) {
        triggers.trigger("NPCDEATH")
                .displayName("NPC Death")
                .description("Runs the selected action after a Citizens NPC dies the configured number of times.")
                .field(
                        NPC,
                        FieldTypes.integer(-1).config("specifics.npcToDie"),
                        "Citizens NPC id that must die before this trigger gains progress.")
                .field(
                        "amount",
                        FieldTypes.integer(1).config("amountNeeded"),
                        "Number of NPC deaths required before this trigger runs.")
                .triggerDescription(trigger -> "NPC to die ID: <WHITE>" + trigger.integer(NPC, -1))
                .register();
    }
}
