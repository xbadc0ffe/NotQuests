/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.component.TypedCommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.execution.preprocessor.CommandPreprocessingContext;
import org.incendo.cloud.internal.CommandNode;
import org.incendo.cloud.minecraft.extras.AudienceProvider;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProcessor;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.*;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionParser;
import rocks.gravili.notquests.paper.commands.arguments.MultiActionsParser;
import rocks.gravili.notquests.paper.commands.arguments.NQNPCParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
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
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.bukkit.parser.WorldParser.worldParser;
import static org.incendo.cloud.minecraft.extras.parser.ComponentParser.miniMessageParser;
import static org.incendo.cloud.parser.standard.DoubleParser.doubleParser;
import static org.incendo.cloud.parser.standard.DurationParser.durationParser;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.LongParser.longParser;
import static org.incendo.cloud.parser.standard.StringArrayParser.stringArrayParser;
import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.ActionParser.actionParser;
import static rocks.gravili.notquests.paper.commands.arguments.ApplyOnParser.applyOnParser;
import static rocks.gravili.notquests.paper.commands.arguments.CategoryParser.categoryParser;
import static rocks.gravili.notquests.paper.commands.arguments.ObjectiveParser.objectiveParser;
import static rocks.gravili.notquests.paper.commands.arguments.QuestParser.questParser;

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
    private PaperCommandManager<CommandSender> commandManager;
    // NotQuests' own native-Brigadier command framework (migration target off Cloud).
    private NQCommands nqCommands;
    private NQCommandManager nqCommandManager;

    /**
     * Returns a SuggestionProvider that suggests MiniMessage tags like &lt;red&gt;, &lt;bold&gt;, etc.
     * Use this with NQArguments.greedyStringArgument() to get MiniMessage suggestions while keeping String return type.
     */
    public org.incendo.cloud.suggestion.SuggestionProvider<CommandSender> miniMessageSuggestions() {
        return (context, input) -> {
            java.util.List<Suggestion> completions = new java.util.ArrayList<>();
            // input.input() returns all remaining input — use lastString for the current token
            String rawInput = input.input();
            String[] parts = rawInput.split(" ");
            String lastString = parts.length > 0 ? parts[parts.length - 1] : "";

            if (lastString.startsWith("{")) {
                completions.addAll(getAdminCommands().placeholders.stream().map(Suggestion::suggestion).toList());
            } else if (lastString.startsWith("<")) {
                for (String tag : main.getUtilManager().getMiniMessageTokens()) {
                    completions.add(Suggestion.suggestion("<" + tag + ">"));
                    if (rawInput.contains("<" + tag + ">")) {
                        if (org.apache.commons.lang3.StringUtils.countMatches(rawInput, "<" + tag + ">") > org.apache.commons.lang3.StringUtils.countMatches(rawInput, "</" + tag + ">")) {
                            completions.add(Suggestion.suggestion("</" + tag + ">"));
                        }
                    }
                }
            }
            return java.util.concurrent.CompletableFuture.completedFuture(completions);
        };
    }
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
    private MinecraftHelp<CommandSender> minecraftUserHelp;
    private NQCommandBuilder userCommandBuilder;
    private UserCommands userCommands;
    // Admin
    private MinecraftHelp<CommandSender> minecraftAdminHelp;
    private NQCommandBuilder adminEditObjectivesBuilder;

    private CommandMap commandMap;

    private CommandPostProcessor<CommandSender> commandPostProcessor;

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
        nametag_containsany = NQFlag.builder("nametag_containsany")
                .withArgument(NQArguments.stringArrayArgument())
                .withDescription(NQDescription.of("This word or every word seperated by a space needs to be part of the nametag"))
                .build();

        nametag_equals = NQFlag.builder("nametag_equals")
                .withArgument(NQArguments.stringArrayArgument())
                .withDescription(NQDescription.of("What the nametag has to be equal"))
                .build();

        taskDescription = NQFlag.builder("taskDescription")
                .withArgument(NQArguments.componentArgument(main))
                .withDescription(NQDescription.of("Custom description of the task"))
                .build();

        speakerColor = NQFlag.builder("speakerColor")
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final ArrayList<String> completions = new ArrayList<>();
                    for (final NamedTextColor namedTextColor : NamedTextColor.NAMES.values()) {
                        completions.add("<" + namedTextColor + ">");
                    }
                    return completions;
                })
                .withDescription(NQDescription.of("Color of the speaker name"))
                .build();

        maxDistance = NQFlag.builder("maxDistance")
                .withArgument(NQArguments.integerArgument())
                .withDescription(NQDescription.of("Enter maximum distance of two locations"))
                .build();

        world = NQFlag.builder("world")
                .withArgument(NQArguments.worldArgument())
                .withDescription(NQDescription.of("World Name"))
                .build();

        applyOn = NQFlag.builder("applyOn")
                .withArgument(NQArguments.integerArgument())
                .withSuggestions((context, input) -> java.util.List.of("0", "1", "2"))
                .withDescription(NQDescription.of("To which part of the Quest it should apply (Examples: 'Quest', 'O1', 'O2. (O1 = Objective 1)."))
                .build();

        triggerWorldString = NQFlag.builder("world_name")
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final ArrayList<String> completions = new ArrayList<>();
                    completions.add("ALL");
                    for (final World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                    return completions;
                })
                .withDescription(NQDescription.of("World where the Trigger applies (Examples: 'world_the_end', 'farmworld', 'world', 'ALL')."))
                .build();

        minimumTimeAfterCompletion = NQFlag.builder("waitTimeAfterCompletion")
                .withArgument(NQArguments.longArgument())
                .withDescription(NQDescription.of("Enter minimum time you have to wait after completion."))
                .build();

        categoryFlag = NQFlag.builder("category")
                .withArgument(categoryArgument(main))
                .withDescription(NQDescription.of("Category name"))
                .build();

        delayFlag = NQFlag.builder("delay")
                .withArgument(NQArguments.durationArgument())
                .withDescription(NQDescription.of("Delay in milliseconds"))
                .build();

        locationX = NQFlag.builder("locationX")
                .withArgument(NQArguments.doubleArgument())
                .withDescription(NQDescription.of("Enter x coordinate location"))
                .build();

        locationY = NQFlag.builder("locationY")
                .withArgument(NQArguments.doubleArgument())
                .withDescription(NQDescription.of("Enter y coordinate location"))
                .build();

        locationZ = NQFlag.builder("locationZ")
                .withArgument(NQArguments.doubleArgument())
                .withDescription(NQDescription.of("Enter z coordinate location"))
                .build();
    }

    public final CommandMap getCommandMap() {
        return commandMap;
    }

    public void preSetupCommands() {
        // Cloud command framework
        try {
            commandManager = PaperCommandManager.builder(
                            SenderMapper.<CommandSourceStack, CommandSender>create(
                                    CommandSourceStack::getSender, CommandSenderSourceStack::new))
                    .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                    .buildOnEnable(main.getMain());
            installCommandHintProcessor();
        } catch (final Exception e) {
            main.getLogManager().severe("There was an error setting up the commands.");
            return;
        }

        // NotQuests' own native-Brigadier command framework. Coexists with Cloud on the same Brigadier
        // dispatcher during the migration off Cloud. See commands.framework package.
        try {
            nqCommands = new NQCommands(main);
            nqCommands.hook();
            nqCommands.registerSelfTest();
            nqCommandManager = new NQCommandManager(main, nqCommands);
            nqCommandManager.registerSelfTest();
        } catch (final Throwable t) {
            main.getLogManager().warn("Could not initialize the native command framework: " + t.getMessage());
        }

        preSetupGeneralCommands();
        preSetupUserCommands();
        preSetupAdminCommands();
    }

    public void preSetupGeneralCommands() {
        // brigadier — native on Paper's PaperCommandManager (no registration / async-completion
        // capability check needed; completions are served natively through Brigadier).
        try {
            CloudBrigadierManager<CommandSender, ?> cloudBrigadierManager = commandManager.brigadierManager();
            cloudBrigadierManager.setNativeNumberSuggestions(true);

            // NOT greedy: StringVariableValueParser is used for single-token required arguments (tag
            // names, permission nodes, quest names, ...) that are FOLLOWED by more arguments, e.g.
            // "TagInteger <tagName> <operator> <amount>". A greedy string would swallow the rest of the
            // line into the first argument, leaving nothing for the following ones -> Brigadier reports
            // "incomplete command". string() (quotable) stops at one token while keeping the native
            // Brigadier coloring; a multi-word string *value* can still be passed quoted ("hello world").
            cloudBrigadierManager.registerMapping(
                    new TypeToken<StringVariableValueParser<CommandSender>>() {
                    }, builder -> builder.cloudSuggestions().toConstant(StringArgumentType.string()));

            // Greedy string to prevent false, red brigardier color when entering special symbols like a
            // comma
            cloudBrigadierManager.registerMapping(
                    new TypeToken<NumberVariableValueParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
            cloudBrigadierManager.registerMapping(
                    new TypeToken<BooleanVariableValueParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
            cloudBrigadierManager.registerMapping(
                    new TypeToken<MultiActionsParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
            cloudBrigadierManager.registerMapping(
                    new TypeToken<ItemStackSelectionParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));

            cloudBrigadierManager.registerMapping(
                    new TypeToken<NQNPCParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
        } catch (final Exception e) {
            main.getLogManager().warn("Failed to initialize Brigadier support: <highlight>" + e.getMessage());
        }

        commandPostProcessor = new CommandPostProcessor<>(main);
        commandManager.registerCommandPostProcessor(commandPostProcessor);
    }

    public void preSetupUserCommands() {
        minecraftUserHelp = MinecraftHelp.create("/nq help", commandManager, AudienceProvider.nativeAudience());

        minecraftUserHelp.colors().primary().styleApply(Style.style(NotQuestColors.main).toBuilder());
        minecraftUserHelp.colors().highlight().styleApply(Style.style(NamedTextColor.WHITE).toBuilder());
        minecraftUserHelp.colors().alternateHighlight().styleApply(Style.style(NotQuestColors.highlight).toBuilder());
        minecraftUserHelp.colors().text().styleApply(Style.style(NamedTextColor.GRAY).toBuilder());
        minecraftUserHelp.colors().accent().styleApply(Style.style(NamedTextColor.DARK_GRAY).toBuilder());

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

        minecraftAdminHelp = MinecraftHelp.create("/qa help", commandManager, AudienceProvider.nativeAudience());

        minecraftAdminHelp.colors().primary().styleApply(Style.style(NotQuestColors.main).toBuilder());
        minecraftAdminHelp.colors().highlight().styleApply(Style.style(NamedTextColor.WHITE).toBuilder());
        minecraftAdminHelp.colors().alternateHighlight().styleApply(Style.style(NotQuestColors.highlight).toBuilder());
        minecraftAdminHelp.colors().text().styleApply(Style.style(NamedTextColor.GRAY).toBuilder());
        minecraftAdminHelp.colors().accent().styleApply(Style.style(NamedTextColor.DARK_GRAY).toBuilder());

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

        adminEditCommandBuilder = adminCommandBuilder.literal("edit", "e").required("quest", questArgument(main), NQDescription.of("Quest Name"));
        adminTagCommandBuilder = adminCommandBuilder.literal("tags", "t");
        adminItemsCommandBuilder = adminCommandBuilder.literal("items", "item", "i");
        adminConversationCommandBuilder = adminCommandBuilder.literal("conversations", "c");
        adminEditAddObjectiveCommandBuilder = adminEditCommandBuilder.literal("objectives", "o").literal("add");
        adminEditAddRequirementCommandBuilder = adminEditCommandBuilder.literal("requirements", "req").literal("add");
        adminEditAddRewardCommandBuilder = adminEditCommandBuilder.literal("rewards", "rew").literal("add");
        adminEditAddTriggerCommandBuilder = adminEditCommandBuilder.literal("triggers", "t")
                .literal("add").required("action", actionArgument(main), NQDescription.of("Action which will be executed when the Trigger triggers."));

        adminEditObjectivesBuilder = adminEditCommandBuilder.literal("objectives").literal("edit").required("objectiveId", objectiveArgument(main, 0), NQDescription.of("Objective-ID"));
        adminEditObjectiveAddUnlockConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("unlock").literal("add");
        adminEditObjectiveAddProgressConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("progress").literal("add");
        adminEditObjectiveAddCompleteConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("complete").literal("add");
        adminActionsCommandBuilder = adminCommandBuilder.literal("actions");
        adminActionsEditCommandBuilder = adminActionsCommandBuilder.literal("edit").required("action", actionArgument(main), NQDescription.of("Action Name"));

        adminActionsAddConditionCommandBuilder =
                adminActionsEditCommandBuilder.literal("conditions").literal("add");

        adminEditObjectiveAddRewardCommandBuilder =
                adminEditObjectivesBuilder.literal("rewards", "rew").literal("add");

        adminAddConditionCommandBuilder = adminCommandBuilder
                .literal("conditions")
                .literal("add")
                .required("Condition Identifier", NQArguments.stringArgument(), NQDescription.of("Condition Identifier"),
                        (context, input) -> java.util.List.of("[Enter new, unique Condition Identifier]"));


        adminConditionCheckCommandBuilder = adminCommandBuilder
                .literal("conditions")
                .literal("check");

        adminAddActionCommandBuilder = adminCommandBuilder
                .literal("actions")
                .literal("add")
                .required("Action Identifier", NQArguments.stringArgument(), NQDescription.of("Action Identifier"),
                        (context, input) -> java.util.List.of("[Enter new, unique Action Identifier]"));

        adminExecuteActionCommandBuilder = adminCommandBuilder
                .literal("actions")
                .literal("execute");
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

        // General Stuff
        MinecraftExceptionHandler.<CommandSender>create(sender -> sender)
                .decorator(message -> main.parse("<main>NotQuests > ").append(message))
                .handler(org.incendo.cloud.exception.ArgumentParseException.class, (formatter, ctx) -> {
                    var cause = ctx.exception().getCause();
                    main.getLogManager().debug("Command (argument parse): " + cause.getMessage());
                    if (main.getConfiguration().debug) {
                        ctx.exception().printStackTrace();
                    }
                    return main.parse("<error>" + cause.getMessage());
                })
                .handler(org.incendo.cloud.exception.CommandExecutionException.class, (formatter, ctx) -> {
                    var cause = ctx.exception().getCause();
                    main.getLogManager().debug("Command (execution): " + cause.getMessage());
                    if (main.getConfiguration().debug) {
                        ctx.exception().printStackTrace();
                    }
                    return main.parse("<error>" + cause.getMessage());
                })
                .handler(org.incendo.cloud.exception.InvalidSyntaxException.class, (formatter, ctx) -> {
                    main.getLogManager().debug("Command (syntax): " + ctx.exception().getMessage());
                    if (main.getConfiguration().debug) {
                        ctx.exception().printStackTrace();
                    }
                    return main.parse("<error>Invalid syntax! Correct syntax is: <main>" + ctx.exception().correctSyntax());
                })
                .defaultInvalidSenderHandler()
                .defaultNoPermissionHandler()
                .registerTo(commandManager);
        // User Stuff
        // Help menu

        nqCommandManager.command(
                userCommandBuilder
                        .literal("help")
                        .required("query", NQArguments.greedyStringArgument())
                        .handler(context -> minecraftUserHelp.queryCommands(context.getOrDefault("query", "nq *"), context.sender())));

        userCommands = new UserCommands(main, nqCommandManager, userCommandBuilder);

        // Admin Stuff
        // Help Menu
        nqCommandManager.command(adminCommandBuilder.commandDescription(NQDescription.of("Opens the help menu"))
                .handler((context) -> {
                    minecraftAdminHelp.queryCommands("qa *", context.sender());
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), context.rawInput().input().split(" "), "[What would you like to do?]", "[...]");
                }));
        nqCommandManager.command(adminCommandBuilder
                .literal("help")
                .optional("query", NQArguments.greedyStringArgument())
                .handler(context -> minecraftAdminHelp.queryCommands(context.getOrDefault("query", "qa *"), context.sender())));

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

    /**
     * Installs a single suggestion processor that refreshes the player's command-hint action bar
     * from the command tree on every completion request — for every argument, keyword steps
     * included. This replaces the old approach where each argument parser pushed its own hint, which
     * only covered custom-parser arguments and so was inconsistent.
     */
    private void installCommandHintProcessor() {
        try {
            final SuggestionProcessor<CommandSender> previous = commandManager.suggestionProcessor();
            commandManager.suggestionProcessor((context, suggestions) -> {
                try {
                    showCommandHint(context);
                } catch (final Throwable ignored) {
                    // A hint failure must never affect the actual command suggestions.
                }
                return previous != null ? previous.process(context, suggestions) : suggestions;
            });
        } catch (final Throwable t) {
            main.getLogManager().warn("Could not install the command-hint processor: " + t.getMessage());
        }
        // Keep the action-bar hint alive across typing pauses (action bars fade after a few seconds).
        main.getUtilManager().startCommandHintRefreshTask();
    }

    private void showCommandHint(final CommandPreprocessingContext<CommandSender> context) {
        if (!main.getConfiguration().isActionBarFancyCommandCompletionEnabled()
                && !main.getConfiguration().isTitleFancyCommandCompletionEnabled()
                && !main.getConfiguration().isBossBarFancyCommandCompletionEnabled()) {
            return;
        }
        if (!(context.commandContext().sender() instanceof final Player player)) {
            return;
        }

        final String fullInput = context.commandContext().rawInput().input();
        final String trimmed = fullInput.strip();
        if (trimmed.isEmpty()) {
            return;
        }
        final boolean trailingSpace = fullInput.endsWith(" ");
        final String[] tokens = trimmed.split("\\s+");
        final int completed = trailingSpace ? tokens.length : tokens.length - 1;

        // Walk the tree, consuming already-typed tokens, to the node whose children are the
        // candidates for the argument currently being typed.
        CommandNode<CommandSender> node = commandManager.commandTree().rootNode();
        for (int i = 0; i < completed && node != null; i++) {
            node = advanceNode(node, tokens[i]);
        }
        if (node == null) {
            return;
        }

        final String hint = buildHint(node);
        if (hint == null || hint.isBlank()) {
            return;
        }
        main.getUtilManager().sendCommandHint(player, fullInput, hint);
    }

    private CommandNode<CommandSender> advanceNode(final CommandNode<CommandSender> node, final String token) {
        CommandNode<CommandSender> valueChild = null;
        for (final CommandNode<CommandSender> child : node.children()) {
            final CommandComponent<CommandSender> component = child.component();
            if (component == null) {
                continue;
            }
            if (component.type() == CommandComponent.ComponentType.LITERAL) {
                if (component.name().equalsIgnoreCase(token)
                        || component.aliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(token))) {
                    return child; // exact keyword match wins
                }
            } else if (component.type() != CommandComponent.ComponentType.FLAG) {
                valueChild = child; // a value argument consumes any token
            }
        }
        return valueChild;
    }

    private String buildHint(final CommandNode<CommandSender> node) {
        boolean hasLiteral = false;
        CommandComponent<CommandSender> valueArg = null;
        for (final CommandNode<CommandSender> child : node.children()) {
            final CommandComponent<CommandSender> component = child.component();
            if (component == null) {
                continue;
            }
            switch (component.type()) {
                case LITERAL -> hasLiteral = true;
                case REQUIRED_VARIABLE, OPTIONAL_VARIABLE -> {
                    if (valueArg == null) {
                        valueArg = component;
                    }
                }
                default -> {
                    // flags etc. are not shown in the hint
                }
            }
        }

        // Keyword steps: don't dump every sub-command (it overflows the bar and is unreadable) — just
        // signal that one of several options goes here. The vanilla tab popup still lists the real ones.
        if (hasLiteral) {
            return "<option>";
        }
        if (valueArg != null) {
            final String description = valueArg.description().textDescription();
            return "[" + (description != null && !description.isBlank() ? description : valueArg.name()) + "]";
        }
        return null;
    }

    public final PaperCommandManager<CommandSender> getPaperCommandManager() {
        return commandManager;
    }

    public final NQCommands getNQCommands() {
        return nqCommands;
    }

    public final NQCommandManager getNQCommandManager() {
        return nqCommandManager;
    }

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

    public final ObjectiveHolder getObjectiveHolderFromContextAndLevel(final CommandContext<CommandSender> context, final int level) {
        final ObjectiveHolder objectiveHolder;
        if (level == 0) {
            objectiveHolder = context.get("quest");
        } else if (level == 1) {
            objectiveHolder = context.get("objectiveId");
        } else {
            objectiveHolder = context.get("objectiveId" + level);
        }
        return objectiveHolder;
    }

    public final Objective getObjectiveFromContextAndLevel(final CommandContext<CommandSender> context, final int level) {
        final Objective objective;
        main.getLogManager().debug(context.get("objectiveId"));
        main.getLogManager().debug(context.get("objectiveId" + (level + 1)));
        if (level == 0) {
            objective = context.get("objectiveId");
        } else {
            objective = context.get("objectiveId" + (level + 1));
        }
        return objective;
    }

    public final ObjectiveHolder getObjectiveHolderFromContextAndLevel(final NQCommandContext context, final int level) {
        final ObjectiveHolder objectiveHolder;
        if (level == 0) {
            objectiveHolder = context.get("quest");
        } else if (level == 1) {
            objectiveHolder = context.get("objectiveId");
        } else {
            objectiveHolder = context.get("objectiveId" + level);
        }
        return objectiveHolder;
    }

    public final Objective getObjectiveFromContextAndLevel(final NQCommandContext context, final int level) {
        final Objective objective;
        main.getLogManager().debug(context.get("objectiveId"));
        main.getLogManager().debug(context.get("objectiveId" + (level + 1)));
        if (level == 0) {
            objective = context.get("objectiveId");
        } else {
            objective = context.get("objectiveId" + (level + 1));
        }
        return objective;
    }
}
