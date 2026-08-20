package com.notquests.paper.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.managers.items.NQItem;

/**
 * Regression test for DATACTX-001: {@code FieldTypes.itemSelection()} must serialize an
 * {@link ItemStackSelection} through {@code ItemStackSelection.saveToFileConfiguration} rather
 * than handing the live object to {@code FileConfiguration.set}. The live object is not
 * {@code ConfigurationSerializable}, so SnakeYAML emits it as a JavaBean under a global
 * {@code !!} tag — every item is dropped on write, and Bukkit's SafeConstructor-based loader
 * then refuses the whole file. Each test drives the real {@code FieldType.save}/{@code load}
 * pair (package-private, hence this package) and asserts the emitted YAML carries the
 * documented structure, parses again, and loads back to an equivalent selection. All four
 * on-disk shapes are covered: materials, itemStacks (with an enchantment), nqItems, and any.
 * A mock server (MockBukkit) is required because ItemStack creation and serialization go
 * through the server's item factory.
 */
class FieldTypesItemSelectionRoundTripTest {

  private static final String BASE = "quests.TestQuest.objectives.1";
  private static final String SELECTION_PATH = BASE + ".specifics.itemStackSelection";

  private NotQuests main;

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
    main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private YamlConfiguration saveAndReparse(final ItemStackSelection selection) {
    final FieldType<ItemStackSelection> field =
        FieldTypes.itemSelection().config("specifics.itemStackSelection");
    final YamlConfiguration configuration = new YamlConfiguration();
    configuration.set(BASE + ".objectiveType", "PickupItems");
    field.save(main, configuration, BASE, selection);

    final String yaml = configuration.saveToString();
    assertFalse(
        yaml.contains("!!"),
        "emitted YAML must not carry a global !! tag (live-object dump); got:\n" + yaml);

    final YamlConfiguration reparsed = new YamlConfiguration();
    assertDoesNotThrow(
        () -> reparsed.loadFromString(yaml),
        "a file written by itemSelection().save must be loadable again");
    return reparsed;
  }

  private ItemStackSelection loadBack(final YamlConfiguration reparsed) {
    final FieldType<ItemStackSelection> field =
        FieldTypes.itemSelection().config("specifics.itemStackSelection");
    final ItemStackSelection loaded = field.load(main, reparsed, BASE);
    assertNotNull(loaded);
    return loaded;
  }

  @Test
  @DisplayName("materials-shape selection survives save -> reparse -> load")
  void materialsShapeRoundTrips() {
    final ItemStackSelection selection = new ItemStackSelection(main);
    selection.addMaterial(Material.WHEAT);
    selection.addMaterial(Material.OAK_LOG);

    final YamlConfiguration reparsed = saveAndReparse(selection);
    assertEquals("WHEAT", reparsed.getString(SELECTION_PATH + ".materials.1"));
    assertEquals("OAK_LOG", reparsed.getString(SELECTION_PATH + ".materials.2"));
    assertFalse(reparsed.getBoolean(SELECTION_PATH + ".any"));

    final ItemStackSelection loaded = loadBack(reparsed);
    assertTrue(loaded.checkIfIsIncluded(Material.WHEAT));
    assertTrue(loaded.checkIfIsIncluded(Material.OAK_LOG));
    assertFalse(loaded.checkIfIsIncluded(Material.STONE));
    assertFalse(loaded.isAny());
  }

  @Test
  @DisplayName("itemStacks-shape selection (enchanted stack) survives save -> reparse -> load")
  void itemStacksShapeRoundTrips() {
    final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 3);
    sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 2);

    final ItemStackSelection selection = new ItemStackSelection(main);
    selection.addItemStack(sword);

    final YamlConfiguration reparsed = saveAndReparse(selection);
    final ItemStack reloadedStack = reparsed.getItemStack(SELECTION_PATH + ".itemStacks.1");
    assertNotNull(reloadedStack, "serialized ItemStack must deserialize from the reparsed file");
    assertEquals(Material.DIAMOND_SWORD, reloadedStack.getType());
    assertEquals(3, reloadedStack.getAmount());
    assertEquals(2, reloadedStack.getEnchantmentLevel(Enchantment.SHARPNESS));

    final ItemStackSelection loaded = loadBack(reparsed);
    assertTrue(loaded.checkIfIsIncluded(sword));
    assertFalse(loaded.isAny());
  }

  @Test
  @DisplayName("nqItems-shape selection survives save -> reparse -> load")
  void nqItemsShapeRoundTrips() {
    final NQItem nqItem = mock(NQItem.class);
    when(nqItem.getItemName()).thenReturn("testitem");
    when(main.getItemsManager().getItem("testitem")).thenReturn(nqItem);

    final ItemStackSelection selection = new ItemStackSelection(main);
    selection.addNqItem(nqItem);

    final YamlConfiguration reparsed = saveAndReparse(selection);
    assertEquals("testitem", reparsed.getString(SELECTION_PATH + ".nqItems.1"));

    final ItemStackSelection loaded = loadBack(reparsed);
    assertTrue(loaded.hasNQItem());
  }

  @Test
  @DisplayName("any-shape selection survives save -> reparse -> load")
  void anyShapeRoundTrips() {
    final ItemStackSelection selection = new ItemStackSelection(main);
    selection.setAny(true);

    final YamlConfiguration reparsed = saveAndReparse(selection);
    assertTrue(reparsed.getBoolean(SELECTION_PATH + ".any"));

    final ItemStackSelection loaded = loadBack(reparsed);
    assertTrue(loaded.isAny());
    assertTrue(loaded.checkIfIsIncluded(Material.WHEAT), "any-selection matches everything");
  }
}
