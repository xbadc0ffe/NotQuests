package com.notquests.paper.conditions;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQFlag;

import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.conditions.CompletedObjective;
import com.notquests.paper.builtin.conditions.Date;
import com.notquests.paper.builtin.conditions.BooleanVariableCondition;
import com.notquests.paper.builtin.conditions.ItemStackListVariableCondition;
import com.notquests.paper.builtin.conditions.ListVariableCondition;
import com.notquests.paper.builtin.conditions.NumberVariableCondition;
import com.notquests.paper.builtin.conditions.StringVariableCondition;
import com.notquests.paper.builtin.conditions.WorldTime;
import com.notquests.paper.managers.data.Category;
import com.notquests.paper.registry.ConditionType;
import com.notquests.paper.registry.DefinedCondition;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.actions.Action;
import com.notquests.paper.conditions.Condition;
import com.notquests.paper.conditions.ConditionFor;
import com.notquests.paper.conditions.Condition.ConditionResult;
import com.notquests.paper.objectives.Objective;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class ConditionCatalog {
    private final NotQuests main;
    private final NQFlag playerSelectorCommandFlag;

    private final HashMap<String, ConditionType> conditionTypes;


    public ConditionCatalog(final NotQuests main) {
        this.main = main;
        conditionTypes = new HashMap<>();
        playerSelectorCommandFlag = NQFlag.builder(
                        "player",
                        NQDescription.of("Player whose quest, tag, inventory, permission, or variable data should be checked."))
                .withArgument(NQArguments.playerArgument())
                .build();
        registerDefaultConditions();

    }

    public void registerDefaultConditions() {
        main.getLogManager().info("Registering conditions...");
        conditionTypes.clear();

        CompletedObjective.register(main, this);
        WorldTime.register(main, this);
        Date.register(main, this);

        NumberVariableCondition.register(main, this);
        StringVariableCondition.register(main, this);
        BooleanVariableCondition.register(main, this);
        ListVariableCondition.register(main, this);
        ItemStackListVariableCondition.register(main, this);


    }

    public ConditionType.Builder condition(final String identifier) {
        return new ConditionType.Builder(main, this, identifier);
    }

    public void registerCondition(final ConditionType definition) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering condition <highlight>" + definition.id());
        }
        conditionTypes.put(definition.id(), definition);

        final NQFlag negateFlag =
                NQFlag.builder("negate", NQDescription.of("Invert the result so this condition passes when it would normally fail."))
                        .build();
        final NQFlag allowProgressDecreaseIfNotFulfilledFlag =
                NQFlag.builder(
                                "allowProgressDecreaseIfNotFulfilled",
                                NQDescription.of("Allow objective progress to decrease even when this progress condition is not currently fulfilled."))
                        .build();

        registerDefinitionCommands(
                definition,
                conditionCommandBuilder(
                                main.getCommandManager().getAdminEditAddRequirementCommandBuilder(),
                                definition,
                                "Creates a new " + definition.displayName() + " requirement")
                        .flag(negateFlag),
                ConditionFor.QUEST);
        registerDefinitionCommands(
                definition,
                conditionCommandBuilder(
                                main.getCommandManager().getAdminEditObjectiveAddUnlockConditionCommandBuilder(),
                                definition,
                                "Creates a new " + definition.displayName() + " unlock condition")
                        .flag(negateFlag),
                ConditionFor.OBJECTIVEUNLOCK);
        registerDefinitionCommands(
                definition,
                conditionCommandBuilder(
                                main.getCommandManager().getAdminEditObjectiveAddProgressConditionCommandBuilder(),
                                definition,
                                "Creates a new " + definition.displayName() + " progress condition")
                        .flag(negateFlag)
                        .flag(allowProgressDecreaseIfNotFulfilledFlag),
                ConditionFor.OBJECTIVEPROGRESS);
        registerDefinitionCommands(
                definition,
                conditionCommandBuilder(
                                main.getCommandManager().getAdminEditObjectiveAddCompleteConditionCommandBuilder(),
                                definition,
                                "Creates a new " + definition.displayName() + " complete condition")
                        .flag(negateFlag),
                ConditionFor.OBJECTIVECOMPLETE);
        registerDefinitionCommands(
                definition,
                conditionCommandBuilder(
                                main.getCommandManager().getAdminAddConditionCommandBuilder(),
                                definition,
                                "Creates a new " + definition.displayName() + " saved condition")
                        .flag(negateFlag)
                        .flag(main.getCommandManager().categoryFlag),
                ConditionFor.ConditionsYML);
        registerDefinitionCommands(
                definition,
                conditionCommandBuilder(
                                main.getCommandManager().getAdminActionsAddConditionCommandBuilder(),
                                definition,
                                "Creates a new " + definition.displayName() + " condition for a saved action")
                        .flag(negateFlag),
                ConditionFor.Action);
        registerDefinitionCommands(
                definition,
                conditionCommandBuilder(
                                main.getCommandManager().getAdminConditionCheckCommandBuilder(),
                                definition,
                                "Checks a " + definition.displayName() + " condition inline")
                        .flag(negateFlag)
                        .flag(playerSelectorCommandFlag),
                ConditionFor.INLINE);
    }

    private NQCommandBuilder conditionCommandBuilder(
            final NQCommandBuilder base, final ConditionType definition, final String commandDescription) {
        final NQCommandBuilder described = base.commandDescription(NQDescription.of(commandDescription));
        if (!definition.usesTypeLiteral()) {
            return described;
        }
        return described.literal(definition.id(), NQDescription.of(definition.description()));
    }

    private void registerDefinitionCommands(
            final ConditionType definition, final NQCommandBuilder builder, final ConditionFor conditionFor) {
        definition.registerCommands(builder, conditionFor);
    }

    public final Condition createCondition(final String type) {
        final ConditionType definition = conditionTypes.get(type);
        if (definition != null) {
            return definition.createCondition();
        }
        return null;
    }

    public final String getConditionType(final Condition condition) {
        if (condition instanceof final DefinedCondition definedCondition) {
            return definedCondition.definition().id();
        }
        return null;
    }

    public final HashMap<String, ConditionType> getConditionTypesAndIdentifiers() {
        return conditionTypes;
    }

    public final Collection<ConditionType> getConditions() {
        return conditionTypes.values();
    }

    public final Collection<String> getConditionIdentifiers() {
        return conditionTypes.keySet();
    }

    public void addCondition(final Condition condition, final NQCommandContext context, final ConditionFor conditionFor) {
        condition.setNegated(context.flags().isPresent("negate"));


        final Quest quest = context.getOrDefault("quest", null);
        Objective objectiveOfQuest = null;
        if (quest != null && context.<Objective>get("objectiveId") != null) {
            objectiveOfQuest = main.getCommandManager().getObjectiveFromContextAndLevel(context, 0); //TODO: Support nested objectives
        }

        final String conditionIdentifier = context.getOrDefault("Condition Identifier", "");


        String actionIdentifier = context.getOrDefault("Action Identifier", "");
        Action foundAction = context.getOrDefault("action", null);


        if (quest != null) {
            condition.setObjectiveHolder(quest);
            condition.setCategory(quest.getCategory());
            if (objectiveOfQuest != null) {//Objective Condition
                condition.setObjective(objectiveOfQuest);

                if (conditionFor == ConditionFor.OBJECTIVEPROGRESS) {
                    final boolean allowProgressDecreaseIfNotFulfilled = context.flags().isPresent("allowProgressDecreaseIfNotFulfilled");
                    condition.setConditionID(objectiveOfQuest.getFreeProgressConditionID());
                    condition.setObjectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled(allowProgressDecreaseIfNotFulfilled);

                    objectiveOfQuest.addProgressCondition(condition, true);

                    context.sender().sendMessage(main.parse(
                            "<success>" + getConditionType(condition) + " Condition successfully added to Objective <highlight>"
                                    + objectiveOfQuest.getDisplayNameOrIdentifier() + "</highlight>!"));

                } else if (conditionFor == ConditionFor.OBJECTIVECOMPLETE) {
                    condition.setConditionID(objectiveOfQuest.getFreeCompleteConditionID());

                    objectiveOfQuest.addCompleteCondition(condition, true);

                    context.sender().sendMessage(main.parse(
                            "<success>" + getConditionType(condition) + " Complete Condition successfully added to Objective <highlight>"
                                    + objectiveOfQuest.getDisplayNameOrIdentifier() + "</highlight>!"));

                } else {
                    condition.setConditionID(objectiveOfQuest.getFreeUnlockConditionID());

                    objectiveOfQuest.addUnlockCondition(condition, true);

                    context.sender().sendMessage(main.parse(
                            "<success>" + getConditionType(condition) + " Unlock Condition successfully added to Objective <highlight>"
                                    + objectiveOfQuest.getDisplayNameOrIdentifier() + "</highlight>!"));
                }
            } else { //Quest Requirement
                condition.setConditionID(quest.getFreeRequirementID());
                quest.addRequirement(condition, true);

                context.sender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition) + " Requirement successfully added to Quest <highlight>"
                                + quest.getIdentifier() + "</highlight>!"
                ));
            }
        } else {
            if (conditionFor == ConditionFor.INLINE) {
                //Execute action here
                final Player playerSelector = context.flags().getValue(playerSelectorCommandFlag, null);

                final UUID uuid;
                final Player player;
                if (playerSelector != null) {
                    uuid = playerSelector.getUniqueId();
                    player = playerSelector;
                } else if (context.sender() instanceof final Player senderPlayer) {
                    uuid = senderPlayer.getUniqueId();
                    player = senderPlayer;
                } else {
                    uuid = null;
                    player = null;
                }
                if (uuid != null) {
                    final ConditionResult conditionResult = condition.check(main.getQuestPlayerManager().getOrCreateQuestPlayer(uuid));
                    main.sendMessage(context.sender(), "<main>" + condition.getConditionType() + " condition result for player " + (player != null ? main.getMiniMessage().serialize(player.name()) : "unknown") + ":</main> <highlight>" + conditionResult.message() + (conditionResult.fulfilled() ? "<positive>fulfilled" : " <negative>(not fulfilled)"));
                }
            } else if (conditionIdentifier != null && !conditionIdentifier.isBlank()) { //conditions.yml

                if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category = context.flags().getValue(main.getCommandManager().categoryFlag, main.getDataManager().getDefaultCategory());
                    condition.setCategory(category);
                }

                if (main.getSavedConditions().getCondition(conditionIdentifier) == null) {
                    context.sender().sendMessage((main.parse(main.getSavedConditions().addCondition(conditionIdentifier, condition))));
                } else {
                    context.sender().sendMessage(main.parse("<error>Error! A condition with the name <highlight>" + conditionIdentifier + "</highlight> already exists!"));
                }
            } else { //Condition For conditions.yml action

                if (foundAction != null || (actionIdentifier != null && !actionIdentifier.isBlank())) {

                    foundAction = foundAction != null ? foundAction : main.getSavedActions().getAction(actionIdentifier);
                    if (foundAction != null) {
                        actionIdentifier = foundAction.getActionName();

                        condition.setCategory(foundAction.getCategory());


                        foundAction.addCondition(condition, true, foundAction.getCategory().getActionsConfig(), "actions." + actionIdentifier);
                        main.getSavedActions().saveActions(foundAction.getCategory());
                        context.sender().sendMessage(main.parse(
                                "<success>" + getConditionType(condition) + " Condition successfully added to Action <highlight>"
                                        + foundAction.getActionName() + "</highlight>!"));
                    }
                }
            }
        }
    }

    public final Condition getConditionFromString(final String conditionString) {
        return null; //TODO
    }

    public void updateVariableConditions() {
        main.getLogManager().info("Re-registering variable conditions due to variable changes...");
        NumberVariableCondition.register(main, this);
        StringVariableCondition.register(main, this);
        BooleanVariableCondition.register(main, this);
        ListVariableCondition.register(main, this);
        ItemStackListVariableCondition.register(main, this);
    }
}
