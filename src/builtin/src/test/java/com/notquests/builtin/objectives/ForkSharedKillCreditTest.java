package com.notquests.builtin.objectives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.BuiltInPack;
import com.notquests.builtin.TestPlatformPlayer;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.ActiveObjective;

import java.time.Duration;

/**
 * FORK DIVERGENCE: shared kill credit for KillMobs. With general.yml kill-credit.mode = shared,
 * every player who hurt a mob shortly before it died and is still near the death progresses
 * their kill objective, not only the player the server reports as the killer. Objectives can
 * pin their own creditMode; solo servers keep vanilla single-killer credit.
 */
class ForkSharedKillCreditTest {
    private static final NQLocation DEATH = NQLocation.at("overworld", 0, 64, 0);
    private static final EntityEvent ZOMBIE_DIED = new EntityEvent("zombie", "", false);

    @Test
    void sharedModeCreditsEveryNearbyContributorOnceAndSkipsFarOnes() {
        final Fixture fixture = Fixture.withMode("shared");
        final TestPlayer killer = new TestPlayer("killer", 0);
        final TestPlayer helper = new TestPlayer("helper", 10);
        final TestPlayer sniper = new TestPlayer("sniper", 100);
        final TestPlayer wolfOwner = new TestPlayer("wolf-owner", 20);
        final ActiveObjective killerHunt = fixture.give(killer, "Hunt", "zombie 5");
        final ActiveObjective helperHunt = fixture.give(helper, "Hunt", "zombie 5");
        final ActiveObjective sniperHunt = fixture.give(sniper, "Hunt", "zombie 5");
        final ActiveObjective wolfOwnerHunt = fixture.give(wolfOwner, "Hunt", "zombie 5");

        fixture.plugin.entityDamagedByPlayer("mob-1", killer);
        fixture.plugin.entityDamagedByPlayer("mob-1", helper);
        fixture.plugin.entityDamagedByPlayer("mob-1", sniper);
        fixture.plugin.entityDamagedByPlayer("mob-1", wolfOwner);
        fixture.plugin.entityDamagedByPlayer("mob-1", wolfOwner);
        fixture.plugin.entityDamagedByPlayer("mob-1", new TestPlayer("offline", 0));
        fixture.plugin.entityDied(null, null, killer, ZOMBIE_DIED);
        fixture.plugin.entityDiedSharedCredit("mob-1", killer, DEATH, ZOMBIE_DIED);

        assertEquals(1, killerHunt.getCurrentProgress(), "the killer is credited exactly once");
        assertEquals(1, helperHunt.getCurrentProgress(), "a nearby contributor shares the kill");
        assertEquals(0, sniperHunt.getCurrentProgress(), "out of range gets nothing");
        assertEquals(1, wolfOwnerHunt.getCurrentProgress(), "two hits still credit once");

        fixture.plugin.entityDiedSharedCredit("mob-1", killer, DEATH, ZOMBIE_DIED);
        assertEquals(1, helperHunt.getCurrentProgress(), "a mob pays out once");
    }

    @Test
    void soloModeOnlyCreditsTheKiller() {
        final Fixture fixture = Fixture.withMode("solo");
        final TestPlayer killer = new TestPlayer("killer", 0);
        final TestPlayer helper = new TestPlayer("helper", 10);
        final ActiveObjective killerHunt = fixture.give(killer, "Hunt", "zombie 5");
        final ActiveObjective helperHunt = fixture.give(helper, "Hunt", "zombie 5");

        fixture.plugin.entityDamagedByPlayer("mob-1", helper);
        fixture.plugin.entityDamagedByPlayer("mob-1", killer);
        fixture.plugin.entityDied(null, null, killer, ZOMBIE_DIED);
        fixture.plugin.entityDiedSharedCredit("mob-1", killer, DEATH, ZOMBIE_DIED);

        assertEquals(1, killerHunt.getCurrentProgress());
        assertEquals(0, helperHunt.getCurrentProgress(), "solo servers keep vanilla credit");
    }

