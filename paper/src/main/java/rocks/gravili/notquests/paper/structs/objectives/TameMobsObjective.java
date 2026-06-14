package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import static rocks.gravili.notquests.paper.commands.arguments.EntityTypeArgument.entityTypeArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class TameMobsObjective extends Objective {
    private String entityToTameType = "";

    public TameMobsObjective(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("entityType", entityTypeArgument(main, false), NQDescription.of("Type of mob the player has to tame. Use 'any' if any tameable mob should count."))
                .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of mobs the player needs to tame."))
                .handler(context -> {
                    final TameMobsObjective objective = new TameMobsObjective(main);
                    objective.setEntityToTameType(context.get("entityType"));
                    objective.setProgressNeededExpression(context.get("amount"));
                    main.getObjectiveManager().addObjective(objective, context, level);
                }));
    }

    public String getEntityToTameType() {
        return entityToTameType;
    }

    public void setEntityToTameType(final String entityToTameType) {
        this.entityToTameType = entityToTameType;
    }

    public boolean countsEntityType(final String entityType) {
        return entityToTameType.equalsIgnoreCase("any") || entityToTameType.equalsIgnoreCase(entityType);
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer,
            final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString("chat.objectives.taskDescription.tameMobs.base", questPlayer, activeObjective)
                .replace("%ENTITYTOTAME%", getEntityToTameType());
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.mobToTame", getEntityToTameType());
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        entityToTameType = configuration.getString(initialPath + ".specifics.mobToTame", "");
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }
}
