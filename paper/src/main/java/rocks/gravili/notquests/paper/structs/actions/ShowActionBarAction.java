package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

public class ShowActionBarAction extends Action {
    private String message = "";

    public ShowActionBarAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder builder,
            final ActionFor actionFor) {
        manager.command(builder
                .required("message", NQArguments.greedyStringArgument(), NQDescription.of("Action-bar message shown above the target player's hotbar. Supports MiniMessage formatting and NotQuests placeholders."))
                .handler(context -> {
                    final ShowActionBarAction action = new ShowActionBarAction(main);
                    action.setMessage(context.get("message"));
                    main.getActionManager().addAction(action, context, actionFor);
                }));
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    @Override
    protected void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null || getMessage().isBlank()) {
            return;
        }
        questPlayer.getPlayer().sendActionBar(main.parse(main.getUtilManager()
                .applyPlaceholders(getMessage(), questPlayer.getPlayer(), questPlayer, getObjectiveHolder(), objects)));
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.message", getMessage());
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        message = configuration.getString(initialPath + ".specifics.message", "");
    }

    @Override
    public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
        message = String.join(" ", arguments);
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Shows action bar: " + getMessage();
    }
}
