package rocks.gravili.notquests.paper.migrations;

import java.util.List;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.migrations.v6_3_0.QuestConfigMigration;

public final class ConfigMigrationManager {
  private final NotQuests main;
  private final List<ConfigMigration> migrations;

  public ConfigMigrationManager(final NotQuests main) {
    this.main = main;
    this.migrations = List.<ConfigMigration>of(new QuestConfigMigration()).stream()
        .sorted((first, second) -> VersionNumber.parse(first.targetVersion())
            .compareTo(VersionNumber.parse(second.targetVersion())))
        .toList();
  }

  public void runStartupMigrations() {
    final String previousVersion = main.getConfiguration().getDataMigrationVersion();
    final String currentVersion = main.getMain().getDescription().getVersion();
    final MigrationContext context = new MigrationContext(main, previousVersion, currentVersion);

    boolean changed = false;
    for (final ConfigMigration migration : migrations) {
      if (!VersionNumber.parse(previousVersion).isBefore(migration.targetVersion())) {
        continue;
      }
      main.getLogManager().info("Running config migration up to <highlight>" + migration.targetVersion()
          + "</highlight> from version <highlight2>" + displayVersion(previousVersion)
          + "</highlight2>...");
      changed |= migration.migrate(context);
    }

    if (changed) {
      main.getLogManager().info("Config migrations completed and changed one or more category files.");
    }
    if (VersionNumber.parse(previousVersion).isBefore(currentVersion)) {
      main.getDataManager().getGeneralConfig().set("data-migration-version-do-not-edit", currentVersion);
      main.getConfiguration().setDataMigrationVersion(currentVersion);
      main.getDataManager().saveGeneralConfig();
    }
  }

  private static String displayVersion(final String version) {
    return version == null || version.isBlank() ? "unknown" : version;
  }
}
