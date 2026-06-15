package rocks.gravili.notquests.paper.managers;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.*;
import rocks.gravili.notquests.paper.commands.category.item.AdminItemsCommand;
import rocks.gravili.notquests.paper.commands.category.tag.AdminTagCommands;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQCommands;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import static rocks.gravili.notquests.paper.commands.arguments.ActionArgument.actionArgument;
import static rocks.gravili.notquests.paper.commands.arguments.CategoryArgument.categoryArgument;
import static rocks.gravili.notquests.paper.commands.arguments.ObjectiveArgument.objectiveArgument;
import static rocks.gravili.notquests.paper.commands.arguments.QuestArgument.questArgument;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class CommandManager {
    private final NotQuests main;
    // Re-usable value flags
    public NQFlag nametag_containsany;
    public NQFlag nametag_equals;
    public NQFlag taskDescription;
    public NQFlag maxDistance;
    public NQFlag categoryFlag;
    public NQFlag delayFlag;

    public NQFlag speakerColor;
    public NQFlag applyOn; // 0 = Quest
    public NQFlag world;
    public NQFlag locationX;
    public NQFlag locationY;
    public NQFlag locationZ;

    public NQFlag triggerWorldString;
    public NQFlag minimumTimeAfterCompletion;
    // NotQuests' own native-Brigadier command framework (migration target off Cloud).
    private NQCommands nqCommands;
    private NQCommandManager nqCommandManager;

    // SOLE remaining Cloud shim. The command tree itself no longer uses Cloud, but six not-yet-migrated
    // variable classes (EnderChestVariable, InventoryVariable, ContainerInventoryVariable,
    // QuestPointsVariable, PlaceholderAPINumberVariable and the Boolean/Number*VariableArgument
    // suggestion bridges) still build Cloud CommandFlags / construct a Cloud CommandContext via this
    // manager. It is kept only so those files compile until they are migrated. Fully-qualified on
    // purpose so the only Cloud reference in this file is this single, clearly-marked accessor.

    // Builders
    private NQCommandBuilder adminCommandBuilder;
    private NQCommandBuilder adminEditCommandBuilder;
    private NQCommandBuilder adminTagCommandBuilder;
    private NQCommandBuilder adminItemsCommandBuilder;
    private NQCommandBuilder adminConversationCommandBuilder;
    private NQCommandBuilder adminEditAddObjectiveCommandBuilder;
    private NQCommandBuilder adminEditAddRequirementCommandBuilder;
    private NQCommandBuilder adminEditAddRewardCommandBuilder;
    private NQCommandBuilder adminEditAddTriggerCommandBuilder;
    private NQCommandBuilder adminEditObjectiveAddUnlockConditionCommandBuilder;
    private NQCommandBuilder adminEditObjectiveAddProgressConditionCommandBuilder;
    private NQCommandBuilder adminEditObjectiveAddCompleteConditionCommandBuilder;

    private NQCommandBuilder adminEditObjectiveAddRewardCommandBuilder;
    private NQCommandBuilder adminAddActionCommandBuilder;
    private NQCommandBuilder adminExecuteActionCommandBuilder;

    private NQCommandBuilder adminActionsCommandBuilder;
    private NQCommandBuilder adminActionsEditCommandBuilder;
    private NQCommandBuilder adminActionsAddConditionCommandBuilder;
    private NQCommandBuilder adminAddConditionCommandBuilder;
    private NQCommandBuilder adminConditionCheckCommandBuilder;

    private AdminCommands adminCommands;
    private AdminEditCommands adminEditCommands;
    private AdminTagCommands adminTagCommands;
    private AdminItemsCommand adminItemsCommands;
    private AdminConversationCommands adminConversationCommands;
    // User
    private NQCommandBuilder userCommandBuilder;
    private UserCommands userCommands;
    // Admin
    private NQCommandBuilder adminEditObjectivesBuilder;

    private CommandMap commandMap;

    public CommandManager(final NotQuests main) {
        this.main = main;

        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception ignored) {
            commandMap = null;
        }

        createCommandFlags();
    }

    public void createCommandFlags() {
        nametag_containsany = NQFlag.builder(
                        "nametag_containsany",
                        NQDescription.of("Only count entities whose nametag contains every provided word."))
                .withArgument(NQArguments.stringArrayArgument())
                .build();

        nametag_equals = NQFlag.builder(
                        "nametag_equals",
                        NQDescription.of("Only count entities whose nametag exactly matches the provided text."))
                .withArgument(NQArguments.stringArrayArgument())
                .build();

        taskDescription = NQFlag.builder(
                        "taskDescription",
                        NQDescription.of("Custom task text shown to players for this objective instead of the default description."))
                .withArgument(NQArguments.componentArgument(main))
                .build();

        speakerColor = NQFlag.builder(
                        "speakerColor",
                        NQDescription.of("MiniMessage color or tag used for conversation speaker names."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final ArrayList<String> completions = new ArrayList<>();
                    for (final NamedTextColor namedTextColor : NamedTextColor.NAMES.values()) {
                        completions.add("<" + namedTextColor + ">");
                    }
                    return completions;
                })
                .build();

        maxDistance = NQFlag.builder("maxDistance", NQDescription.of("Maximum distance allowed from the target location or NPC."))
                .withArgument(NQArguments.integerArgument())
                .build();

        world = NQFlag.builder("world", NQDescription.of("World where this sound or location-based effect should be played."))
                .withArgument(NQArguments.worldArgument())
                .build();

        applyOn = NQFlag.builder(
                        "applyOn",
                        NQDescription.of("Quest or objective target this trigger, action, or condition should apply to, such as Quest, O1, or O2."))
                .withArgument(NQArguments.integerArgument())
                .withSuggestions((context, input) -> java.util.List.of("0", "1", "2"))
                .build();

        triggerWorldString = NQFlag.builder(
                        "world_name",
                        NQDescription.of("World name filter for world enter/leave triggers, or ALL for every world."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final ArrayList<String> completions = new ArrayList<>();
                    completions.add("ALL");
                    for (final World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                    return completions;
                })
                .build();

        minimumTimeAfterCompletion = NQFlag.builder(
                        "waitTimeAfterCompletion",
                        NQDescription.of("Minimum time that must pass after completing the quest before this condition can pass."))
                .withArgument(NQArguments.longArgument())
                .build();

        categoryFlag = NQFlag.builder("category", NQDescription.of("Quest category used to store or look up the created NotQuests object."))
                .withArgument(categoryArgument(main))
                .build();

        delayFlag = NQFlag.builder("delay", NQDescription.of("How long to wait before running this action, such as 1s, 500ms, or 2m."))
                .withArgument(NQArguments.durationArgument())
                .build();

        locationX = NQFlag.builder("locationX", NQDescription.of("X coordinate used by this command."))
                .withArgument(NQArguments.doubleArgument())
                .build();

        locationY = NQFlag.builder("locationY", NQDescription.of("Y coordinate used by this command."))
                .withArgument(NQArguments.doubleArgument())
                .build();

        locationZ = NQFlag.builder("locationZ", NQDescription.of("Z coordinate used by this command."))
                .withArgument(NQArguments.doubleArgument())
                .build();
    }

    public final CommandMap getCommandMap() {
        return commandMap;
    }

    public void preSetupCommands() {
        // NotQuests' own native-Brigadier command framework (commands.framework package).
        try {
            nqCommands = new NQCommands(main);
            nqCommands.hook();
            nqCommandManager = new NQCommandManager(main, nqCommands);
        } catch (final Throwable t) {
            main.getLogManager().warn("Could not initialize the native command framework: " + t.getMessage());
        }

        preSetupGeneralCommands();
        preSetupUserCommands();
        preSetupAdminCommands();
    }

    public void preSetupGeneralCommands() {
    }

    public void preSetupUserCommands() {
        userCommandBuilder = nqCommandManager.commandBuilder(
                        "nq",
                        NQDescription.of("Player commands for NotQuests"),
                        "notquests",
                        "nquests",
                        "nquest",
                        "notquest",
                        "quest",
                        "quests",
                        "q",
                        "qg")
                .permission("notquests.use");
    }

    public void preSetupAdminCommands() {
        adminCommandBuilder = nqCommandManager.commandBuilder(
                        "nqa",
                        NQDescription.of("Admin commands for NotQuests"),
                        "nquestsadmin",
                        "nquestadmin",
                        "notquestadmin",
                        "qadmin",
                        "questadmin",
                        "qa",
                        "qag",
                        "notquestsadmin")
                .permission("notquests.admin");

        adminEditCommandBuilder = adminCommandBuilder.literal("edit", NQDescription.of("Opens subcommands for editing a specific quest."), "e").required("quest", questArgument(main),
                NQDescription.of("Identifier of the quest to edit; use /qa list to see available quests."));
        adminTagCommandBuilder = adminCommandBuilder.literal("tags", NQDescription.of("Manages player tags."), "t");
        adminItemsCommandBuilder = adminCommandBuilder.literal("items", NQDescription.of("Manages custom NotQuests items."), "item", "i");
        adminConversationCommandBuilder = adminCommandBuilder.literal("conversations", NQDescription.of("Manages conversations and their NPC attachments."), "c");
        adminEditAddObjectiveCommandBuilder = adminEditCommandBuilder.literal("objectives", NQDescription.of("Manages objectives on the selected quest."), "o").literal("add", NQDescription.of("Adds a new objective to the selected quest."));
        adminEditAddRequirementCommandBuilder = adminEditCommandBuilder.literal("requirements", NQDescription.of("Manages requirements that must pass before the selected quest can be taken."), "req").literal("add", NQDescription.of("Adds a requirement that must pass before players can take the selected quest."));
        adminEditAddRewardCommandBuilder = adminEditCommandBuilder.literal("rewards", NQDescription.of("Manages rewards granted by the selected quest."), "rew").literal("add", NQDescription.of("Adds a reward granted by the selected quest."));
        adminEditAddTriggerCommandBuilder = adminEditCommandBuilder.literal("triggers", NQDescription.of("Manages triggers attached to this quest."), "t")
                .literal("add", NQDescription.of("Adds a trigger that runs an action when the selected quest changes state.")).required("action", actionArgument(main), NQDescription.of("Action which will be executed when the Trigger triggers."));

        adminEditObjectivesBuilder = adminEditCommandBuilder.literal("objectives", NQDescription.of("Manages objectives on the selected quest.")).literal("edit", NQDescription.of("Opens subcommands for editing a specific objective on the selected quest.")).required("objectiveId",
                objectiveArgument(main, 0), NQDescription.of("Objective ID shown by this quest's objectives list."));
        adminEditObjectiveAddUnlockConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions", NQDescription.of("Manages conditions attached to the selected objective.")).literal("unlock", NQDescription.of("Configures conditions required before the objective can unlock.")).literal("add", NQDescription.of("Adds an unlock condition to the selected objective."));
        adminEditObjectiveAddProgressConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions", NQDescription.of("Manages conditions attached to the selected objective.")).literal("progress", NQDescription.of("Configures conditions required while the objective is progressing.")).literal("add", NQDescription.of("Adds a progress condition to the selected objective."));
        adminEditObjectiveAddCompleteConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions", NQDescription.of("Manages conditions attached to the selected objective.")).literal("complete", NQDescription.of("Configures conditions required before the objective can complete.")).literal("add", NQDescription.of("Adds a completion condition to the selected objective."));
        adminActionsCommandBuilder = adminCommandBuilder.literal("actions", NQDescription.of("Manages saved actions, inline actions, and action execution."));
        adminActionsEditCommandBuilder = adminActionsCommandBuilder.literal("edit", NQDescription.of("Opens subcommands for editing a saved action.")).required("action", actionArgument(main),
                NQDescription.of("Identifier of the saved action to edit; use /qa actions to list saved actions."));

        adminActionsAddConditionCommandBuilder =
                adminActionsEditCommandBuilder.literal("conditions", NQDescription.of("Manages conditions required before the selected action can run.")).literal("add", NQDescription.of("Adds a condition to the selected saved action."));

        adminEditObjectiveAddRewardCommandBuilder =
                adminEditObjectivesBuilder.literal("rewards", NQDescription.of("Manages rewards granted by the selected objective."), "rew").literal("add", NQDescription.of("Adds a reward granted when the selected objective completes."));

        adminAddConditionCommandBuilder = adminCommandBuilder
                .literal("conditions", NQDescription.of("Manages saved conditions that can be reused by quests, objectives, and actions."))
                .literal("add", NQDescription.of("Creates a new saved condition."))
                .required("Condition Identifier", NQArguments.stringArgument(),
                        NQDescription.of("Unique identifier for the new saved condition."),
                        (context, input) -> java.util.List.of("[Enter new, unique Condition Identifier]"));


        adminConditionCheckCommandBuilder = adminCommandBuilder
                .literal("conditions", NQDescription.of("Manages saved conditions that can be reused by quests, objectives, and actions."))
                .literal("check", NQDescription.of("Checks a condition inline without saving it."));

        adminAddActionCommandBuilder = adminCommandBuilder
                .literal("actions", NQDescription.of("Manages saved actions, inline actions, and action execution."))
                .literal("add", NQDescription.of("Creates a new saved action."))
                .required("Action Identifier", NQArguments.stringArgument(),
                        NQDescription.of("Unique identifier for the new saved action."),
                        (context, input) -> java.util.List.of("[Enter new, unique Action Identifier]"));

        adminExecuteActionCommandBuilder = adminCommandBuilder
                .literal("actions", NQDescription.of("Manages saved actions, inline actions, and action execution."))
                .literal("execute", NQDescription.of("Executes the selected action or command."));
    }

    public void setupCommands() {

    /* PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadminold");
    if (notQuestsAdminCommand != null) {
        final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
        notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
        notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);


        registerCommodoreCompletions(commodore, notQuestsAdminCommand);
    }*/
        // Register the notquests command & tab completer. This command will be used by Players
    /*final PluginCommand notQuestsCommand = main.getCommand("notquests");
    if (notQuestsCommand != null) {
        final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
        notQuestsCommand.setExecutor(commandNotQuests);
        notQuestsCommand.setTabCompleter(commandNotQuests);


    }*/

        constructCommands();
    }

    public void constructCommands() {

        // User Stuff
        // Help menu
        nqCommandManager.command(
                userCommandBuilder
                        .literal("help", NQDescription.of("Shows command help."))
                        .required("query", NQArguments.greedyStringArgument(),
                                NQDescription.of("Command name, topic, or search text to show help for."))
                        .handler(context -> {
                            nqCommandManager.sendRootHelp(
                                    context.sender(), "nq", "<main>NotQuests <unimportant>— available commands:");
                        }));

        userCommands = new UserCommands(main, nqCommandManager, userCommandBuilder);

        // Admin Stuff
        // Help Menu
        nqCommandManager.command(adminCommandBuilder.commandDescription(NQDescription.of("Opens the help menu"))
                .handler((context) -> {
                    nqCommandManager.sendRootHelp(
                            context.sender(), "nqa", "<main>NotQuests <unimportant>— available admin commands:");
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), context.rawInput().input().split(" "), "[What would you like to do?]", "[...]");
                }));
        nqCommandManager.command(adminCommandBuilder
                .literal("help", NQDescription.of("Shows command help."))
                .optional("query", NQArguments.greedyStringArgument(),
                        NQDescription.of("Optional command name, topic, or search text to show admin help for."))
                .handler(context -> {
                    nqCommandManager.sendRootHelp(
                            context.sender(), "nqa", "<main>NotQuests <unimportant>— available admin commands:");
                }));

        adminCommands = new AdminCommands(main, nqCommandManager, adminCommandBuilder);

        adminEditCommands = new AdminEditCommands(main, nqCommandManager, adminEditCommandBuilder);

        adminTagCommands = new AdminTagCommands(main, nqCommandManager, adminTagCommandBuilder);

        adminItemsCommands = new AdminItemsCommand(main, nqCommandManager, adminItemsCommandBuilder);
    }

    public void setupAdminConversationCommands(
            final ConversationManager
                    conversationManager) { // Has to be done after ConversationManager is initialized
        adminConversationCommands =
                new AdminConversationCommands(
                        main, nqCommandManager, adminConversationCommandBuilder, conversationManager);
    }

    public final NQCommands getNQCommands() {
        return nqCommands;
    }

    public final NQCommandManager getNQCommandManager() {
        return nqCommandManager;
    }

    /**
     * Sole remaining Cloud accessor — kept only for the six not-yet-migrated variable classes that
     * still construct Cloud CommandFlags / a Cloud CommandContext from this manager. Do not add new
     * callers; migrate them to the native framework instead.
     */

    public final NQCommandBuilder getAdminCommandBuilder() {
        return adminCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditCommandBuilder() {
        return adminEditCommandBuilder;
    }

    public final NQCommandBuilder getAdminItemsCommandBuilder() {
        return adminItemsCommandBuilder;
    }

    public final NQCommandBuilder getAdminTagCommandBuilder() {
        return adminTagCommandBuilder;
    }

    public final NQCommandBuilder getAdminConversationCommandBuilder() {
        return adminConversationCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditAddObjectiveCommandBuilder() {
        return adminEditAddObjectiveCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditAddRequirementCommandBuilder() {
        return adminEditAddRequirementCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectiveAddUnlockConditionCommandBuilder() {
        return adminEditObjectiveAddUnlockConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectiveAddProgressConditionCommandBuilder() {
        return adminEditObjectiveAddProgressConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectiveAddCompleteConditionCommandBuilder() {
        return adminEditObjectiveAddCompleteConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminActionsAddConditionCommandBuilder() {
        return adminActionsAddConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminActionsCommandBuilder() {
        return adminActionsCommandBuilder;
    }

    public final NQCommandBuilder getAdminActionsEdituilder() {
        return adminActionsEditCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectiveAddRewardCommandBuilder() {
        return adminEditObjectiveAddRewardCommandBuilder;
    }

    public final NQCommandBuilder getAdminAddActionCommandBuilder() {
        return adminAddActionCommandBuilder;
    }

    public final NQCommandBuilder getAdminExecuteActionCommandBuilder() {
        return adminExecuteActionCommandBuilder;
    }

    public final NQCommandBuilder getAdminAddConditionCommandBuilder() {
        return adminAddConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminConditionCheckCommandBuilder() {
        return adminConditionCheckCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectivesBuilder() {
        return adminEditObjectivesBuilder;
    }

    public final NQCommandBuilder getAdminEditAddRewardCommandBuilder() {
        return adminEditAddRewardCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditAddTriggerCommandBuilder() {
        return adminEditAddTriggerCommandBuilder;
    }

    public final AdminCommands getAdminCommands() {
        return adminCommands;
    }

    public final AdminEditCommands getAdminEditCommands() {
        return adminEditCommands;
    }

    public final AdminTagCommands getAdminTagCommands() {
        return adminTagCommands;
    }

    public final AdminItemsCommand getAdminItemsCommands() {
        return adminItemsCommands;
    }

    public final AdminConversationCommands getAdminConversationCommands() {
        return adminConversationCommands;
    }

    // Player Stuff
    public final UserCommands getUserCommands() {
        return userCommands;
    }

    public final NQCommandBuilder getUserCommandBuilder() {
        return userCommandBuilder;
    }

    public final ObjectiveHolder getObjectiveHolderFromContextAndLevel(final NQCommandContext context, final int level) {
        return rocks.gravili.notquests.paper.commands.arguments.ObjectiveArgument.resolveHolder(context.brigadier(), level);
    }

    public final Objective getObjectiveFromContextAndLevel(final NQCommandContext context, final int level) {
        final ObjectiveHolder holder = rocks.gravili.notquests.paper.commands.arguments.ObjectiveArgument.resolveHolder(context.brigadier(), level + 1);
        return holder instanceof Objective ? (Objective) holder : null;
    }
}
