package rocks.gravili.notquests.paper.structs.conditions;

import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

import static rocks.gravili.notquests.paper.commands.arguments.ConditionArgument.conditionArgument;

public class ConditionCondition extends Condition {

  private Condition condition = null;

  public ConditionCondition(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      NQCommandManager manager,
      NQCommandBuilder builder,
      ConditionFor conditionFor) {
    manager.command(builder.required("Condition", conditionArgument(main), NQDescription.of("Name of the condition which will be checked"))
            .handler(
                (context) -> {
                  final Condition condition = context.get("Condition");
                  ConditionCondition conditionCondition = new ConditionCondition(main);
                  conditionCondition.setCondition(condition);
                  main.getConditionsManager().addCondition(conditionCondition, context, conditionFor);
                }));
  }

  public final Condition getCondition() {
    return condition;
  }

  public void setCondition(final Condition condition) {
    this.condition = condition;
  }

  @Override
  public String checkInternally(final QuestPlayer questPlayer) {
    if (condition == null) {
      return "<warn>Error: ConditionCondition cannot be checked because the condition was not found. Report this to the server owner.";
    }

    return condition.check(questPlayer).message();
  }

  @Override
  public String getConditionDescriptionInternally(QuestPlayer questPlayer, Object... objects) {
    if (condition != null) {
      return "<unimportant>-- Complete Condition: <highlight>" + condition.getConditionName();
    } else {
      return "<unimportant>-- Complete Condition: Condition not found.";
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    if (getCondition() != null) {
      configuration.set(initialPath + ".specifics.condition", getCondition().getConditionName());
    } else {
      main.getLogManager()
          .warn(
              "Error: cannot save Condition for condition condition, because it's null. Configuration path: "
                  + initialPath);
    }
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    String conditionName = configuration.getString(initialPath + ".specifics.condition");
    this.condition = main.getConditionsYMLManager().getCondition(conditionName);
    if (condition == null) {
      main.getLogManager()
          .warn(
              "Error: ConditionCondition cannot find the condition with name "
                  + conditionName
                  + ". Condition Path: "
                  + initialPath);
    }
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    String conditionName = arguments.get(0);
    this.condition = main.getConditionsYMLManager().getCondition(conditionName);
    if (condition == null) {
      main.getLogManager()
          .warn(
              "Error: ConditionCondition cannot find the condition with name "
                  + conditionName
                  + ". Provided condition: "
                  + arguments.get(0));
    }
  }
}
