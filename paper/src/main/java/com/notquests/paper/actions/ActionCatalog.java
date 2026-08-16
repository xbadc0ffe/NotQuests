package com.notquests.paper.actions;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQFlag;

import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.managers.data.Category;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.builtin.actions.ActionChain;
import com.notquests.paper.builtin.actions.BetonQuestFireEvent;
import com.notquests.paper.builtin.actions.BetonQuestFireInlineEvent;
import com.notquests.paper.builtin.actions.Beam;
import com.notquests.paper.builtin.actions.BroadcastMessage;
import com.notquests.paper.builtin.actions.Chat;
import com.notquests.paper.builtin.actions.CloseInventory;
import com.notquests.paper.builtin.actions.CompleteQuest;
import com.notquests.paper.builtin.actions.ConsoleCommand;
import com.notquests.paper.builtin.actions.FailQuest;
import com.notquests.paper.builtin.actions.GiveItem;
import com.notquests.paper.builtin.actions.GiveQuest;
import com.notquests.paper.builtin.actions.BooleanVariableAction;
import com.notquests.paper.builtin.actions.ItemStackListVariableAction;
import com.notquests.paper.builtin.actions.ListVariableAction;
import com.notquests.paper.builtin.actions.NumberVariableAction;
import com.notquests.paper.builtin.actions.OpenGui;
import com.notquests.paper.builtin.actions.PlayerCommand;
import com.notquests.paper.builtin.actions.PlaySound;
import com.notquests.paper.builtin.actions.SendMessage;
import com.notquests.paper.builtin.actions.ShowActionBar;
import com.notquests.paper.builtin.actions.ShowTitle;
import com.notquests.paper.builtin.actions.SpawnMob;
import com.notquests.paper.builtin.actions.SpawnParticle;
import com.notquests.paper.builtin.actions.StartConversation;
import com.notquests.paper.builtin.actions.StringVariableAction;
import com.notquests.paper.builtin.actions.Teleport;
import com.notquests.paper.builtin.actions.TriggerCommandActionType;
import com.notquests.paper.objectives.Objective;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class ActionCatalog {
    private final NotQuests main;
    private final NQFlag playerSelectorCommandFlag;
    private final HashMap<String, ActionType> actionTypes;

    public ActionCatalog(final NotQuests main) {
        this.main = main;
        actionTypes = new HashMap<>();
        playerSelectorCommandFlag = NQFlag.builder(
                        "player",
                        NQDescription.of("Player who should be used as the target when executing this action from a command."))
                .withArgument(NQArguments.playerArgument())
                .build();
        registerDefaultActions();
    }

    public void registerDefaultActions() {
        main.getLogManager().info("Registering actions...");

        actionTypes.clear();
        ActionChain.register(main, this);
        GiveQuest.register(main, this);
        CompleteQuest.register(main, this);
        FailQuest.register(main, this);
        TriggerCommandActionType.register(main, this);
        StartConversation.register(main, this);

        ConsoleCommand.register(main, this);
        PlayerCommand.register(main, this);
        Chat.register(main, this);

        GiveItem.register(main, this);
        SpawnMob.register(main, this);
        SendMessage.register(main, this);
        BroadcastMessage.register(main, this);
        ShowTitle.register(main, this);
        ShowActionBar.register(main, this);

        PlaySound.register(main, this);
        SpawnParticle.register(main, this);
        Teleport.register(main, this);


        NumberVariableAction.register(main, this);
        StringVariableAction.register(main, this);
        BooleanVariableAction.register(main, this);
        ListVariableAction.register(main, this);
        ItemStackListVariableAction.register(main, this);

        Beam.register(main, this);

        OpenGui.register(main, this);
        CloseInventory.register(main, this);

        if (main.getIntegrationsManager().isBetonQuestEnabled()) {
            BetonQuestFireEvent.register(main, this);
            BetonQuestFireInlineEvent.register(main, this);
        }
    }

    public ActionType.Builder action(final String identifier) {
        return new ActionType.Builder(main, this, identifier);
    }

    public void registerAction(final ActionType definition) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering action <highlight>" + definition.id());
        }
        actionTypes.put(definition.id(), definition);

        registerDefinitionCommands(
                definition,
                actionCommandBuilder(
                        main.getCommandManager().getAdminEditAddRewardCommandBuilder(),
                        definition,
                        "Creates a new " + definition.displayName() + " action"),
                ActionFor.QUEST);
        registerDefinitionCommands(
                definition,
                actionCommandBuilder(
                        main.getCommandManager().getAdminEditObjectiveAddRewardCommandBuilder(),
                        definition,
                        "Creates a new " + definition.displayName() + " action"),
                ActionFor.OBJECTIVE);
        registerDefinitionCommands(
                definition,
                actionCommandBuilder(
                                main.getCommandManager().getAdminAddActionCommandBuilder(),
                                definition,
                                "Creates a new " + definition.displayName() + " action")
                        .flag(main.getCommandManager().categoryFlag)
                        .flag(main.getCommandManager().delayFlag),
                ActionFor.ActionsYML);
        registerDefinitionCommands(
                definition,
                actionCommandBuilder(
                                main.getCommandManager().getAdminExecuteActionCommandBuilder(),
                                definition,
                                "Executes a new " + definition.displayName() + " action inline")
                        .flag(playerSelectorCommandFlag)
                        .flag(main.getCommandManager().delayFlag),
                ActionFor.INLINE);
    }

    private NQCommandBuilder actionCommandBuilder(
            final NQCommandBuilder base, final ActionType definition, final String commandDescription) {
        final NQCommandBuilder described = base.commandDescription(NQDescription.of(commandDescription));
        if (!definition.usesTypeLiteral()) {
            return described;
        }
        return described.literal(definition.id(), NQDescription.of(definition.description()));
    }

    private void registerDefinitionCommands(
            final ActionType definition, final NQCommandBuilder builder, final ActionFor actionFor) {
        definition.registerCommands(builder, actionFor);
    }

    public final Action createAction(final String type) {
        final ActionType definition = actionTypes.get(type);
        if (definition != null) {
            return definition.createAction();
        }
        return null;
    }

    public final String getActionType(final Action action) {
        if (action instanceof final DefinedAction definedAction) {
            return definedAction.definition().id();
        }
        return null;
    }

    public final HashMap<String, ActionType> getActionTypesAndIdentifiers() {
        return actionTypes;
    }

    public final Collection<ActionType> getActions() {
        return actionTypes.values();
    }

    public final Collection<String> getActionIdentifiers() {
        return actionTypes.keySet();
    }

    public void addAction(final Action action, final NQCommandContext context, final ActionFor actionFor) {
        final Quest quest = context.getOrDefault("quest", null);
        Objective objectiveOfQuest = null;
        if (quest != null && context.<Objective>get("objectiveId") != null) {
            objectiveOfQuest = main.getCommandManager().getObjectiveFromContextAndLevel(context, 0); //TODO: Support nested objectives
        }
        final String actionIdentifier =
                context.getOrDefault("Action Identifier", context.getOrDefault("action", ""));

        if (context.flags().contains(main.getCommandManager().delayFlag)) {
            final Duration delayDuration =
                    context
                            .flags()
                            .getValue(
                                    main.getCommandManager().delayFlag,
                                    null);
            if (delayDuration != null) {
                action.setExecutionDelay(delayDuration.toMillis());
            }
        }

        if (quest != null) {
            action.setObjectiveHolder(quest);
            action.setCategory(quest.getCategory());
            if (objectiveOfQuest != null) { // Objective Reward
                action.setObjective(objectiveOfQuest);
                action.setActionID(objectiveOfQuest.getFreeRewardID());

                objectiveOfQuest.addReward(
                        action,
                        true); // TODO: Also do addAction which are executed when the objective is unlocked (and
                // not just when completed)

                context.sender().sendMessage(main.parse("<success>" + getActionType(action)
                        + " Reward successfully added to Objective <highlight>"
                        + objectiveOfQuest.getDisplayNameOrIdentifier()
                        + "</highlight>!"));
            } else { // Quest Reward
                action.setActionID(quest.getFreeRewardID());
                quest.addReward(action, true);

                context.sender().sendMessage(main.parse("<success>"
                        + getActionType(action)
                        + " Reward successfully added to Quest <highlight>"
                        + quest.getIdentifier()
                        + "</highlight>!"));
            }
        } else {
            if (actionFor == ActionFor.INLINE) {
                //Execute action here
                final Player playerSelector = context.flags().getValue(playerSelectorCommandFlag, null);

                final UUID uuid;
                if (playerSelector != null) {
                    uuid = playerSelector.getUniqueId();
                } else if (context.sender() instanceof final Player senderPlayer) {
                    uuid = senderPlayer.getUniqueId();
                } else {
                    uuid = null;
                }

                if (uuid != null) {
                    action.execute(main.getQuestPlayerManager().getOrCreateQuestPlayer(uuid));
                }
            } else if (actionIdentifier != null && !actionIdentifier.isBlank()) { // actions.yml
                if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category =
                            context
                                    .flags()
                                    .getValue(
                                            main.getCommandManager().categoryFlag,
                                            main.getDataManager().getDefaultCategory());
                    action.setCategory(category);
                }

                if (main.getSavedActions().getAction(actionIdentifier) == null) {
                    context.sender().sendMessage(main.parse(main.getSavedActions().addAction(actionIdentifier, action)));
                } else {
                    context.sender().sendMessage(main.parse("<error>Error! An action with the name <highlight>"
                            + actionIdentifier
                            + "</highlight> already exists!"));
                }
            }
        }
    }

    public void updateVariableActions() {
        main.getLogManager().info("Re-registering variable action commands due to variable changes...");
        NumberVariableAction.register(main, this);
        StringVariableAction.register(main, this);
        BooleanVariableAction.register(main, this);
        ListVariableAction.register(main, this);
        ItemStackListVariableAction.register(main, this);
    }
}
