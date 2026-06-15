package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class JumpObjective extends Objective {

  public JumpObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      NQCommandManager manager,
      NQCommandBuilder addObjectiveBuilder,
      final int level) {
    manager.command(addObjectiveBuilder
            .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of times the player needs to jump."))
            .handler(
                (context) -> {
                  final String amountExpression = context.get("amount");

                  JumpObjective jumpObjective = new JumpObjective(main);
                  jumpObjective.setProgressNeededExpression(amountExpression);

                  main.getObjectiveManager().addObjective(jumpObjective, context, level);
                }));
  }

  @Override
  public void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess) {}

  @Override
  public void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed) {}

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.jump.base",
            questPlayer,
            activeObjective,
            Map.of(
                "%AMOUNTOFJUMPS%",
                ""
                    + (activeObjective != null
                        ? activeObjective.getProgressNeeded()
                        : getProgressNeededExpression().getRawExpression())));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {}

  @Override
  public void load(FileConfiguration configuration, String initialPath) {}
}
