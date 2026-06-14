/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package rocks.gravili.notquests.paper.structs.actions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.time.Duration;
import java.util.ArrayList;

public class ShowTitleAction extends Action {
    private String title = "";
    private long fadeInMillis = 500;
    private long stayMillis = 3000;
    private long fadeOutMillis = 500;

    public ShowTitleAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder builder,
            final ActionFor actionFor) {
        manager.command(builder
                .required("title", NQArguments.greedyStringArgument(), NQDescription.of("Title text shown in the center of the target player's screen. Supports MiniMessage formatting and NotQuests placeholders. Use a vertical bar (`|`) to add a subtitle."))
                .handler(context -> {
                    final ShowTitleAction action = new ShowTitleAction(main);
                    action.setTitle(context.get("title"));
                    main.getActionManager().addAction(action, context, actionFor);
                }));

        manager.command(builder
                .literal("timed", NQDescription.of("Creates a title action with custom fade-in, stay, and fade-out durations."))
                .required("fadeIn", NQArguments.durationArgument(), NQDescription.of("How long the title should fade in. Examples: 250ms, 1s."))
                .required("stay", NQArguments.durationArgument(), NQDescription.of("How long the title should stay fully visible. Examples: 3s, 1500ms."))
                .required("fadeOut", NQArguments.durationArgument(), NQDescription.of("How long the title should fade out. Examples: 500ms, 1s."))
                .required("title", NQArguments.greedyStringArgument(), NQDescription.of("Title text shown in the center of the target player's screen. Supports MiniMessage formatting and NotQuests placeholders. Use a vertical bar (`|`) to add a subtitle."))
                .handler(context -> {
                    final ShowTitleAction action = new ShowTitleAction(main);
                    action.setFadeInMillis(((Duration) context.get("fadeIn")).toMillis());
                    action.setStayMillis(((Duration) context.get("stay")).toMillis());
                    action.setFadeOutMillis(((Duration) context.get("fadeOut")).toMillis());
                    action.setTitle(context.get("title"));
                    main.getActionManager().addAction(action, context, actionFor);
                }));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public long getFadeInMillis() {
        return fadeInMillis;
    }

    public void setFadeInMillis(final long fadeInMillis) {
        this.fadeInMillis = Math.max(0, fadeInMillis);
    }

    public long getStayMillis() {
        return stayMillis;
    }

    public void setStayMillis(final long stayMillis) {
        this.stayMillis = Math.max(0, stayMillis);
    }

    public long getFadeOutMillis() {
        return fadeOutMillis;
    }

    public void setFadeOutMillis(final long fadeOutMillis) {
        this.fadeOutMillis = Math.max(0, fadeOutMillis);
    }

    @Override
    protected void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null || getTitle().isBlank()) {
            return;
        }

        final String resolved = main.getUtilManager()
                .applyPlaceholders(getTitle(), questPlayer.getPlayer(), questPlayer, getObjectiveHolder(), objects);
        final String[] parts = resolved.split("\\|", 2);
        final Component titleComponent = main.parse(parts[0]);
        final Component subtitleComponent = parts.length > 1 ? main.parse(parts[1]) : Component.empty();
        questPlayer.getPlayer().showTitle(Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(
                        Duration.ofMillis(getFadeInMillis()),
                        Duration.ofMillis(getStayMillis()),
                        Duration.ofMillis(getFadeOutMillis()))));
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.title", getTitle());
        configuration.set(initialPath + ".specifics.fadeInMillis", getFadeInMillis());
        configuration.set(initialPath + ".specifics.stayMillis", getStayMillis());
        configuration.set(initialPath + ".specifics.fadeOutMillis", getFadeOutMillis());
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        title = configuration.getString(initialPath + ".specifics.title", "");
        fadeInMillis = configuration.getLong(initialPath + ".specifics.fadeInMillis", 500);
        stayMillis = configuration.getLong(initialPath + ".specifics.stayMillis", 3000);
        fadeOutMillis = configuration.getLong(initialPath + ".specifics.fadeOutMillis", 500);
    }

    @Override
    public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
        title = String.join(" ", arguments);
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Shows title: " + getTitle();
    }
}
