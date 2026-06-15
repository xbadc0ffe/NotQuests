package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

public class SendMessageAction extends Action {

  private String messageToSend = "";

  public SendMessageAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      NQCommandManager manager,
      NQCommandBuilder builder,
      ActionFor actionFor) {
    manager.command(
        builder.required("Sending Message", NQArguments.greedyStringArgument(), NQDescription.of("Message that should be sent to the target player."))
            .handler((context) -> {
                  final String messageToSend = (String) context.get("Sending Message");
                  SendMessageAction sendMessageAction = new SendMessageAction(main);
                  sendMessageAction.setMessageToSend(messageToSend);
                  main.getActionManager().addAction(sendMessageAction, context, actionFor);
                }));
  }

  public final String getMessageToSend() {
    return messageToSend;
  }

  public void setMessageToSend(final String messageToSend) {
    this.messageToSend = messageToSend;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (getMessageToSend().isBlank()) {
      main.getLogManager().warn("Tried to execute SendMessage action with empty message.");
      return;
    }

    questPlayer
        .getPlayer()
        .sendMessage(
            main.parse(
                main.getUtilManager()
                    .applyPlaceholders(
                        getMessageToSend(), questPlayer.getPlayer(), getObjectiveHolder(), objects)));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.message", getMessageToSend());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.messageToSend = configuration.getString(initialPath + ".specifics.message", "");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.messageToSend = String.join(" ", arguments);
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayerr, final Object... objects) {
    return "Sends Message: " + getMessageToSend();
  }
}
