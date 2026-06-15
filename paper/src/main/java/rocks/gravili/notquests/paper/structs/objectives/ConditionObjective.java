package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import static rocks.gravili.notquests.paper.commands.arguments.ConditionArgument.conditionArgument;

public class ConditionObjective extends Objective {
    private Condition condition = null;
    private boolean checkOnlyWhenCorrespondingVariableValueChanged = false;

    public ConditionObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("condition", conditionArgument(main), NQDescription.of("Identifier of the saved condition this objective should watch."))
                .flag(NQFlag.builder("checkOnlyWhenCorrespondingVariableValueChanged", NQDescription.of("This checks this condition only, when the corresponding variable value is changed via an action, instead of checking every x seconds.")).build())
                .handler(
                        (context) -> {
                            final Condition condition = context.get("condition");
                            final boolean checkOnlyWhenCorrespondingVariableValueChanged =
                                    context.flags().isPresent("checkOnlyWhenCorrespondingVariableValueChanged");

                            ConditionObjective conditionObjective = new ConditionObjective(main);
                            conditionObjective.setCondition(condition);
                            conditionObjective.setCheckOnlyWhenCorrespondingVariableValueChanged(
                                    checkOnlyWhenCorrespondingVariableValueChanged);

                            main.getObjectiveManager().addObjective(conditionObjective, context, level);
                        }));
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        if (condition != null) {
            return condition.isHidden(questPlayer) ? "Hidden" : condition.getConditionDescription(questPlayer, getObjectiveHolder());
        } else {
            return "<YELLOW>Error: Condition not found.";
        }
    }

    public final Condition getCondition() {
        return condition;
    }

    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    public final boolean isCheckOnlyWhenCorrespondingVariableValueChanged() {
        return checkOnlyWhenCorrespondingVariableValueChanged;
    }

    public void setCheckOnlyWhenCorrespondingVariableValueChanged(
            final boolean checkOnlyWhenCorrespondingVariableValueChanged) {
        this.checkOnlyWhenCorrespondingVariableValueChanged =
                checkOnlyWhenCorrespondingVariableValueChanged;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if (condition != null) {
            configuration.set(initialPath + ".specifics.condition", getCondition().getConditionName());
        }
        configuration.set(
                initialPath + ".specifics.checkOnlyWhenCorrespondingVariableValueChanged",
                isCheckOnlyWhenCorrespondingVariableValueChanged());
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
        activeObjective.getQuestPlayer().setHasActiveConditionObjectives(true);
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
        activeObjective.getQuestPlayer().setHasActiveConditionObjectives(false);
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        String conditionName = configuration.getString(initialPath + ".specifics.condition", "");
        condition = main.getConditionsYMLManager().getCondition(conditionName);
        if (condition == null) {
            main.getLogManager()
                    .warn(
                            "Error: Cannot load Condition <highlight>"
                                    + conditionName
                                    + "</highlight> of Condition Objective for Quest <highlight2>"
                                    + getObjectiveHolder().getIdentifier()
                                    + "</highlight>, because the condition does not exist.");
        }
        checkOnlyWhenCorrespondingVariableValueChanged =
                configuration.getBoolean(
                        ".specifics.checkOnlyWhenCorrespondingVariableValueChanged", false);
    }
}
