package com.notquests.paper.managers.tags;

import org.jetbrains.annotations.NotNull;
import com.notquests.paper.NotQuests;
import com.notquests.paper.managers.data.Category;

import java.util.Locale;

public class Tag {
  private final NotQuests main;
  private final TagType tagType;
  private final String tagName;
  private Category category;

  public Tag(
      @NotNull final NotQuests main,
      @NotNull final String tagName,
      @NotNull final TagType tagType) {
    this.main = main;
    this.tagName = tagName.toLowerCase(Locale.ROOT);
    this.tagType = tagType;
    category = main.getDataManager().getDefaultCategory();
  }

  public final TagType getTagType() {
    return tagType;
  }

  public final String getTagName() {
    return tagName;
  }

  public final Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
  }
}
