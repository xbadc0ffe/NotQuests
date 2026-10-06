package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * FORK DIVERGENCE: covers the category active-quest cap. At most maxActiveQuests of a category
 * group can be active at the same time; further accepts are blocked with
 * MAX_ACTIVE_QUESTS_PER_CATEGORY until a held quest ends.
 */
class ForkCategoryMaxActiveTest {
  private static final long NOW = TimeUnit.DAYS.toMillis(100);

  private static Quest.CategoryRules oneActive(final String... questIdentifiers) {
    return new Quest.CategoryRules(-1, 1, Set.of(questIdentifiers));
  }

  private static Quest.AcceptCheck check(
      final Quest quest,
      final Quest.CategoryRules categoryRules,
      final List<String> activeQuestIdentifiers) {
    return Quest.acceptCheck(quest, -1, activeQuestIdentifiers, List.of(), List.of(), categoryRules, NOW);
  }

  @Test
  void blocksSecondQuestWhileACategoryQuestIsActive() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck check = check(quest, oneActive("QuestA", "QuestB"), List.of("QuestA"));

    assertEquals(Quest.AcceptCheck.Status.MAX_ACTIVE_QUESTS_PER_CATEGORY, check.status());
  }

  @Test
  void alreadyAcceptedTakesPrecedenceForTheHeldQuestItself() {
    final Quest quest = new Quest("QuestA");

    final Quest.AcceptCheck check = check(quest, oneActive("QuestA", "QuestB"), List.of("QuestA"));

    assertEquals(Quest.AcceptCheck.Status.ALREADY_ACCEPTED, check.status());
  }

  @Test
  void ignoresActiveQuestsOutsideTheCategoryGroup() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck check =
        check(quest, oneActive("QuestA", "QuestB"), List.of("OtherCategoryQuest"));

    assertEquals(Quest.AcceptCheck.Status.ACCEPTABLE, check.status());
  }

  @Test
  void matchingOfActiveQuestIdentifiersIsCaseInsensitive() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck check = check(quest, oneActive("QUESTA", "QUESTB"), List.of("questa"));

    assertEquals(Quest.AcceptCheck.Status.MAX_ACTIVE_QUESTS_PER_CATEGORY, check.status());
  }

  @Test
  void disabledCapAllowsParallelCategoryQuests() {
    final Quest quest = new Quest("QuestB");

    final Quest.AcceptCheck disabledCap = check(
        quest,
        new Quest.CategoryRules(-1, -1, Set.of("QuestA", "QuestB")),
        List.of("QuestA"));
    final Quest.AcceptCheck noRules = check(quest, null, List.of("QuestA"));

    assertEquals(Quest.AcceptCheck.Status.ACCEPTABLE, disabledCap.status());
    assertEquals(Quest.AcceptCheck.Status.ACCEPTABLE, noRules.status());
  }

  @Test
  void capAboveOneAllowsUpToTheConfiguredAmount() {
    final Quest quest = new Quest("QuestC");
    final Quest.CategoryRules rules =
        new Quest.CategoryRules(-1, 2, Set.of("QuestA", "QuestB", "QuestC"));

    assertEquals(
        Quest.AcceptCheck.Status.ACCEPTABLE,
        check(quest, rules, List.of("QuestA")).status());
    assertEquals(
        Quest.AcceptCheck.Status.MAX_ACTIVE_QUESTS_PER_CATEGORY,
        check(quest, rules, List.of("QuestA", "QuestB")).status());
  }

  @Test
  void capCombinesWithSharedCooldownIndependently() {
    final Quest quest = new Quest("QuestB");
    final Quest.CategoryRules rules =
        new Quest.CategoryRules(1440, 1, Set.of("QuestA", "QuestB"));

    final Quest.AcceptCheck blockedByCap = Quest.acceptCheck(
        quest, -1, List.of("QuestA"), List.of(), List.of(), rules, NOW);
    assertEquals(Quest.AcceptCheck.Status.MAX_ACTIVE_QUESTS_PER_CATEGORY, blockedByCap.status());

    final Quest.AcceptCheck blockedByCooldown = Quest.acceptCheck(
        quest,
        -1,
        List.of(),
        List.of(new QuestPlayer.CompletedQuest("QuestA", "player", NOW - TimeUnit.HOURS.toMillis(10))),
        List.of(),
        rules,
        NOW);
    assertEquals(Quest.AcceptCheck.Status.SHARED_COOLDOWN, blockedByCooldown.status());
  }

  @Test
  void categorySetterTreatsNonPositiveValuesAsDisabled() {
    final Category category = new Category("swordsman");
    assertEquals(-1, category.getMaxActiveQuests());

    category.setMaxActiveQuests(1);
    assertEquals(1, category.getMaxActiveQuests());

    category.setMaxActiveQuests(0);
    assertEquals(-1, category.getMaxActiveQuests());

    category.setMaxActiveQuests(-3);
    assertEquals(-1, category.getMaxActiveQuests());
  }
}
