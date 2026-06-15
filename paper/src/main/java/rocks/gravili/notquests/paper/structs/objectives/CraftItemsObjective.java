package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class CraftItemsObjective extends Objective {

  private ItemStackSelection itemStackSelection;

  public CraftItemsObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      NQCommandManager manager,
      NQCommandBuilder addObjectiveBuilder,
      final int level) {
    manager.command(
        addObjectiveBuilder
            .required("materials", itemStackSelectionArgument(main), NQDescription.of("Material of the item which needs to be crafted"))
            .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of items which need to be crafted"))
            .handler(
                (context) -> {
                  final String amountExpression = context.get("amount");

                  final ItemStackSelection itemStackSelection = context.get("materials");

                  CraftItemsObjective craftItemsObjective = new CraftItemsObjective(main);
                  craftItemsObjective.setItemStackSelection(itemStackSelection);

                  craftItemsObjective.setProgressNeededExpression(amountExpression);

                  main.getObjectiveManager().addObjective(craftItemsObjective, context, level);
                }));
  }

  public final ItemStackSelection getItemStackSelection() {
    return itemStackSelection;
  }

  public void setItemStackSelection(final ItemStackSelection itemStackSelection) {
    this.itemStackSelection = itemStackSelection;
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
            "chat.objectives.taskDescription.craftItems.base",
            questPlayer,
            activeObjective,
            Map.of(
                "%ITEMTOCRAFTTYPE%", getItemStackSelection().getAllMaterialsListedTranslated("main"),
                "%ITEMTOCRAFTNAME%", "",
                "%(%", "",
                "%)%", ""));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    getItemStackSelection()
        .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    this.itemStackSelection = new ItemStackSelection(main);
    itemStackSelection.loadFromFileConfiguration(
        configuration, initialPath + ".specifics.itemStackSelection");
  }
}
