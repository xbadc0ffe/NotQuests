package rocks.gravili.notquests.paper.migrations;

import rocks.gravili.notquests.paper.NotQuests;

record MigrationContext(NotQuests main, String previousVersion, String currentVersion) {}
