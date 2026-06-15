package rocks.gravili.notquests.paper.structs.objectives.hooks.betonquest;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.quest.objective.ObjectiveState;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.Map;

public class BetonQuestObjectiveStateChangeObjective extends Objective {
  private String packageName = "";
  private String objectiveName = "";
  private ObjectiveState objectiveState = ObjectiveState.COMPLETED;

  public BetonQuestObjectiveStateChangeObjective(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      final NotQuests main,
      final NQCommandManager manager,
      final NQCommandBuilder addObjectiveBuilder,
      final int level) {
    manager.command(
        addObjectiveBuilder
            .required(
                "package",
                NQArguments.stringArgument(),
                NQDescription.of("BetonQuest package which contains the objective to watch."),
                (context, input) -> betonQuestManager(main).packageNames())
            .required(
                "objective",
                NQArguments.stringArgument(),
                NQDescription.of("BetonQuest objective name whose state should advance this NotQuests objective."),
                (context, input) -> betonQuestManager(main).objectiveNames(context.get("package")))
            .required(
                "objectiveState",
                NQArguments.stringArgument(),
                NQDescription.of("BetonQuest objective state that should count as progress."),
                (context, input) -> betonQuestManager(main).objectiveStates())
            .handler(
                context -> {
                  final String packageName = context.get("package");
                  final String objectiveName = context.get("objective");
                  final String stateName = context.get("objectiveState");
                  final ObjectiveState state;
                  try {
                    betonQuestManager(main).objectiveIdentifier(packageName, objectiveName);
                    state = ObjectiveState.valueOf(stateName.toUpperCase(java.util.Locale.ROOT));
                  } catch (final IllegalArgumentException exception) {
                    main.sendMessage(
                        context.sender(),
                        "<error>Error: BetonQuest objective state <highlight>"
                            + stateName
                            + "</highlight> does not exist.");
                    return;
                  } catch (final QuestException exception) {
                    main.sendMessage(
                        context.sender(),
                        "<error>Error: BetonQuest objective <highlight>"
                            + packageName
                            + "."
                            + objectiveName
                            + "</highlight> does not exist.");
                    return;
                  }

                  final BetonQuestObjectiveStateChangeObjective objective =
                      new BetonQuestObjectiveStateChangeObjective(main);
                  objective.setPackageName(packageName);
                  objective.setObjectiveName(objectiveName);
                  objective.setObjectiveState(state);
                  main.getObjectiveManager().addObjective(objective, context, level);
                }));
  }

  private static BetonQuestManager betonQuestManager(final NotQuests main) {
    return main.getIntegrationsManager().getBetonQuestManager();
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(final String packageName) {
    this.packageName = packageName == null ? "" : packageName;
  }

  public String getObjectiveName() {
    return objectiveName;
  }

  public void setObjectiveName(final String objectiveName) {
    this.objectiveName = objectiveName == null ? "" : objectiveName;
  }

  public ObjectiveState getObjectiveState() {
    return objectiveState;
  }

  public void setObjectiveState(final ObjectiveState objectiveState) {
    this.objectiveState = objectiveState == null ? ObjectiveState.COMPLETED : objectiveState;
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.BetonQuestCompleteObjective.base",
            questPlayer,
            activeObjective,
            Map.of("%BETONQUESTOBJECTIVENAME%", getObjectiveName()));
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
  public void save(final FileConfiguration configuration, final String initialPath) {
    configuration.set(initialPath + ".specifics.packageName", getPackageName());
    configuration.set(initialPath + ".specifics.objectiveName", getObjectiveName());
    configuration.set(initialPath + ".specifics.objectiveState", getObjectiveState().name());
  }

  @Override
  public void load(final FileConfiguration configuration, final String initialPath) {
    setPackageName(configuration.getString(initialPath + ".specifics.packageName"));
    setObjectiveName(configuration.getString(initialPath + ".specifics.objectiveName"));
    try {
      setObjectiveState(
          ObjectiveState.valueOf(
              configuration.getString(initialPath + ".specifics.objectiveState", "COMPLETED")));
    } catch (final IllegalArgumentException exception) {
      setObjectiveState(ObjectiveState.COMPLETED);
    }
  }

  public String getObjectiveFullID() {
    return getPackageName() + "." + getObjectiveName();
  }
}
