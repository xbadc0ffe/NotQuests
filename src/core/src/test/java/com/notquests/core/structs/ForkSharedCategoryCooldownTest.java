package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.notquests.core.config.YamlConfig;
import com.notquests.core.managers.ConfigurationManager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * FORK DIVERGENCE: covers the shared (category-wide) accept cooldown. Completing any quest of a
 * shared group blocks accepting every quest of the group until the shared window has passed,
 * independently of each quest's own accept cooldown.
 */
class ForkSharedCategoryCooldownTest {
  private static final long NOW = TimeUnit.DAYS.toMillis(100);
  private static final long TEN_HOURS_AGO = NOW - TimeUnit.HOURS.toMillis(10);
  private static final long ONE_DAY_MINUTES = 1440;

  private static Quest.CategoryRules sharedDay(final String... questIdentifiers) {
    return new Quest.CategoryRules(ONE_DAY_MINUTES, Set.of(questIdentifiers));
  }

  private static Quest.AcceptCheck check(
      final Quest quest,
      final Quest.CategoryRules categoryRules,
      final List<QuestPlayer.CompletedQuest> completed,
      final List<QuestPlayer.FailedQuest> failed) {
    return Quest.acceptCheck(quest, -1, List.of(), completed, failed, categoryRules, NOW);
  }

  @Test
  void sharedCooldownBlocksSiblingQuestAfterAnyGroupCompletion() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck check = check(
        quest,
        sharedDay("QuestA", "QuestB"),
        List.of(new QuestPlayer.CompletedQuest("QuestA", "player", TEN_HOURS_AGO)),
        List.of());

