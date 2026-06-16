package com.notquests.paper.builtin.objectives;

import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.objectives.Objective;

public final class KillMobs {
    private KillMobs() {}

    public static final String TYPE = "KillMobs";
    public static final String ENTITY_TYPE = "entityType";

    public static boolean isKillMobs(final Objective objective) {
        return objective instanceof DefinedObjective defined && defined.isType(TYPE);
    }

    public static String target(final Objective objective) {
        return objective instanceof DefinedObjective defined && defined.isType(TYPE)
                ? defined.text(ENTITY_TYPE)
                : "";
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective(TYPE)
                .displayName("Kill Mobs")
                .description("Counts matching mobs killed by the player.")
                .field(
                        "entityType",
                        FieldTypes.entityType().config("specifics.mobToKill"),
                        "Entity type the player must kill, or `any` to count every mob.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching mobs the player must kill.")
                .flag(
                        "nametag_equals",
                        FieldTypes.greedyText().config("extras.nameTagEquals"),
                        "Only count mobs whose custom name exactly matches this text.")
                .flag(
                        "nametag_containsany",
                        FieldTypes.greedyText().config("extras.nameTagContainsAny"),
                        "Only count mobs whose custom name contains every word in this text.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.killMobs.base",
                                questPlayer,
                                activeObjective,
                                Map.of("%MOBTOKILL%", objective.text("entityType"))))
                .on(EntityDeathEvent.class, event -> event.getEntity().getKiller(), (event, objective) -> {
                    final EntityType killedMob = event.getEntity().getType();
                    final String target = objective.text(ENTITY_TYPE);
                    if (!target.equalsIgnoreCase("any") && !target.equalsIgnoreCase(killedMob.toString())) {
                        return;
                    }
                    if (event.getEntity() == event.getEntity().getKiller()) {
                        return;
                    }
                    if (!matchesNameTag(event.getEntity().customName(), objective.text("nametag_equals"), objective.text("nametag_containsany"))) {
                        return;
                    }
                    objective.addProgress(1);
                })
                .register();
    }

    private static boolean matchesNameTag(
            final Component customName,
            final String equals,
            final String containsEveryWord) {
        if (equals.isBlank() && containsEveryWord.isBlank()) {
            return true;
        }
        if (customName == null) {
            return false;
        }
        final String plainName = PlainTextComponentSerializer.plainText().serialize(customName);
        if (plainName.isBlank()) {
            return false;
        }
        final String lowerName = plainName.toLowerCase(Locale.ROOT);
        if (!containsEveryWord.isBlank()) {
            for (final String namePart : containsEveryWord.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!lowerName.contains(namePart)) {
                    return false;
                }
            }
        }
        return equals.isBlank() || lowerName.equals(equals.toLowerCase(Locale.ROOT));
    }
}
