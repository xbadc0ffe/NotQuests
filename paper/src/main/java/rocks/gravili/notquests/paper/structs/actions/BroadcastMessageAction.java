package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

public class BroadcastMessageAction extends Action {

    private String messageToBroadcast = "";

    public BroadcastMessageAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder builder,
            ActionFor actionFor) {
        manager.command(
                builder
                        .required("Broadcast Message", NQArguments.greedyStringArgument(), NQDescription.of("Message that should be broadcast to the server."))
                        .handler((context) -> {
                            final String messageToBroadcast = (String) context.get("Broadcast Message");
                            BroadcastMessageAction broadcastMessageAction = new BroadcastMessageAction(main);
                            broadcastMessageAction.setMessageToBroadcast(messageToBroadcast);
                            main.getActionManager().addAction(broadcastMessageAction, context, actionFor);
                        }));
    }

    public final String getMessageToBroadcast() {
        return messageToBroadcast;
    }

    public void setMessageToBroadcast(final String messageToBroadcast) {
        this.messageToBroadcast = messageToBroadcast;
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        if (getMessageToBroadcast().isBlank()) {
            main.getLogManager().warn("Tried to execute SendMessage action with empty message.");
            return;
        }

        Bukkit.broadcast(
                main.parse(
                        main.getUtilManager()
                                .applyPlaceholders(
                                        getMessageToBroadcast(),
                                        questPlayer.getPlayer(),
                                        questPlayer,
                                        getObjectiveHolder(),
                                        objects)));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.message", getMessageToBroadcast());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.messageToBroadcast = configuration.getString(initialPath + ".specifics.message", "");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.messageToBroadcast = String.join(" ", arguments);
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Broadcasts Message: " + getMessageToBroadcast();
    }
}
