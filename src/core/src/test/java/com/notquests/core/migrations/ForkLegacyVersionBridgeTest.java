package com.notquests.core.migrations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.config.YamlConfig;
import com.notquests.core.migrations.ConfigurationMigrations.Context;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FORK DIVERGENCE: fork builds 26.2.1 and 26.2.2 stored their own version in
 * data-migration-version-do-not-edit while still writing pre-v7 data shapes. Because
 * 26.x parses above the 7.0.0 migration target, the one-time v7 conversion would be
 * silently skipped for them. These tests pin the bridge that maps exactly those two
 * stored versions into the pre-7.0.0 space, and prove the gate math end to end
 * against the real migration list.
 */
class ForkLegacyVersionBridgeTest {
    @TempDir
    Path dataFolder;

    @Test
    void legacyForkVersionsReadAsPreV7() {
        assertEquals("6.9.9", storedVersionReadsAs("26.2.1"));
        assertEquals("6.9.9", storedVersionReadsAs("26.2.2"));
    }

    @Test
    void currentForkVersionsAndUpstreamVersionsAreUntouched() {
        assertEquals("26.3.3", storedVersionReadsAs("26.3.3"));
        assertEquals("6.3.0", storedVersionReadsAs("6.3.0"));
        assertEquals("7.0.0", storedVersionReadsAs("7.0.0"));
    }

    @Test
    void v7MigrationRunsForDataStoredByLegacyForkBuilds() {
        final RunResult result = runStartupGate("26.2.2", "26.3.3");

        assertEquals(1, result.backups.get(), "the v7 migration must be selected for 26.2.x data");
        assertEquals("7.0.0", result.backupTarget.get());
        assertEquals("26.3.3", result.savedVersion.get(), "the stored version must advance to the fork build");
    }

    @Test
    void v7MigrationStaysSkippedForDataStoredByV7FormatForkBuilds() {
        final RunResult result = runStartupGate("26.3.3", "26.3.4");

        assertEquals(0, result.backups.get(), "post-v7 fork data must not re-run the v7 conversion");
        assertEquals("26.3.4", result.savedVersion.get(), "the version marker still advances");
    }

    @Test
    void naturalUpstreamUpgradePathIsUnaffected() {
        final RunResult result = runStartupGate("6.3.0", "26.3.3");

        assertEquals(1, result.backups.get());
        assertEquals("7.0.0", result.backupTarget.get());
        assertEquals("26.3.3", result.savedVersion.get());
    }

    private static String storedVersionReadsAs(final String stored) {
        return ConfigurationMigrations.dataVersion(YamlConfig.fromMap(Map.of(
                ConfigurationMigrations.DATA_VERSION, stored)));
    }

    private RunResult runStartupGate(final String storedVersion, final String currentVersion) {
        final YamlConfig generalConfig = YamlConfig.fromMap(Map.of(
                ConfigurationMigrations.DATA_VERSION, storedVersion));
        final Context context = new Context(dataFolder, generalConfig, (message, exception) -> {});
        final RunResult result = new RunResult();

        new ConfigurationMigrations().run(new ConfigurationMigrations.MigrationRun<>(
                ConfigurationMigrations.dataVersion(generalConfig),
                currentVersion,
                context,
                ignored -> {},
                target -> {
                    result.backups.incrementAndGet();
                    result.backupTarget.set(target);
                },
                result.savedVersion::set));
        return result;
    }

    private static final class RunResult {
        final AtomicInteger backups = new AtomicInteger();
        final AtomicReference<String> backupTarget = new AtomicReference<>();
        final AtomicReference<String> savedVersion = new AtomicReference<>();
    }
}
