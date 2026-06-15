package rocks.gravili.notquests.paper.managers.registering;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import static rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableArgument.booleanVariableArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableArgument.stringVariableArgument;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.variables.*;
import rocks.gravili.notquests.paper.structs.variables.hooks.*;
import rocks.gravili.notquests.paper.structs.variables.reflectionVariables.*;
import rocks.gravili.notquests.paper.structs.variables.tags.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;


public class VariablesManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Variable<?>>> variables;
    public ArrayList<String> alreadyFullRegisteredVariables = new ArrayList<>();

    EvaluationEnvironment env = new EvaluationEnvironment();

    public VariablesManager(final NotQuests main) {
        this.main = main;
        variables = new HashMap<>();

        registerDefaultVariables();

        env.addFunction("test", 0, d -> 4);
        CompiledExpression exp = Crunch.compileExpression("test() + 1", env);
        exp.evaluate(); // will return 5
    }

    public void registerDefaultVariables() {
        main.getLogManager().info("Registering variables...");

        variables.clear();
        registerVariable("True", TrueVariable.class);
        registerVariable("False", FalseVariable.class);
        registerVariable("Condition", ConditionVariable.class);

        registerVariable("QuestPoints", QuestPointsVariable.class);
        registerVariable("Money", MoneyVariable.class);
        registerVariable("ActiveQuests", ActiveQuestsVariable.class);
        registerVariable("CompletedQuests", CompletedQuestsVariable.class);
        registerVariable("CompletedObjectiveIDsOfQuest", CompletedObjectiveIDsOfQuestVariable.class);
        registerVariable("Permission", PermissionVariable.class);
        registerVariable("Statistic", PlayerStatisticVariable.class);
        registerVariable("Name", PlayerNameVariable.class);
        registerVariable("Experience", PlayerExperienceVariable.class);
        registerVariable("ExperienceLevel", PlayerExperienceLevelVariable.class);
        registerVariable("FoodLevel", PlayerFoodLevelVariable.class);
        registerVariable("Saturation", PlayerSaturationVariable.class);
        registerVariable("CurrentWorld", PlayerCurrentWorldVariable.class);
        registerVariable("CurrentPositionX", PlayerCurrentPositionXVariable.class);
        registerVariable("CurrentPositionY", PlayerCurrentPositionYVariable.class);
        registerVariable("CurrentPositionZ", PlayerCurrentPositionZVariable.class);
        registerVariable("DistanceToLocation", DistanceToLocationVariable.class);
        registerVariable("NearbyEntityCount", NearbyEntityCountVariable.class);
        registerVariable("Weather", WeatherVariable.class);
        registerVariable("RandomNumberBetweenRange", RandomNumberBetweenRangeVariable.class);
        registerVariable("PlaytimeTicks", PlayerPlaytimeTicksVariable.class);
        registerVariable("PlaytimeMinutes", PlayerPlaytimeMinutesVariable.class);
        registerVariable("PlaytimeHours", PlayerPlaytimeHoursVariable.class);

        registerVariable("Glowing", PlayerGlowingVariable.class);
        registerVariable("Op", PlayerOpVariable.class);
        registerVariable("Climbing", PlayerClimbingVariable.class);
        registerVariable("InLava", PlayerInLavaVariable.class);
        registerVariable("InWater", PlayerInWaterVariable.class);
        registerVariable("Ping", PlayerPingVariable.class);
        registerVariable("WalkSpeed", PlayerWalkSpeedVariable.class);
        registerVariable("FlySpeed", PlayerFlySpeedVariable.class);


        registerVariable("Sleeping", PlayerSleepingVariable.class);
        registerVariable("Sneaking", PlayerSneakingVariable.class);
        registerVariable("Sprinting", PlayerSprintingVariable.class);
        registerVariable("Swimming", PlayerSwimmingVariable.class);
        registerVariable("Health", PlayerHealthVariable.class);
        registerVariable("MaxHealth", PlayerMaxHealthVariable.class);
        registerVariable("GameMode", PlayerGameModeVariable.class);
        registerVariable("Flying", PlayerFlyingVariable.class);
        registerVariable("DayOfWeek", DayOfWeekVariable.class);
        registerVariable("CurrentBiome", PlayerCurrentBiomeVariable.class);

        registerVariable("Chance", ChanceVariable.class);
        registerVariable("Advancement", AdvancementVariable.class);
        registerVariable("Inventory", InventoryVariable.class);
        registerVariable("EnderChest", EnderChestVariable.class);

        registerVariable("ContainerInventory", ContainerInventoryVariable.class);
        registerVariable("Block", BlockVariable.class);

        registerVariable("TagBoolean", BooleanTagVariable.class);
        registerVariable("TagInteger", IntegerTagVariable.class);
        registerVariable("TagFloat", FloatTagVariable.class);
        registerVariable("TagDouble", DoubleTagVariable.class);
        registerVariable("TagString", StringTagVariable.class);
        registerVariable("QuestOnCooldown", QuestOnCooldownVariable.class);
        registerVariable("QuestAbleToAccept", QuestAbleToAcceptVariable.class);
        registerVariable("QuestReachedMaxAccepts", QuestReachedMaxAcceptsVariable.class);
        registerVariable("QuestReachedMaxCompletions", QuestReachedMaxCompletionsVariable.class);
        registerVariable("QuestReachedMaxFails", QuestReachedMaxFailsVariable.class);

        registerVariable("ItemInInventoryEnchantments", ItemInInventoryEnchantmentsVariable.class);

        registerVariable("ReflectionStaticDouble", ReflectionStaticDoubleVariable.class);
        registerVariable("ReflectionStaticFloat", ReflectionStaticFloatVariable.class);
        registerVariable("ReflectionStaticInteger", ReflectionStaticIntegerVariable.class);
        registerVariable("ReflectionStaticBoolean", ReflectionStaticBooleanVariable.class);
        registerVariable("ReflectionStaticString", ReflectionStaticStringVariable.class);


        if (main.getIntegrationsManager().isPlaceholderAPIEnabled()) {
            registerVariable("PlaceholderAPINumber", PlaceholderAPINumberVariable.class);
            registerVariable("PlaceholderAPIString", PlaceholderAPIStringVariable.class);
        }
        if (main.getIntegrationsManager().isTownyEnabled()) {
            registerVariable("TownyNationTownCount", TownyNationTownCountVariable.class);
            registerVariable("TownyTownResidentCount", TownyTownResidentCountVariable.class);
            registerVariable("TownyTownPlotCount", TownyTownPlotCountVariable.class);
            registerVariable("TownyNationName", TownyNationNameVariable.class);
        }

        if (main.getIntegrationsManager().isFloodgateEnabled()) {
            registerVariable("FloodgateIsFloodgatePlayer", FloodgateIsFloodgatePlayerVariable.class);
        }
        if (main.getIntegrationsManager().isBetonQuestEnabled()) {
            registerVariable("BetonQuestCondition", BetonQuestConditionVariable.class);
        }
        registerVariableCheckCommands();
    }

    public void registerVariableCheckCommands() {
        //Variable check commands
        for (final String variableString : getVariableIdentifiers()) {

            final Variable<?> variable = getVariableFromString(variableString);

            if (variable == null) {
                continue;
            }
            if (alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }


            final rocks.gravili.notquests.paper.commands.framework.NQFlag playerSelectorCommandFlag =
                    rocks.gravili.notquests.paper.commands.framework.NQFlag.builder(
                                    "player",
                                    NQDescription.of("Player whose current variable value should be checked; defaults to the command sender when possible."))
                            .withArgument(rocks.gravili.notquests.paper.commands.framework.NQArguments.playerArgument())
                            .build();


            final NQCommandBuilder variableCheckCommandBuilder = main.getCommandManager().getAdminCommandBuilder()
                    .literal("variables", NQDescription.of("Evaluates NotQuests variables for a player or the command sender."), "variable")
                    .literal("check", NQDescription.of("Displays a NotQuests variable's value for a player or the command sender."));


            main.getCommandManager().getNQCommandManager().command(registerVariableCommands(variableString, variableCheckCommandBuilder)
                    .flag(playerSelectorCommandFlag)
                    .handler((context) -> {

                        final Player playerSelector = context.flags().getValue(playerSelectorCommandFlag, null);

                        final Player player;
                        final UUID uuid;
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


                        final HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                            additionalStringArguments.put(stringParser.getIdentifier(), context.get(stringParser.getIdentifier()));
                        }
                        variable.setAdditionalStringArguments(additionalStringArguments);

                        final HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                            additionalNumberArguments.put(numberParser.getIdentifier(), new NumberExpression(main, context.get(numberParser.getIdentifier())));
                        }
                        variable.setAdditionalNumberArguments(additionalNumberArguments);

                        final HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                            additionalBooleanArguments.put(booleanParser.getIdentifier(), new NumberExpression(main, context.get(booleanParser.getIdentifier())));
                        }
                        for (final rocks.gravili.notquests.paper.commands.framework.NQFlag commandFlag : variable.getRequiredBooleanFlags()) {
                            additionalBooleanArguments.put(commandFlag.name(), context.flags().isPresent(commandFlag.name()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        variable.setAdditionalBooleanArguments(additionalBooleanArguments);


                        final Object variableValue = variable.getValue(uuid != null ? main.getQuestPlayerManager().getOrCreateQuestPlayer(uuid) : null);
                        String variableValueString = variableValue != null ? variableValue.toString() : "null";

                        if (variableValue != null) {
                            if (variable.getVariableDataType() == VariableDataType.LIST) {
                                variableValueString = String.join(",", (String[]) variableValue);
                            } else if (variable.getVariableDataType() == VariableDataType.ITEMSTACKLIST) {
                                variableValueString = "";
                                int counter = 0;
                                for (final ItemStack itemStack : (ItemStack[]) variableValue) {
                                    if (counter == 0) {
                                        variableValueString += itemStack.toString();
                                    } else {
                                        variableValueString += ", " + itemStack.toString();
                                    }
                                    counter++;
                                }
                            }
                        }
                        main.sendMessage(context.sender(), "<main>" + variableString + " variable (" + variable.getVariableDataType() + ") result for player " + (player != null ? main.getMiniMessage().serialize(player.name()) : "unknown") + ":</main> <highlight>" + variableValueString);
                    })
            );


        }
    }

    public NQCommandBuilder registerVariableCommands(
            String variableString, NQCommandBuilder builder) {
        NQCommandBuilder newBuilder =
                builder.literal(variableString, variableLiteralDescription(variableString));

        Variable<?> variable = getVariableFromString(variableString);
        if (variable != null) {
            if (variable.getRequiredStrings() != null) {
                for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                    newBuilder = newBuilder.required(
                            stringParser.getIdentifier(),
                            stringVariableArgument(stringParser.getIdentifier(), variable),
                            variableArgumentDescription(variableString, stringParser.getIdentifier(), "text"));
                }
            }
            if (variable.getRequiredNumbers() != null) {
                for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                    // Positional (non-greedy): these required numbers (e.g. a Block variable's x/y/z)
                    // are followed by further arguments, so they must not greedily swallow the rest.
                    newBuilder = newBuilder.required(
                            numberParser.getIdentifier(),
                            numberVariableArgument(numberParser.getIdentifier(), variable, false),
                            variableArgumentDescription(variableString, numberParser.getIdentifier(), "number"));
                }
            }
            if (variable.getRequiredBooleans() != null) {
                for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                    newBuilder = newBuilder.required(
                            booleanParser.getIdentifier(),
                            booleanVariableArgument(booleanParser.getIdentifier(), variable, false),
                            variableArgumentDescription(variableString, booleanParser.getIdentifier(), "boolean expression"));
                }
            }
            if (variable.getRequiredBooleanFlags() != null) {
                for (rocks.gravili.notquests.paper.commands.framework.NQFlag commandFlag : variable.getRequiredBooleanFlags()) {
                    final NQDescription description = commandFlag.description().isEmpty()
                            ? NQDescription.of("Optional toggle for the " + variableString + " variable: " + commandFlag.name() + ".")
                            : commandFlag.description();
                    newBuilder = newBuilder.flag(rocks.gravili.notquests.paper.commands.framework.NQFlag.presence(commandFlag.name(), description));
                }
            }
        }
        return newBuilder;
    }

    private NQDescription variableArgumentDescription(
            final String variableString, final String identifier, final String valueKind) {
        final String normalized = identifier.toLowerCase(java.util.Locale.ROOT);
        final String description = switch (normalized) {
            case "entitytype" -> "Entity type counted by the " + variableString + " variable. Use any to count all nearby entities.";
            case "radius" -> "Radius in blocks around the player used by the " + variableString + " variable.";
            case "tagname" -> "Name of the NotQuests tag used by " + variableString + ". Tab-completion only shows tags with the matching value type.";
            case "class path" -> "Fully qualified Java class name that contains the static field read by " + variableString + ".";
            case "field" -> "Name of the static field read from the configured class.";
            case "quest to check", "questname" -> "Quest identifier whose state should be checked by the " + variableString + " variable.";
            case "world" -> "World name used for the location lookup.";
            case "x" -> "X coordinate used for the location lookup.";
            case "y" -> "Y coordinate used for the location lookup.";
            case "z" -> "Z coordinate used for the location lookup.";
            case "statistic" -> "Minecraft statistic key to read for this player, such as MOB_KILLS or JUMP.";
            case "advancement" -> "Minecraft advancement key to check, for example minecraft:story/mine_stone.";
            case "conditions" -> "Name of the condition list to evaluate for this variable.";
            case "itemslot" -> "Inventory slot to inspect. Use the slot names suggested by tab-completion.";
            case "permission" -> "Permission node to check on the target player.";
            case "placeholder" -> "PlaceholderAPI placeholder to resolve for the target player.";
            case "chance" -> "Chance percentage to evaluate, from 0 to 100.";
            case "min" -> "Minimum number in the accepted range.";
            case "max" -> "Maximum number in the accepted range.";
            default -> "Required " + valueKind + " parameter for the " + variableString + " variable.";
        };
        return NQDescription.of(description);
    }

    private NQDescription variableLiteralDescription(final String variableString) {
        return NQDescription.of(variableLiteralDescriptionText(variableString));
    }

    public static String variableLiteralDescriptionText(final String variableString) {
        return switch (variableString) {
            case "FoodLevel" -> "Reads or changes the target player's visible hunger bar from 0 to 20.";
            case "Saturation" -> "Reads or changes the target player's hidden food saturation value.";
            case "DistanceToLocation" -> "Returns the target player's distance in blocks from a fixed world location.";
            case "NearbyEntityCount" -> "Counts entities near the target player inside the configured radius.";
            case "Weather" -> "Reads or changes the weather in the target player's current world.";
            default -> "Selects the " + variableString + " variable for this action, condition, objective, or variable check.";
        };
    }

    public void registerVariable(
            final String identifier, final Class<? extends Variable<?>> variable) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering variable <highlight>" + identifier);
        }
        variables.put(identifier, variable);

    /*if(main.getActionManager() != null){
        main.getActionManager().updateVariableActions();
    }*/
        if (!main.getDataManager().isCurrentlyLoading()) {
            if (main.getConditionsManager() != null) {
                main.getConditionsManager().updateVariableConditions();
            }
            if (main.getActionManager() != null) {
                main.getActionManager().updateVariableActions();
            }
            if (main.getObjectiveManager() != null) {
                main.getObjectiveManager().updateVariableObjectives();
            }
            alreadyFullRegisteredVariables.add(identifier);
        }

    /*try {
        Method commandHandler = Variable.getMethod("handleCommands", main.getClass(), NQCommandManager.class, NQCommandBuilder.class, VariableFor.class);
        commandHandler.invoke(Variable, main, main.getCommandManager().getNQCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder(), VariableFor.QUEST);
        commandHandler.invoke(Variable, main, main.getCommandManager().getNQCommandManager(), main.getCommandManager().getAdminEditObjectiveAddVariableCommandBuilder(), VariableFor.OBJECTIVE);
        commandHandler.invoke(Variable, main, main.getCommandManager().getNQCommandManager(), main.getCommandManager().getAdminAddVariableCommandBuilder(), VariableFor.variablesYML); //For Actions.yml
        commandHandler.invoke(Variable, main, main.getCommandManager().getNQCommandManager(), main.getCommandManager().getAdminEditActionsAddVariableCommandBuilder(), VariableFor.Action); //For Actions.yml
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        e.printStackTrace();
    }*/
    }

    public final Class<? extends Variable<?>> getVariableClass(final String type) {
        return variables.get(type);
    }

    public final String getVariableType(final Class<? extends Variable> variable) {
        for (final String VariableType : variables.keySet()) {
            if (variables.get(VariableType).equals(variable)) {
                return VariableType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Variable<?>>> getVariablesAndIdentifiers() {
        return variables;
    }

    public final Collection<Class<? extends Variable<?>>> getVariables() {
        return variables.values();
    }

    public final Collection<String> getVariableIdentifiers() {
        return variables.keySet();
    }

    public void addVariable(Variable<?> Variable, NQCommandContext context) {
    }

    public final Variable<?> getVariableFromString(final String variableString) {
        Class<? extends Variable<?>> variableClass = getVariableClass(variableString);
        try {
            return variableClass.getDeclaredConstructor(NotQuests.class).newInstance(main);
        } catch (Exception e) {
            return null;
        }
    }
}
