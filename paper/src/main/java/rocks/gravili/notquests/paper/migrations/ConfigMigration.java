package rocks.gravili.notquests.paper.migrations;

interface ConfigMigration {
  String id();

  String introducedInVersion();

  boolean migrate(MigrationContext context);
}
