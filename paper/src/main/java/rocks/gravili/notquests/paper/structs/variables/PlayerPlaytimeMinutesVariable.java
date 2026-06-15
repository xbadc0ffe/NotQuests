package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Statistic;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerPlaytimeMinutesVariable extends Variable<Double> {
  public PlayerPlaytimeMinutesVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Double getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return questPlayer.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) / 1200d;
    } else {
      return null;
    }
  }

  @Override
  public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      questPlayer.getPlayer().setStatistic(Statistic.PLAY_ONE_MINUTE, newValue.intValue() * 1200);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Playtime in minutes";
  }

  @Override
  public String getSingular() {
    return "Playtime in minute";
  }
}