    @Test
    void objectiveCreditModeOverridesTheServerDefault() {
        final Fixture soloServer = Fixture.withMode("solo");
        final TestPlayer partyHelper = new TestPlayer("party-helper", 10);
        final ActiveObjective party = soloServer.give(partyHelper, "Party", "zombie 5 --creditMode shared");
        soloServer.plugin.entityDamagedByPlayer("mob-1", partyHelper);
        soloServer.plugin.entityDiedSharedCredit("mob-1", new TestPlayer("killer", 0), DEATH, ZOMBIE_DIED);
        assertEquals(1, party.getCurrentProgress(), "an objective can opt into shared credit on a solo server");

        final Fixture sharedServer = Fixture.withMode("shared");
        final TestPlayer loner = new TestPlayer("loner", 10);
        final ActiveObjective alone = sharedServer.give(loner, "Alone", "zombie 5 --creditMode solo");
        sharedServer.plugin.entityDamagedByPlayer("mob-1", loner);
        sharedServer.plugin.entityDiedSharedCredit("mob-1", new TestPlayer("killer", 0), DEATH, ZOMBIE_DIED);
        assertEquals(0, alone.getCurrentProgress(), "an objective can insist on solo credit on a shared server");
    }

    @Test
    void contributorsAreCreditedWhenNobodyIsTheVanillaKiller() {
        final Fixture fixture = Fixture.withMode("shared");
        final TestPlayer helper = new TestPlayer("helper", 10);
        final ActiveObjective hunt = fixture.give(helper, "Hunt", "zombie 5");

        fixture.plugin.entityDamagedByPlayer("mob-1", helper);
        fixture.plugin.entityDied(null, null, null, null);
        fixture.plugin.entityDiedSharedCredit("mob-1", null, DEATH, ZOMBIE_DIED);

        assertEquals(1, hunt.getCurrentProgress(), "a mob that burned to death after the hit still counts");
    }

    @Test
    void sharedCreditStillAppliesTheObjectiveFilters() {
        final Fixture fixture = Fixture.withMode("shared");
        final TestPlayer helper = new TestPlayer("helper", 10);
        final ActiveObjective skeletons = fixture.give(helper, "Bones", "skeleton 5");

        fixture.plugin.entityDamagedByPlayer("mob-1", helper);
        fixture.plugin.entityDiedSharedCredit("mob-1", null, DEATH, ZOMBIE_DIED);

        assertEquals(0, skeletons.getCurrentProgress(), "the mob type filter is unchanged");
    }

    private static Objectives.Type type(final NotQuestsRegistry registry, final String id) {
        return registry.objectives().stream()
                .filter(objective -> objective.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private record Fixture(NotQuestsPlugin plugin, NotQuestsAdapter adapter) {
        static Fixture withMode(final String mode) {
            final NotQuestsPlugin plugin = NotQuestsPlugin.create();
            final NotQuestsAdapter adapter =
                    plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
            BuiltInPack.register(plugin, adapter);
            final YamlConfig general = YamlConfig.empty();
            ConfigurationManager.ensure(general);
            general.set("general.kill-credit.mode", mode);
            plugin.configuration().loadFrom(general);
            return new Fixture(plugin, adapter);
        }

        ActiveObjective give(final TestPlayer player, final String questName, final String arguments) {
            if (plugin.quest(questName) == null) {
                assertTrue(plugin.createQuest(questName).success());
                plugin.quest(questName).addObjective(
                        "KillMobs",
                        Objectives.parse(adapter, type(plugin.registry(), "KillMobs"), arguments),
                        "");
            }
            plugin.registerQuestPlayer(player, "default", true);
            assertTrue(plugin.giveQuest(player, questName, false, ignored -> {}));
            return plugin.activeObjectives(player.playerIdentifier()).getFirst();
        }
    }

    private record EntityEvent(String entityTypeId, String plainCustomName, boolean selfAttributedDeath)
            implements Objectives.EntityEvent {}

    private static final class TestPlayer implements TestPlatformPlayer {
        private final String playerIdentifier;
        private final double distanceToDeath;

        private TestPlayer(final String playerIdentifier, final double distanceToDeath) {
            this.playerIdentifier = playerIdentifier;
            this.distanceToDeath = distanceToDeath;
        }

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return playerIdentifier;
        }

        @Override
        public double distanceTo(final NQLocation location) {
            return distanceToDeath;
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {}

        @Override
        public void sendActionBar(final String miniMessage) {}

        @Override
        public void showProgressBossBar(final String miniMessage, final double progress) {}

        @Override
        public void hideProgressBossBar() {}

        @Override
        public void showTitle(
                final String title,
                final String subtitle,
                final Duration fadeIn,
                final Duration stay,
                final Duration fadeOut) {}

        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {}

        @Override
        public NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
    }
}
