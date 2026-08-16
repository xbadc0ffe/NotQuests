package com.notquests.paper.migrations;

public interface ConfigMigration {
  String targetVersion();

  boolean migrate(MigrationContext context);
}
