package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Locale;

import static rocks.gravili.notquests.paper.commands.arguments.QuestArgument.questArgument;

public class GiveQuestAction extends Action {

  private String questToGiveName = "";
  private boolean forceGive = false;

  public GiveQuestAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      NQCommandManager manager,
      NQCommandBuilder builder,
      ActionFor actionFor) {
    manager.command(
        builder
            .required("quest to give", questArgument(main), NQDescription.of("Name of the Quest which should be given to the player."))
            .flag(NQFlag.presence("forceGive",
                        NQDescription.of("Force-gives the Quest to the player, disregarding most Quest requirements/cooldowns/...")))
            .handler(
                (context) -> {
                  final Quest foundQuest = context.get("quest to give");
                  final boolean forceGive = context.flags().isPresent("forceGive");

                  GiveQuestAction giveQuestAction = new GiveQuestAction(main);
                  giveQuestAction.setQuestToGiveName(foundQuest.getIdentifier() );
                  giveQuestAction.setForceGive(forceGive);

                  main.getActionManager().addAction(giveQuestAction, context, actionFor);
                }));
  }

  public final String getQuestToGiveName() {
    return questToGiveName;
  }

  public void setQuestToGiveName(final String questName) {
    this.questToGiveName = questName;
  }

  public final boolean isForceGive() {
    return forceGive;
  }

  public void setForceGive(final boolean forceGive) {
    this.forceGive = forceGive;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    Quest foundQuest = main.getQuestManager().getQuest(getQuestToGiveName());
    if (foundQuest == null) {
      main.getLogManager()
          .warn(
              "Tried to execute GiveQuest action with null quest. Cannot find the following Quest: "
                  + getQuestToGiveName());
      return;
    }
    if (!isForceGive()) {
      // acceptQuest sends the formatted success message/title itself and returns the "accepted"
      // sentinel on success, or a feedback message (e.g. "requirements not fulfilled") on failure.
      // Forward only the failure message so accepting via a GUI / NPC / armor stand gives the same
      // feedback as /q take, instead of silently closing the GUI with no indication.
      final String result = main.getQuestPlayerManager().acceptQuest(questPlayer, foundQuest, true, true);
      final var player = questPlayer.getPlayer();
      if (player != null && result != null && !result.equals("accepted")) {
        main.sendMessage(player, result);
      }
    } else {
      main.getQuestPlayerManager().forceAcceptQuestSilent(questPlayer.getUniqueId(), foundQuest);
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.quest", getQuestToGiveName());
    configuration.set(initialPath + ".specifics.forceGive", isForceGive());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.questToGiveName = configuration.getString(initialPath + ".specifics.quest");
    this.forceGive = configuration.getBoolean(initialPath + ".specifics.forceGive");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.questToGiveName = arguments.get(0);

    this.forceGive = String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--forcegive");
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Gives Quest: " + getQuestToGiveName();
  }
}
