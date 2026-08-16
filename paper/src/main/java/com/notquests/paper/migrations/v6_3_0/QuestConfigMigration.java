package com.notquests.paper.migrations.v6_3_0;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import com.notquests.paper.managers.data.Category;
import com.notquests.paper.migrations.ConfigMigration;
import com.notquests.paper.migrations.MigrationContext;

public final class QuestConfigMigration implements ConfigMigration {
  private static final String ITEM_SELECTION_PATH = "specifics.itemStackSelection";

  @Override
  public String targetVersion() {
    return "6.3.0";
  }

  @Override
  public boolean migrate(final MigrationContext context) {
    boolean changed = false;
    for (final Category category : context.main().getDataManager().getCategories()) {
      final boolean questsChanged = migrateQuestConfig(category.getQuestsConfig());
      final boolean actionsChanged = migrateActionsSection(category.getActionsConfig(), "actions");

      if (questsChanged) {
        category.saveQuestsConfig();
      }
      if (actionsChanged) {
        category.saveActionsConfig();
      }
      changed |= questsChanged || actionsChanged;
    }
    return changed;
  }

  boolean migrateQuestConfig(final FileConfiguration configuration) {
    if (configuration == null) {
      return false;
    }
    boolean changed = false;
    final ConfigurationSection quests = configuration.getConfigurationSection("quests");
    if (quests == null) {
      return false;
    }
    for (final String questName : quests.getKeys(false)) {
      final String questPath = "quests." + questName;
      changed |= migrateObjectivesSection(configuration, questPath + ".objectives");
      changed |= migrateActionsSection(configuration, questPath + ".rewards");
    }
    return changed;
  }

  private boolean migrateObjectivesSection(final FileConfiguration configuration, final String sectionPath) {
    final ConfigurationSection objectives = configuration.getConfigurationSection(sectionPath);
    if (objectives == null) {
      return false;
    }

    boolean changed = false;
    for (final String objectiveId : objectives.getKeys(false)) {
      changed |= migrateObjective(configuration, sectionPath + "." + objectiveId);
    }
    return changed;
  }

