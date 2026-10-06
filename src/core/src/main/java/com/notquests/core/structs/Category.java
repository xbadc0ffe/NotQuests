package com.notquests.core.structs;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;

import java.util.Locale;

public final class Category {
    public static final String DEFAULT_NAME = "default";

    private final String identifier;
    private String displayName = "";
    private String progressOrder = "";
    private ItemSelection guiItemSelection;
    private boolean guiItemGlow;
    private int conversationDelayMillis;
    // FORK DIVERGENCE: shared accept cooldown (minutes) across every quest in this category;
    // -1 or 0 = disabled. Completing any category quest starts the shared window.
    private long sharedAcceptCooldownComplete = -1;
    // FORK DIVERGENCE: cap on simultaneously active quests from this category; -1 or 0 = disabled.
    private int maxActiveQuests = -1;

    public Category(final String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Category identifier cannot be blank.");
        }
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayNameOrIdentifier() {
        return displayName == null || displayName.isBlank() ? identifier : displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName == null ? "" : displayName;
    }

    public String getProgressOrder() {
        return progressOrder;
    }

    public void setProgressOrder(final String progressOrder) {
        this.progressOrder = progressOrder == null ? "" : progressOrder;
    }

    public String getGuiItem() {
        return guiItemSelection == null ? "" : guiItemSelection.listedMaterials("");
    }

    public void setGuiItem(final String guiItem) {
        setGuiItem(guiItem == null || guiItem.isBlank() ? null : ItemStackSelection.parse(guiItem));
    }

    public ItemSelection getGuiItemSelection() {
        return guiItemSelection;
    }

    public void setGuiItem(final ItemSelection guiItemSelection) {
        this.guiItemSelection = guiItemSelection;
    }

    public boolean isGuiItemGlow() {
        return guiItemGlow;
    }

    public void setGuiItemGlow(final boolean guiItemGlow) {
        this.guiItemGlow = guiItemGlow;
    }

    public int getConversationDelayInMS() {
        return conversationDelayMillis;
    }

    public void setConversationDelayInMS(final int conversationDelayMillis) {
        this.conversationDelayMillis = Math.max(conversationDelayMillis, 0);
    }

    public long getSharedAcceptCooldownComplete() {
        return sharedAcceptCooldownComplete;
    }

    public void setSharedAcceptCooldownComplete(final long sharedAcceptCooldownCompleteMinutes) {
        this.sharedAcceptCooldownComplete =
                sharedAcceptCooldownCompleteMinutes <= 0 ? -1 : sharedAcceptCooldownCompleteMinutes;
    }

    public int getMaxActiveQuests() {
        return maxActiveQuests;
    }

    public void setMaxActiveQuests(final int maxActiveQuests) {
        this.maxActiveQuests = maxActiveQuests <= 0 ? -1 : maxActiveQuests;
    }

    public static String canonical(final String categoryName) {
        return categoryName == null || categoryName.isBlank()
                ? DEFAULT_NAME
                : categoryName.trim();
    }

    public static String join(final String parentCategoryName, final String childCategoryName) {
        final String child = canonical(childCategoryName);
        if (parentCategoryName == null || parentCategoryName.isBlank()) {
            return child;
        }
        return canonical(parentCategoryName) + "." + child;
    }

    public static boolean same(final String first, final String second) {
        return canonical(first).equalsIgnoreCase(canonical(second));
    }

    public static boolean isDefault(final String categoryName) {
        return same(categoryName, DEFAULT_NAME);
    }

    public static boolean isTopLevel(final String categoryName) {
        return !canonical(categoryName).contains(".");
    }

    public static String parent(final String categoryName) {
        final String canonical = canonical(categoryName);
        final int separator = canonical.lastIndexOf('.');
        return separator < 0 ? "" : canonical.substring(0, separator);
    }

    public static String leaf(final String categoryName) {
        final String canonical = canonical(categoryName);
        final int separator = canonical.lastIndexOf('.');
        return separator < 0 ? canonical : canonical.substring(separator + 1);
    }

    public static String key(final String categoryName) {
        return canonical(categoryName).toLowerCase(Locale.ROOT);
    }
}
