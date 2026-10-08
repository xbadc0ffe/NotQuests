package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

/**
 * FORK DIVERGENCE: shared kill credit. The tracker remembers who hurt which entity, pays each
 * entity out once, honours the time window, and prunes itself so mobs that never die cannot
 * accumulate.
 */
class ForkKillCreditTrackerTest {
    private static final long WINDOW = 30_000L;

    @Test
    void contributorsAreReturnedOnceInFirstHitOrderAndThenForgotten() {
        final KillCreditTracker tracker = new KillCreditTracker();
        tracker.recordHit("mob", "alice", 1_000L, WINDOW);
        tracker.recordHit("mob", "bob", 2_000L, WINDOW);
        tracker.recordHit("mob", "alice", 3_000L, WINDOW);
        tracker.recordHit("other", "carol", 3_000L, WINDOW);

        final Set<String> contributors = tracker.contributors("mob", 4_000L, WINDOW);

        assertEquals(List.of("alice", "bob"), List.copyOf(contributors));
        assertEquals(Set.of(), tracker.contributors("mob", 4_000L, WINDOW), "a mob pays out once");
        assertEquals(1, tracker.trackedEntities(), "unrelated entities stay tracked");
        assertEquals(Set.of("carol"), tracker.contributors("other", 4_000L, WINDOW));
    }

    @Test
    void hitsOlderThanTheWindowDoNotCount() {
        final KillCreditTracker tracker = new KillCreditTracker();
        tracker.recordHit("mob", "alice", 0L, WINDOW);
        tracker.recordHit("mob", "bob", 10_000L, WINDOW);

        assertEquals(Set.of("bob"), tracker.contributors("mob", WINDOW + 5_000L, WINDOW));
        assertEquals(Set.of(), tracker.contributors("mob", 1L, WINDOW), "already paid out");
        assertEquals(Set.of(), tracker.contributors("unknown", 1L, WINDOW));
        assertEquals(Set.of(), tracker.contributors(null, 1L, WINDOW));
    }

    @Test
    void pruneDropsStaleHitsAndEmptyEntities() {
        final KillCreditTracker tracker = new KillCreditTracker();
        tracker.recordHit("stale", "alice", 0L, WINDOW);
        tracker.recordHit("mixed", "alice", 0L, WINDOW);
        tracker.recordHit("mixed", "bob", 50_000L, WINDOW);
        tracker.recordHit("fresh", "carol", 60_000L, WINDOW);

        tracker.prune(60_000L, WINDOW);

        assertEquals(2, tracker.trackedEntities());
        assertEquals(Set.of("bob"), tracker.contributors("mixed", 60_000L, WINDOW));
        assertEquals(Set.of("carol"), tracker.contributors("fresh", 60_000L, WINDOW));
    }

    @Test
    void recordingHitsPrunesAutomaticallySoForgottenMobsCannotAccumulate() {
        final KillCreditTracker tracker = new KillCreditTracker();
        for (int mob = 0; mob < KillCreditTracker.PRUNE_EVERY - 1; mob++) {
            tracker.recordHit("mob-" + mob, "alice", 0L, WINDOW);
        }
        assertEquals(KillCreditTracker.PRUNE_EVERY - 1, tracker.trackedEntities());

        tracker.recordHit("late", "bob", WINDOW * 10, WINDOW);

        assertEquals(1, tracker.trackedEntities(), "the prune threshold drops every stale mob");
        assertEquals(Set.of("bob"), tracker.contributors("late", WINDOW * 10, WINDOW));
    }

    @Test
    void blankIdentitiesAreIgnored() {
        final KillCreditTracker tracker = new KillCreditTracker();
        tracker.recordHit(null, "alice", 0L, WINDOW);
        tracker.recordHit("", "alice", 0L, WINDOW);
        tracker.recordHit("mob", null, 0L, WINDOW);
        tracker.recordHit("mob", " ", 0L, WINDOW);

        assertEquals(0, tracker.trackedEntities());
        assertTrue(tracker.contributors("mob", 0L, WINDOW).isEmpty());
    }
}
