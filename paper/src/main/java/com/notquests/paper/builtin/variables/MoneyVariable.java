package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class MoneyVariable extends Variable<Double> {
  public MoneyVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Double getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      if (!main.getIntegrationsManager().isVaultEnabled()
          || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
        return 0D;
      } else {
        return main.getIntegrationsManager()
            .getVaultManager()
            .getEconomy()
            .getBalance(questPlayer.getPlayer(), questPlayer.getPlayer().getWorld().getName());
      }
    } else {
      return 0D;
    }
  }

  @Override
  public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      if (!main.getIntegrationsManager().isVaultEnabled()
          || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
        return false;
      } else {
        final double currentBalance =
            main.getIntegrationsManager()
                .getVaultManager()
                .getEconomy()
                .getBalance(questPlayer.getPlayer());
        if (newValue > currentBalance) {
          // player.sendMessage("Deposited " + (newValue-currentBalance));
          main.getIntegrationsManager()
              .getVaultManager()
              .getEconomy()
              .depositPlayer(questPlayer.getPlayer(), newValue - currentBalance);
        } else {
          // player.sendMessage("Withdraw " + (currentBalance - newValue));

          main.getIntegrationsManager()
              .getVaultManager()
              .getEconomy()
              .withdrawPlayer(questPlayer.getPlayer(), currentBalance - newValue);
        }
        return true;
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
    return "Money";
  }

  @Override
  public String getSingular() {
    return "Money";
  }
}
