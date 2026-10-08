package com.notquests.core.structs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * FORK DIVERGENCE: shared kill credit.
 *
 * <p>Remembers which players hurt which entity so that a death can credit everyone who fought
 * it inside a time window, not only the single player the platform reports as the killer.
 * Identities are the platform's player identifiers and entity ids (UUID strings on Paper).
 * Policy (window length, range, whether an objective accepts shared credit) lives with the
 * caller; this class only keeps the hit log small and answers "who hit this recently".
 *
 * <p>Not thread-safe: it is fed and drained from the server thread. Stale entities are pruned
 * automatically every {@value #PRUNE_EVERY} recorded hits, so mobs that never die (despawned,
 * unloaded, killed with nobody watching) cannot accumulate.
 */
public final class KillCreditTracker {
    static final int PRUNE_EVERY = 256;

    private final Map<String, Map<String, Long>> hits = new HashMap<>();
    private int recordsSincePrune;

    /** Remembers that {@code playerId} hurt {@code entityId} at {@code nowMillis}. */
    public void recordHit(
            final String entityId,
            final String playerId,
            final long nowMillis,
            final long windowMillis) {
        if (blank(entityId) || blank(playerId)) {
            return;
        }
        hits.computeIfAbsent(entityId, ignored -> new LinkedHashMap<>()).put(playerId, nowMillis);
        if (++recordsSincePrune >= PRUNE_EVERY) {
            prune(nowMillis, windowMillis);
        }
    }

    /**
     * Returns, and forgets, every player who hurt {@code entityId} within the last
     * {@code windowMillis}, in first-hit order. Players whose last hit is older than the window
     * are dropped; a player is listed once however often they hit.
     */
    public Set<String> contributors(final String entityId, final long nowMillis, final long windowMillis) {
        final Map<String, Long> entityHits = entityId == null ? null : hits.remove(entityId);
        if (entityHits == null || entityHits.isEmpty()) {
            return Set.of();
        }
        final Set<String> contributors = new LinkedHashSet<>();
        for (final Map.Entry<String, Long> hit : entityHits.entrySet()) {
            if (nowMillis - hit.getValue() <= windowMillis) {
                contributors.add(hit.getKey());
            }
        }
        return contributors;
    }

    /** Drops every hit older than the window and every entity left without hits. */
    public void prune(final long nowMillis, final long windowMillis) {
        recordsSincePrune = 0;
        hits.values().removeIf(entityHits -> {
            entityHits.values().removeIf(hitAt -> nowMillis - hitAt > windowMillis);
            return entityHits.isEmpty();
        });
    }

    /** Number of entities currently remembered (diagnostics and tests). */
    public int trackedEntities() {
        return hits.size();
    }

    private static boolean blank(final String value) {
        return value == null || value.isBlank();
    }
}
