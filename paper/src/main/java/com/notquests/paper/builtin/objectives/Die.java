package com.notquests.paper.builtin.objectives;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class Die {
    private Die() {}

    public static boolean countsDamageType(final String configuredDamageType, final String actualDamageType) {
        return configuredDamageType == null
                || configuredDamageType.isBlank()
                || (actualDamageType != null && configuredDamageType.equalsIgnoreCase(actualDamageType));
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("Die")
                .displayName("Die")
                .description("Counts times the player dies, optionally requiring a specific death cause.")
                .field(
                        "amount",
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Number of times the player must die.")
                .flag(
                        "cause",
                        FieldTypes.text((context, input) -> damageTypeSuggestions()).config("specifics.damageType"),
                        "Only count deaths caused by this damage type, such as fall, lava, drown, or player_attack.")
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString("chat.objectives.taskDescription.die.base", questPlayer, activeObjective)
                        .replace("%DAMAGETYPE%", objective.text("cause").isBlank() ? "any" : objective.text("cause")))
                .on(
                        PlayerDeathEvent.class,
                        PlayerDeathEvent::getPlayer,
                        (event, objective) -> {
                            if (countsDamageType(objective.text("cause"), deathDamageType(event.getPlayer()))) {
                                objective.addProgress(1);
                            }
                        })
                .register();
    }

    private static String deathDamageType(final Player player) {
        final EntityDamageEvent lastDamageCause = player.getLastDamageCause();
        return lastDamageCause == null ? "unknown" : lastDamageCause.getDamageSource().getDamageType().key().value();
    }

    private static List<String> damageTypeSuggestions() {
        final List<String> completions = new ArrayList<>();
        final var damageTypeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE);
        for (final DamageType type : damageTypeRegistry) {
            completions.add(damageTypeRegistry.getKeyOrThrow(type).getKey());
        }
        return completions;
    }
}
