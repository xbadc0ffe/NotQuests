package com.notquests.paper.minimessage;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import com.notquests.paper.NotQuests;

public class MessageManager {
  private final NotQuests main;
  private final MiniMessage miniMessage;
  private final TagResolver tagResolver;

  public MessageManager(final NotQuests main) {
    this.main = main;

    final TagResolver mainGradient = TagResolver.resolver("main", SimpleGradientTag::main);
    final TagResolver highlight = TagResolver.resolver("highlight", SimpleGradientTag::highlight);
    final TagResolver highlight2 =
        TagResolver.resolver("highlight2", SimpleGradientTag::highlight2);
    final TagResolver error = TagResolver.resolver("error", SimpleGradientTag::error);
    final TagResolver success = TagResolver.resolver("success", SimpleGradientTag::success);
    final TagResolver unimportant =
        TagResolver.resolver("unimportant", SimpleGradientTag::unimportant);
    final TagResolver warn = TagResolver.resolver("warn", SimpleGradientTag::warn);
    final TagResolver veryUnimportant =
        TagResolver.resolver("veryunimportant", SimpleGradientTag::veryUnimportant);
    final TagResolver negative = TagResolver.resolver("negative", SimpleGradientTag::negative);
    final TagResolver positive = TagResolver.resolver("positive", SimpleGradientTag::positive);

    final TagResolver tagResolver =
        TagResolver.builder()
            .resolvers(
                TagResolver.standard(),
                mainGradient,
                highlight,
                highlight2,
                error,
                success,
                unimportant,
                warn,
                veryUnimportant,
                negative,
                positive)
            .build();

    this.tagResolver = tagResolver;
    miniMessage = MiniMessage.builder().tags(tagResolver).build();
  }

  public final MiniMessage getMiniMessage() {
    return miniMessage;
  }

  public final TagResolver getTagResolver() {
    return tagResolver;
  }
}
