package rocks.gravili.notquests.paper.migrations;

import java.util.List;
import rocks.gravili.notquests.paper.NotQuests;

public final class ConfigMigrationManager {
  private final NotQuests main;
  private final List<ConfigMigration> migrations;

  public ConfigMigrationManager(final NotQuests main) {
    this.main = main;
    this.migrations = List.of(new LegacyQuestConfigMigration());
  }

  public void runStartupMigrations() {
    final String previousVersion = main.getConfiguration().getDataMigrationVersion();
    final String currentVersion = main.getMain().getDescription().getVersion();
    final MigrationContext context = new MigrationContext(main, previousVersion, currentVersion);

    boolean changed = false;
    for (final ConfigMigration migration : migrations) {
      if (!VersionNumber.parse(previousVersion).isBefore(migration.introducedInVersion())) {
        continue;
      }
      main.getLogManager().info("Running config migration <highlight>" + migration.id()
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
