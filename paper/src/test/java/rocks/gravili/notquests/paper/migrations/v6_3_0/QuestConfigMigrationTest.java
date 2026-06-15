package rocks.gravili.notquests.paper.migrations.v6_3_0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class QuestConfigMigrationTest {
  private final QuestConfigMigration migration = new QuestConfigMigration();

  @Test
  void migratesLegacyObjectiveItemSelectionAndProgressNeeded() {
    final YamlConfiguration config = new YamlConfiguration();
    final String path = "quests.TestQuest.objectives.1";
    config.set(path + ".objectiveType", "BreakBlocks");
    config.set(path + ".progressNeeded", 12);
    config.set(path + ".specifics.blockToBreak.material", "DIRT");

    assertTrue(migration.migrateQuestConfig(config));

    assertEquals("BreakBlocks", config.getString(path + ".objectiveType"));
    assertEquals("12", config.getString(path + ".progressNeededExpression"));
    assertFalse(config.contains(path + ".progressNeeded"));
    assertEquals("DIRT", config.getString(path + ".specifics.itemStackSelection.materials.1"));
    assertFalse(config.getBoolean(path + ".specifics.itemStackSelection.any"));
    assertFalse(config.contains(path + ".specifics.blockToBreak.material"));
  }

  @Test
  void migratesCollectItemsToPickupItemsBeforeTypeSpecificMigration() {
    final YamlConfiguration config = new YamlConfiguration();
    final String path = "quests.TestQuest.objectives.1";
    final ItemStack legacyStack = new ItemStack(Material.APPLE);
    config.set(path + ".objectiveType", "CollectItems");
    config.set(path + ".specifics.itemToCollect.itemstack", legacyStack);

    assertTrue(migration.migrateQuestConfig(config));

    assertEquals("PickupItems", config.getString(path + ".objectiveType"));
    assertEquals(legacyStack, config.getItemStack(path + ".specifics.itemStackSelection.itemStacks.1"));
    assertFalse(config.contains(path + ".specifics.itemToCollect.itemstack"));
  }

  @Test
  void migratesGiveItemActionsInActionsYmlAndQuestRewards() {
    final YamlConfiguration config = new YamlConfiguration();
    final ItemStack directActionStack = new ItemStack(Material.STONE);
    final ItemStack questRewardStack = new ItemStack(Material.DIAMOND);
    config.set("actions.GiveStone.actionType", "GiveItem");
    config.set("actions.GiveStone.specifics.item", directActionStack);
    config.set("quests.TestQuest.rewards.1.actionType", "GiveItem");
    config.set("quests.TestQuest.rewards.1.specifics.rewardItem", questRewardStack);

    assertTrue(migration.migrateActionsSection(config, "actions"));
    assertTrue(migration.migrateQuestConfig(config));

    assertEquals(directActionStack, config.getItemStack("actions.GiveStone.specifics.itemStackSelection.itemStacks.1"));
    assertEquals(questRewardStack, config.getItemStack("quests.TestQuest.rewards.1.specifics.itemStackSelection.itemStacks.1"));
    assertFalse(config.contains("actions.GiveStone.specifics.item"));
    assertFalse(config.contains("quests.TestQuest.rewards.1.specifics.rewardItem"));
  }

  @Test
  void migratesDeliverItemsLegacyNpcReference() {
    final YamlConfiguration config = new YamlConfiguration();
    final String path = "quests.TestQuest.objectives.1";
    config.set(path + ".objectiveType", "DeliverItems");
    config.set(path + ".specifics.recipientNPCID", 42);

    assertTrue(migration.migrateQuestConfig(config));

    assertEquals("citizens", config.getString(path + ".specifics.recipientNPC.type"));
    assertEquals(42, config.getInt(path + ".specifics.recipientNPC.integerID"));
    assertFalse(config.contains(path + ".specifics.recipientNPCID"));
  }
}