  private boolean migrateObjective(final FileConfiguration configuration, final String objectivePath) {
    boolean changed = false;

    String objectiveType = configuration.getString(objectivePath + ".objectiveType", "");
    if ("CollectItems".equals(objectiveType)) {
      objectiveType = "PickupItems";
      configuration.set(objectivePath + ".objectiveType", objectiveType);
      changed = true;
    }

    if (configuration.contains(objectivePath + ".progressNeeded")) {
      configuration.set(
          objectivePath + ".progressNeededExpression",
          String.valueOf(configuration.getInt(objectivePath + ".progressNeeded", 1)));
      configuration.set(objectivePath + ".progressNeeded", null);
      changed = true;
    }

    changed |= switch (objectiveType) {
      case "BreakBlocks" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyMaterial("specifics.blockToBreak.material"));
      case "PlaceBlocks" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyMaterial("specifics.blockToPlace.material"));
      case "PickupItems", "DeliverItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToCollect.itemstack"));
      case "ConsumeItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToConsume.itemstack"));
      case "CraftItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToCraft.itemstack"));
      case "FishItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToFish.itemstack"));
      case "Smelt" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToSmelt.itemstack"));
      default -> false;
    };

    if ("DeliverItems".equals(objectiveType)) {
      changed |= migrateDeliverItemsNpc(configuration, objectivePath);
    }
    if ("TalkToNPC".equals(objectiveType)) {
      changed |= migrateTalkToNpc(configuration, objectivePath);
    }

    changed |= migrateActionsSection(configuration, objectivePath + ".rewards");
    return changed;
  }

  boolean migrateActionsSection(final FileConfiguration configuration, final String sectionPath) {
    if (configuration == null) {
      return false;
    }
    final ConfigurationSection actions = configuration.getConfigurationSection(sectionPath);
    if (actions == null) {
      return false;
    }

    boolean changed = false;
    for (final String actionId : actions.getKeys(false)) {
      changed |= migrateAction(configuration, sectionPath + "." + actionId);
    }
    return changed;
  }

  private boolean migrateAction(final FileConfiguration configuration, final String actionPath) {
    if ("GiveItem".equals(configuration.getString(actionPath + ".actionType", ""))) {
      return migrateItemSelection(
          configuration,
          actionPath,
          legacyItemStack("specifics.item"),
          legacyItemStack("specifics.rewardItem"));
    }
    return false;
  }

  private boolean migrateDeliverItemsNpc(final FileConfiguration configuration, final String objectivePath) {
    final String recipientPath = objectivePath + ".specifics.recipientNPC";
    if (configuration.contains(recipientPath + ".type")) {
      return false;
    }

    final String citizensPath = objectivePath + ".specifics.recipientNPCID";
    final String armorStandPath = objectivePath + ".specifics.recipientArmorStandID";
    if (configuration.contains(citizensPath)) {
      configuration.set(recipientPath + ".type", "citizens");
      configuration.set(recipientPath + ".integerID", configuration.getInt(citizensPath));
      configuration.set(citizensPath, null);
      configuration.set(armorStandPath, null);
      return true;
    }
    if (configuration.contains(armorStandPath)) {
      configuration.set(recipientPath + ".type", "armorstand");
      configuration.set(recipientPath + ".uuidID", configuration.getString(armorStandPath));
      configuration.set(armorStandPath, null);
      configuration.set(citizensPath, null);
      return true;
    }
    return false;
  }

  private boolean migrateTalkToNpc(final FileConfiguration configuration, final String objectivePath) {
    final String npcPath = objectivePath + ".specifics.npcToTalkTo";
    if (configuration.contains(npcPath + ".type")) {
      return false;
    }

    final String citizensPath = objectivePath + ".specifics.NPCtoTalkID";
    final String armorStandPath = objectivePath + ".specifics.ArmorStandToTalkUUID";
    if (configuration.contains(citizensPath)) {
      configuration.set(npcPath + ".type", "citizens");
      configuration.set(npcPath + ".integerID", configuration.getInt(citizensPath));
      configuration.set(citizensPath, null);
      configuration.set(armorStandPath, null);
      return true;
    }
    if (configuration.contains(armorStandPath)) {
      configuration.set(npcPath + ".type", "armorstand");
      configuration.set(npcPath + ".uuidID", configuration.getString(armorStandPath));
      configuration.set(armorStandPath, null);
      configuration.set(citizensPath, null);
      return true;
    }
    return false;
  }

  private boolean migrateItemSelection(
      final FileConfiguration configuration,
      final String basePath,
      final LegacySelectionSource... sources) {
    final String nqItemPath = basePath + ".specifics.nqitem";
    boolean changed = false;
    final boolean hasNqItem = configuration.contains(nqItemPath);
    final String nqItemName = configuration.getString(nqItemPath, "");

    if (hasNqItem && nqItemName != null && !nqItemName.isBlank()) {
      appendSelectionValue(configuration, basePath, "nqItems", nqItemName);
      changed = true;
    }

    if (!hasNqItem || nqItemName == null || nqItemName.isBlank()) {
      for (final LegacySelectionSource source : sources) {
        final String sourcePath = basePath + "." + source.relativePath();
        if (!configuration.contains(sourcePath)) {
          continue;
        }
        final Object value = configuration.get(sourcePath);
        if (value != null) {
          appendSelectionValue(configuration, basePath, source.targetSection(), value);
        }
        changed = true;
      }
    }

    if (hasNqItem) {
      configuration.set(nqItemPath, null);
      changed = true;
    }
    for (final LegacySelectionSource source : sources) {
      final String sourcePath = basePath + "." + source.relativePath();
      if (configuration.contains(sourcePath)) {
        configuration.set(sourcePath, null);
        changed = true;
      }
    }

    if (changed && !configuration.contains(basePath + "." + ITEM_SELECTION_PATH + ".any")) {
      configuration.set(basePath + "." + ITEM_SELECTION_PATH + ".any", false);
    }
    return changed;
  }

  private void appendSelectionValue(
      final FileConfiguration configuration,
      final String basePath,
      final String targetSection,
      final Object value) {
    final String sectionPath = basePath + "." + ITEM_SELECTION_PATH + "." + targetSection;
    configuration.set(sectionPath + "." + nextNumericKey(configuration, sectionPath), value);
  }

  private int nextNumericKey(final FileConfiguration configuration, final String sectionPath) {
    final ConfigurationSection section = configuration.getConfigurationSection(sectionPath);
    if (section == null) {
      return 1;
    }
    int highest = 0;
    for (final String key : section.getKeys(false)) {
      try {
        highest = Math.max(highest, Integer.parseInt(key));
      } catch (final NumberFormatException ignored) {
        // Non-numeric custom keys are left alone; migrated entries continue after numeric keys.
      }
    }
    return highest + 1;
  }

  private LegacySelectionSource legacyMaterial(final String relativePath) {
    return new LegacySelectionSource(relativePath, "materials");
  }

  private LegacySelectionSource legacyItemStack(final String relativePath) {
    return new LegacySelectionSource(relativePath, "itemStacks");
  }

  private record LegacySelectionSource(String relativePath, String targetSection) {}
}
