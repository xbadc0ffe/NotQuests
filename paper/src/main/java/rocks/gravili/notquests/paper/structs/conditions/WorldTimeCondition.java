package rocks.gravili.notquests.paper.structs.conditions;

import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

public class WorldTimeCondition extends Condition {

  private int minTime, maxTime;

  public WorldTimeCondition(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      NQCommandManager manager,
      NQCommandBuilder builder,
      ConditionFor conditionFor) {
    manager.command(builder
            .required("minTime", NQArguments.integerArgument(), NQDescription.of("Minimum world time (24-hour clock)"))
            .required("maxTime", NQArguments.integerArgument(), NQDescription.of("Maximum world time (24-hour clock)"))
            .handler(
                (context) -> {
                  final int minTime = context.get("minTime");
                  final int maxTime = context.get("maxTime");

                  WorldTimeCondition worldTimeCondition = new WorldTimeCondition(main);
                  worldTimeCondition.setMinTime(minTime);
                  worldTimeCondition.setMaxTime(maxTime);

                  main.getConditionsManager().addCondition(worldTimeCondition, context, conditionFor);
                }));
  }

  public final int getMinTime() {
    return minTime;
  }

  public void setMinTime(final int minTime) {
    this.minTime = minTime;
  }

  public final int getMaxTime() {
    return maxTime;
  }

  public void setMaxTime(final int maxTime) {
    this.maxTime = maxTime;
  }

  @Override
  public String checkInternally(final QuestPlayer questPlayer) {
    long currentTime = questPlayer.getPlayer().getWorld().getTime();

    if (currentTime >= 18000) {

      currentTime = currentTime / 1000 - 18;
    } else {

      currentTime = currentTime / 1000 + 6;
    }

    if (getMaxTime() >= getMinTime()) {
      if (currentTime <= getMaxTime() && currentTime >= getMinTime()) {
        return "";
      } else {
        return "<YELLOW>Come back between <highlight>"
            + getMinTime()
            + "</highlight> and <highlight>"
            + getMaxTime()
            + "</highlight> (It's now "
            + currentTime
            + ")";
      }
    } else { // Maxtime is the next day
      if (currentTime <= getMinTime()) { // Chec for next day
        if (currentTime <= getMaxTime()) {
          return "";
        } else {
          return "<YELLOW>Come back between <highlight>"
              + getMinTime()
              + "</highlight> and <highlight>"
              + getMaxTime()
              + "</highlight> (It's now "
              + currentTime
              + ")";
        }
      } else { // Check for current day
        if (currentTime >= getMinTime() && currentTime <= 24) {
          return "";
        } else {
          return "<YELLOW>Come back between <highlight>"
              + getMinTime()
              + "</highlight> and <highlight>"
              + getMaxTime()
              + "</highlight> (It's now "
              + currentTime
              + ")";
        }
      }
    }
  }

  @Override
  public String getConditionDescriptionInternally(QuestPlayer questPlayer, Object... objects) {
    return "<GRAY>-- World time: " + getMinTime() + " - " + getMaxTime();
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.minTime", getMinTime());
    configuration.set(initialPath + ".specifics.maxTime", getMaxTime());
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    minTime = configuration.getInt(initialPath + ".specifics.minTime");
    maxTime = configuration.getInt(initialPath + ".specifics.maxTime");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    minTime = Integer.parseInt(arguments.get(0));
    maxTime = Integer.parseInt(arguments.get(1));
  }
}
