package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerMaxHealthVariable extends Variable<Double> {

  public PlayerMaxHealthVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Double getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      final AttributeInstance attributeInstance =
          questPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH);
      return attributeInstance != null ? attributeInstance.getValue() : 0;
    } else {
      return 0d;
    }
  }

  @Override
  public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      final AttributeInstance attributeInstance =
          questPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH);
      if (attributeInstance != null) {
        attributeInstance.setBaseValue(newValue);
        return true;
      } else {
        return false;
      }
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
    return "Maximum Health";
  }

  @Override
  public String getSingular() {
    return "Maximum Health";
  }
}
