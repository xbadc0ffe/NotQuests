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

import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class OpenBuriedTreasureObjective extends Objective {

  public OpenBuriedTreasureObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      NQCommandManager manager,
      NQCommandBuilder addObjectiveBuilder,
      final int level) {
    manager.command(addObjectiveBuilder
            .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of buried treasured to open"))
            .handler(
                (context) -> {
                  final String amountExpression = context.get("amount");

                  OpenBuriedTreasureObjective openBuriedTreasureObjective =
                      new OpenBuriedTreasureObjective(main);
                  openBuriedTreasureObjective.setProgressNeededExpression(amountExpression);

                  main.getObjectiveManager().addObjective(openBuriedTreasureObjective, context, level);
                }));
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.openBuriedTreasure.base",
            questPlayer,
            activeObjective);
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {}

  @Override
  public void load(FileConfiguration configuration, String initialPath) {}

  @Override
  public void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess) {}

  @Override
  public void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed) {}
}
