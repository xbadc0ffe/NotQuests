package com.notquests.paper.migrations;

import com.notquests.paper.NotQuests;

public record MigrationContext(NotQuests main, String previousVersion, String currentVersion) {}
