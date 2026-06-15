package rocks.gravili.notquests.paper.migrations;

import rocks.gravili.notquests.paper.NotQuests;

public record MigrationContext(NotQuests main, String previousVersion, String currentVersion) {}
