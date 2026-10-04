package com.notquests.core.managers;

import com.notquests.core.structs.Category;
import com.notquests.core.structs.Quest;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the authoritative configured quest and category repositories. */
public final class QuestManager {
    // FORK DIVERGENCE: quests keep their DEFINITION order - the order they appear in
    // quests.yml, with newly created quests appended. Administrators order that file on
    // purpose, and every listing (NPC GUIs, take lists, suggestions) plus every config
    // re-save flows through getAllQuests(), so sorting here alphabetized the GUIs AND
    // rewrote the admin's hand-ordered file on the first save. Pre-7.0.0 releases kept
    // an insertion-ordered list for the same reason. The synchronized LinkedHashMap
    // keeps computeIfAbsent/putIfAbsent atomic (SynchronizedMap overrides them);
    // iteration happens only inside getAllQuests' synchronized copy.
    private final Map<String, Quest> quests = Collections.synchronizedMap(new LinkedHashMap<>());
    private final ConcurrentHashMap<String, Category> categories = new ConcurrentHashMap<>();

    public QuestManager() {
        ensureDefaultCategory();
    }

    public List<Quest> getAllQuests() {
        synchronized (quests) {
            return List.copyOf(quests.values());
        }
    }

    public List<String> getQuestNames() {
        return getAllQuests().stream().map(Quest::getIdentifier).toList();
    }

    public List<String> getQuestNamesInCategory(final String categoryName) {
        final String checkedCategory = categoryName == null || categoryName.isBlank()
                ? Category.DEFAULT_NAME
                : categoryName;
        return getAllQuests().stream()
                .filter(quest -> Category.same(quest.getCategory(), checkedCategory))
                .map(Quest::getIdentifier)
                .toList();
    }

    public int getQuestCount() {
        return quests.size();
    }

    public Quest getQuest(final String questName) {
        if (questName == null || questName.isBlank()) {
            return null;
        }
        return quests.get(questKey(questName));
    }

    public Quest getOrCreateQuest(final String questName) {
        if (questName == null || questName.isBlank()) {
            throw new IllegalArgumentException("Quest name cannot be blank.");
        }
        ensureDefaultCategory();
        return quests.computeIfAbsent(questKey(questName), ignored -> new Quest(questName));
    }

    public Quest createQuest(final String questName, final String categoryName) {
        if (questName == null || questName.isBlank()) {
            return null;
        }
        final Quest created = new Quest(questName);
        if (categoryName != null && !categoryName.isBlank()) {
            created.setCategory(categoryName);
        }
        if (quests.putIfAbsent(questKey(questName), created) != null) {
            return null;
        }
        getOrCreateCategory(created.getCategory());
        return created;
    }

    public Quest cloneQuest(final String sourceQuestName, final String newQuestName) {
        final Quest source = getQuest(sourceQuestName);
        if (source == null || newQuestName == null || newQuestName.isBlank()) {
            return null;
        }
        final Quest copy = source.copy(newQuestName);
        return quests.putIfAbsent(questKey(newQuestName), copy) == null ? copy : null;
    }

    public Quest removeQuest(final String questName) {
        if (questName == null || questName.isBlank()) {
            return null;
        }
        return quests.remove(questKey(questName));
    }

    public List<Category> getAllCategories() {
        return categories.values().stream()
                .sorted(Comparator.comparing(Category::getIdentifier, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> getCategoryNames() {
        return getAllCategories().stream().map(Category::getIdentifier).toList();
    }

    public List<String> getTopLevelCategoryNames() {
        return getCategoryNames().stream().filter(Category::isTopLevel).toList();
    }

    public int getCategoryCount() {
        return categories.size();
    }

    public String getDefaultCategoryName() {
        return Category.DEFAULT_NAME;
    }

    public Category getCategory(final String categoryName) {
        return categories.get(Category.key(categoryName));
    }

    public Category getOrCreateCategory(final String categoryName) {
        final String canonicalName = Category.canonical(categoryName);
        return categories.computeIfAbsent(Category.key(canonicalName), ignored -> new Category(canonicalName));
    }

    public boolean createCategory(final String categoryName) {
        final String canonicalName = Category.canonical(categoryName);
        return categories.putIfAbsent(Category.key(canonicalName), new Category(canonicalName)) == null;
    }

    public boolean createCategory(final String categoryName, final String parentCategoryName) {
        return createCategory(getCategoryIdentifier(categoryName, parentCategoryName));
    }

    public Category removeCategory(final String categoryName) {
        if (Category.same(Category.DEFAULT_NAME, categoryName)) {
            return null;
        }
        return categories.remove(Category.key(categoryName));
    }

    public String getCategoryIdentifier(final String categoryName, final String parentCategoryName) {
        return parentCategoryName == null || parentCategoryName.isBlank()
                ? Category.canonical(categoryName)
                : Category.join(parentCategoryName, categoryName);
    }

    public String getCategoryDisplayNameOrIdentifier(final String categoryName) {
        final Category category = getCategory(categoryName);
        return category == null ? Category.canonical(categoryName) : category.getDisplayNameOrIdentifier();
    }

    public void ensureDefaultCategory() {
        categories.computeIfAbsent(
                Category.key(Category.DEFAULT_NAME),
                ignored -> new Category(Category.DEFAULT_NAME));
    }

    public void clear() {
        quests.clear();
        categories.clear();
        ensureDefaultCategory();
    }

    private static String questKey(final String questName) {
        return questName.toLowerCase(Locale.ROOT);
    }
}
