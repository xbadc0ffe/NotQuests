package rocks.gravili.notquests.paper.structs.variables;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class QuestPointsVariable extends Variable<Long> {
  public QuestPointsVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
    addRequiredBooleanFlag(
        NQFlag.presence(
            "notifyPlayer",
            NQDescription.of(
                "Notifies the player for when their QuestPoints are changed or set")) // TODO: setOnlyRequiredValues once implemented
        );
  }

  @Override
  public Long getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer == null) {
      return 0L;
    }
    return questPlayer.getQuestPoints();
  }

  @Override
  public boolean setValueInternally(Long newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer == null) {
      return false;
    }
    questPlayer.setQuestPoints(newValue, getRequiredBooleanValue("notifyPlayer", questPlayer));
    return true;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Quest Points";
  }

  @Override
  public String getSingular() {
    return "Quest Point";
  }
}
