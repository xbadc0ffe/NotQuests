package rocks.gravili.notquests.paper.structs.variables;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerSleepingVariable extends Variable<Boolean> {
  public PlayerSleepingVariable(NotQuests main) {
    super(main);
    setCanSetValue(false);
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return questPlayer.getPlayer().isSleeping();
    } else {
      return false;
    }
  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Sleeping";
  }

  @Override
  public String getSingular() {
    return "Sleeping";
  }
}