    assertEquals(Quest.AcceptCheck.Status.SHARED_COOLDOWN, check.status());
    assertEquals(ONE_DAY_MINUTES - TimeUnit.HOURS.toMinutes(10), check.timeToWaitInMinutes());
  }

  @Test
  void sharedCooldownCoversTheCompletedQuestItself() {
    final Quest quest = new Quest("QuestA");

    final Quest.AcceptCheck check = check(
        quest,
        sharedDay("QuestA", "QuestB"),
        List.of(new QuestPlayer.CompletedQuest("QuestA", "player", TEN_HOURS_AGO)),
        List.of());

    assertEquals(Quest.AcceptCheck.Status.SHARED_COOLDOWN, check.status());
  }

  @Test
  void sharedCooldownExpiresAfterItsWindow() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck check = check(
        quest,
        sharedDay("QuestA", "QuestB"),
        List.of(new QuestPlayer.CompletedQuest("QuestA", "player", NOW - TimeUnit.HOURS.toMillis(25))),
        List.of());

    assertEquals(Quest.AcceptCheck.Status.ACCEPTABLE, check.status());
  }

  @Test
  void questOwnCooldownStatusWinsWhenItIsTheLongerWait() {
    final Quest quest = new Quest("QuestA");
    quest.setAcceptCooldownComplete(2 * ONE_DAY_MINUTES);

    final Quest.AcceptCheck check = check(
        quest,
        sharedDay("QuestA", "QuestB"),
        List.of(new QuestPlayer.CompletedQuest("QuestA", "player", TEN_HOURS_AGO)),
        List.of());

    assertEquals(Quest.AcceptCheck.Status.COOLDOWN, check.status());
    assertEquals(2 * ONE_DAY_MINUTES - TimeUnit.HOURS.toMinutes(10), check.timeToWaitInMinutes());
  }

  @Test
  void sharedCooldownStatusWinsWhenItIsTheLongerWait() {
    final Quest quest = new Quest("QuestB");
    quest.setAcceptCooldownComplete(60);

    final Quest.AcceptCheck check = check(
        quest,
        sharedDay("QuestA", "QuestB"),
        List.of(new QuestPlayer.CompletedQuest("QuestA", "player", TEN_HOURS_AGO)),
        List.of());

    assertEquals(Quest.AcceptCheck.Status.SHARED_COOLDOWN, check.status());
    assertEquals(ONE_DAY_MINUTES - TimeUnit.HOURS.toMinutes(10), check.timeToWaitInMinutes());
  }

  @Test
  void ignoresCompletionsOutsideTheSharedGroup() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck check = check(
        quest,
        sharedDay("QuestA", "QuestB"),
        List.of(new QuestPlayer.CompletedQuest("OtherCategoryQuest", "player", TEN_HOURS_AGO)),
        List.of());

    assertEquals(Quest.AcceptCheck.Status.ACCEPTABLE, check.status());
  }

  @Test
  void failedQuestsDoNotStartTheSharedCooldown() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck check = check(
        quest,
        sharedDay("QuestA", "QuestB"),
        List.of(),
        List.of(new QuestPlayer.FailedQuest("QuestA", "player", TEN_HOURS_AGO)));

    assertEquals(Quest.AcceptCheck.Status.ACCEPTABLE, check.status());
  }

  @Test
  void sharedGroupMatchingIsCaseInsensitive() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck check = check(
        quest,
        sharedDay("QUESTA", "QUESTB"),
        List.of(new QuestPlayer.CompletedQuest("questa", "player", TEN_HOURS_AGO)),
        List.of());

    assertEquals(Quest.AcceptCheck.Status.SHARED_COOLDOWN, check.status());
    assertTrue(new Quest.CategoryRules(1, Set.of("QuestA")).includes("qUeStA"));
    assertFalse(new Quest.CategoryRules(1, Set.of("QuestA")).includes("QuestB"));
  }

  @Test
  void legacyAcceptCheckOverloadKeepsOldBehaviour() {
    final Quest quest = new Quest("QuestA");
    quest.setAcceptCooldownComplete(ONE_DAY_MINUTES);

    final Quest.AcceptCheck check = Quest.acceptCheck(
        quest,
        -1,
        List.of(),
        List.of(new QuestPlayer.CompletedQuest("QuestA", "player", TEN_HOURS_AGO)),
        List.of(),
        NOW);

    assertEquals(Quest.AcceptCheck.Status.COOLDOWN, check.status());
    assertEquals(ONE_DAY_MINUTES - TimeUnit.HOURS.toMinutes(10), check.timeToWaitInMinutes());
  }

  @Test
  void cooldownDisplayFormatsSharedCooldownLikeARegularCooldown() {
    final Quest.CooldownDisplay display = Quest.CooldownDisplay.from(new Quest.AcceptCheck(
        Quest.AcceptCheck.Status.SHARED_COOLDOWN, 0, 0, 0, 3 * ONE_DAY_MINUTES));

    assertEquals(Quest.CooldownDisplay.Bucket.DAYS, display.bucket());
    assertEquals("3.0", display.value());
  }

  @Test
  void visibilityHidesQuestBlockedByTheSharedCooldown() {
    final Quest quest = new Quest("QuestB");
    final QuestPlayer player = new QuestPlayer("player", "default");
    player.addCompletedQuest(new QuestPlayer.CompletedQuest("QuestA", "player", TEN_HOURS_AGO));

    final YamlConfig yaml = YamlConfig.empty();
    yaml.set("gui.quest-visibility-evaluations.accept-cooldown.enabled", true);
    final ConfigurationManager settings = new ConfigurationManager();
    settings.loadFrom(yaml);

    assertEquals(List.of(), Quest.visibleQuestIdentifiers(
        List.of(quest),
        player,
        settings,
        NOW,
        ignored -> true,
        ignored -> sharedDay("QuestA", "QuestB")));
    assertEquals(List.of("QuestB"), Quest.visibleQuestIdentifiers(
        List.of(quest),
        player,
        settings,
        NOW,
        ignored -> true,
        ignored -> null));
  }

  @Test
  void categorySetterTreatsNonPositiveValuesAsDisabled() {
    final Category category = new Category("explorer");
    assertEquals(-1, category.getSharedAcceptCooldownComplete());

    category.setSharedAcceptCooldownComplete(ONE_DAY_MINUTES);
    assertEquals(ONE_DAY_MINUTES, category.getSharedAcceptCooldownComplete());

    category.setSharedAcceptCooldownComplete(0);
    assertEquals(-1, category.getSharedAcceptCooldownComplete());

    category.setSharedAcceptCooldownComplete(-5);
    assertEquals(-1, category.getSharedAcceptCooldownComplete());
  }
}
