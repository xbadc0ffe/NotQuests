package com.notquests.paper.variables;
import com.notquests.paper.commands.framework.NQCommandContext;
import static com.notquests.paper.commands.arguments.variables.BooleanVariableArgument.booleanVariableArgument;
import static com.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;
import static com.notquests.paper.commands.arguments.variables.StringVariableArgument.stringVariableArgument;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQCommandBuilder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import com.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.registry.VariableType;
import com.notquests.paper.builtin.variables.*;
import com.notquests.paper.variables.*;
import com.notquests.paper.builtin.variables.hooks.*;
import com.notquests.paper.builtin.variables.reflection.*;
import com.notquests.paper.builtin.variables.tags.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;


public class VariableCatalog {
    private final NotQuests main;

    private final HashMap<String, VariableType> variableTypes;
    public ArrayList<String> alreadyFullRegisteredVariables = new ArrayList<>();

    public VariableCatalog(final NotQuests main) {
        this.main = main;
        variableTypes = new HashMap<>();

        registerDefaultVariables();
    }

    public void registerDefaultVariables() {
        main.getLogManager().info("Registering variables...");

        variableTypes.clear();
        registerVariable("True", TrueVariable::new, "Always returns true.");
        registerVariable("False", FalseVariable::new, "Always returns false.");
        registerVariable("Condition", ConditionVariable::new, "Evaluates a saved NotQuests condition list.");

        registerVariable("QuestPoints", QuestPointsVariable::new, "Reads or changes the target player's quest points.");
        registerVariable("Money", MoneyVariable::new, "Reads or changes the target player's Vault economy balance.");
        registerVariable("ActiveQuests", ActiveQuestsVariable::new, "Reads or changes the quests currently active for the target player.");
        registerVariable("CompletedQuests", CompletedQuestsVariable::new, "Reads or changes the quests completed by the target player.");
        registerVariable(
                "CompletedObjectiveIDsOfQuest",
                CompletedObjectiveIDsOfQuestVariable::new,
                "Reads or changes the completed objective IDs for one quest on the target player.");
        registerVariable("Permission", PermissionVariable::new, "Checks or changes whether the target player has a permission node.");
        registerVariable("Statistic", PlayerStatisticVariable::new, "Reads or changes one Bukkit statistic for the target player.");
        registerVariable("Name", PlayerNameVariable::new, "Reads or changes the target player's display name.");
        registerVariable("Experience", PlayerExperienceVariable::new, "Reads or changes the target player's raw experience progress.");
        registerVariable("ExperienceLevel", PlayerExperienceLevelVariable::new, "Reads or changes the target player's experience level.");
        registerVariable("FoodLevel", PlayerFoodLevelVariable::new, "Reads or changes the target player's hunger bar value from 0 to 20.");
        registerVariable("Saturation", PlayerSaturationVariable::new, "Reads or changes the target player's hidden saturation value.");
        registerVariable("CurrentWorld", PlayerCurrentWorldVariable::new, "Reads or changes the world the target player is currently in.");
        registerVariable("CurrentPositionX", PlayerCurrentPositionXVariable::new, "Reads or changes the target player's current X coordinate.");
        registerVariable("CurrentPositionY", PlayerCurrentPositionYVariable::new, "Reads or changes the target player's current Y coordinate.");
        registerVariable("CurrentPositionZ", PlayerCurrentPositionZVariable::new, "Reads or changes the target player's current Z coordinate.");
        registerVariable("DistanceToLocation", DistanceToLocationVariable::new, "Measures the target player's distance from a configured location.");
        registerVariable("NearbyEntityCount", NearbyEntityCountVariable::new, "Counts nearby entities around the target player by entity type and radius.");
        registerVariable("Weather", WeatherVariable::new, "Reads or changes the weather in the target player's current world.");
        registerVariable("RandomNumberBetweenRange", RandomNumberBetweenRangeVariable::new, "Returns a random whole number between the configured minimum and maximum.");
        registerVariable("PlaytimeTicks", PlayerPlaytimeTicksVariable::new, "Reads the target player's playtime in server ticks.");
        registerVariable("PlaytimeMinutes", PlayerPlaytimeMinutesVariable::new, "Reads the target player's playtime in minutes.");
        registerVariable("PlaytimeHours", PlayerPlaytimeHoursVariable::new, "Reads the target player's playtime in hours.");

        registerVariable("Glowing", PlayerGlowingVariable::new, "Reads or changes whether the target player is glowing.");
        registerVariable("Op", PlayerOpVariable::new, "Reads or changes whether the target player is a server operator.");
        registerVariable("Climbing", PlayerClimbingVariable::new, "Checks whether the target player is currently climbing.");
        registerVariable("InLava", PlayerInLavaVariable::new, "Checks whether the target player is currently in lava.");
        registerVariable("InWater", PlayerInWaterVariable::new, "Checks whether the target player is currently in water.");
        registerVariable("Ping", PlayerPingVariable::new, "Reads the target player's current network ping in milliseconds.");
        registerVariable("WalkSpeed", PlayerWalkSpeedVariable::new, "Reads or changes the target player's walk speed.");
        registerVariable("FlySpeed", PlayerFlySpeedVariable::new, "Reads or changes the target player's fly speed.");


        registerVariable("Sleeping", PlayerSleepingVariable::new, "Checks whether the target player is currently sleeping.");
        registerVariable("Sneaking", PlayerSneakingVariable::new, "Reads or changes whether the target player is sneaking.");
        registerVariable("Sprinting", PlayerSprintingVariable::new, "Reads or changes whether the target player is sprinting.");
        registerVariable("Swimming", PlayerSwimmingVariable::new, "Reads or changes whether the target player is swimming.");
        registerVariable("Health", PlayerHealthVariable::new, "Reads or changes the target player's current health.");
        registerVariable("MaxHealth", PlayerMaxHealthVariable::new, "Reads or changes the target player's maximum health.");
        registerVariable("GameMode", PlayerGameModeVariable::new, "Reads or changes the target player's game mode.");
        registerVariable("Flying", PlayerFlyingVariable::new, "Reads or changes whether the target player is flying.");
        registerVariable("DayOfWeek", DayOfWeekVariable::new, "Returns the current day of week as text.");
        registerVariable("CurrentBiome", PlayerCurrentBiomeVariable::new, "Reads the biome at the target player's current location.");

        registerVariable("Chance", ChanceVariable::new, "Randomly returns true based on the configured percent chance.");
        registerVariable("Advancement", AdvancementVariable::new, "Checks or changes whether the target player has a Minecraft advancement.");
        registerVariable("Inventory", InventoryVariable::new, "Reads or changes the target player's inventory contents.");
        registerVariable("EnderChest", EnderChestVariable::new, "Reads or changes the target player's ender chest contents.");

        registerVariable("ContainerInventory", ContainerInventoryVariable::new, "Reads or changes the inventory of a container block at a configured location.");
        registerVariable("Block", BlockVariable::new, "Reads or changes the material of the block at a configured location.");

        registerVariable("TagBoolean", BooleanTagVariable::new, "Reads or changes a boolean NotQuests tag on the target player.");
        registerVariable("TagInteger", IntegerTagVariable::new, "Reads or changes an integer NotQuests tag on the target player.");
        registerVariable("TagFloat", FloatTagVariable::new, "Reads or changes a float NotQuests tag on the target player.");
        registerVariable("TagDouble", DoubleTagVariable::new, "Reads or changes a double NotQuests tag on the target player.");
        registerVariable("TagString", StringTagVariable::new, "Reads or changes a text NotQuests tag on the target player.");
        registerVariable("QuestOnCooldown", QuestOnCooldownVariable::new, "Checks whether a quest is currently on cooldown for the target player.");
        registerVariable("QuestAbleToAccept", QuestAbleToAcceptVariable::new, "Checks whether the target player can accept a quest right now.");
        registerVariable("QuestReachedMaxAccepts", QuestReachedMaxAcceptsVariable::new, "Checks whether the target player reached a quest's accept limit.");
        registerVariable("QuestReachedMaxCompletions", QuestReachedMaxCompletionsVariable::new, "Checks whether the target player reached a quest's completion limit.");
        registerVariable("QuestReachedMaxFails", QuestReachedMaxFailsVariable::new, "Checks whether the target player reached a quest's fail limit.");

        registerVariable("ItemInInventoryEnchantments", ItemInInventoryEnchantmentsVariable::new, "Lists enchantments on an item in one target-player inventory slot.");

        registerVariable("ReflectionStaticDouble", ReflectionStaticDoubleVariable::new, "Reads or changes a static Java double field by reflection.");
        registerVariable("ReflectionStaticFloat", ReflectionStaticFloatVariable::new, "Reads or changes a static Java float field by reflection.");
        registerVariable("ReflectionStaticInteger", ReflectionStaticIntegerVariable::new, "Reads or changes a static Java integer field by reflection.");
        registerVariable("ReflectionStaticBoolean", ReflectionStaticBooleanVariable::new, "Reads or changes a static Java boolean field by reflection.");
        registerVariable("ReflectionStaticString", ReflectionStaticStringVariable::new, "Reads or changes a static Java text field by reflection.");


        if (main.getIntegrationsManager().isPlaceholderAPIEnabled()) {
            registerVariable("PlaceholderAPINumber", PlaceholderAPINumberVariable::new, "Reads a PlaceholderAPI placeholder as a number for the target player.");
            registerVariable("PlaceholderAPIString", PlaceholderAPIStringVariable::new, "Reads a PlaceholderAPI placeholder as text for the target player.");
        }
        if (main.getIntegrationsManager().isTownyEnabled()) {
            registerVariable("TownyNationTownCount", TownyNationTownCountVariable::new, "Reads the number of towns in the target player's Towny nation.");
            registerVariable("TownyTownResidentCount", TownyTownResidentCountVariable::new, "Reads the number of residents in the target player's Towny town.");
            registerVariable("TownyTownPlotCount", TownyTownPlotCountVariable::new, "Reads the number of plots owned by the target player's Towny town.");
            registerVariable("TownyNationName", TownyNationNameVariable::new, "Reads the name of the target player's Towny nation.");
        }

        if (main.getIntegrationsManager().isFloodgateEnabled()) {
            registerVariable("FloodgateIsFloodgatePlayer", FloodgateIsFloodgatePlayerVariable::new, "Checks whether the target player joined through Floodgate.");
        }
        if (main.getIntegrationsManager().isBetonQuestEnabled()) {
            registerVariable("BetonQuestCondition", BetonQuestConditionVariable::new, "Evaluates a BetonQuest condition for the target player.");
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


            final com.notquests.paper.commands.framework.NQFlag playerSelectorCommandFlag =
                    com.notquests.paper.commands.framework.NQFlag.builder(
                                    "player",
                                    NQDescription.of("Player whose current variable value should be checked; defaults to the command sender when possible."))
                            .withArgument(com.notquests.paper.commands.framework.NQArguments.playerArgument())
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
                        for (final com.notquests.paper.commands.framework.NQFlag commandFlag : variable.getRequiredBooleanFlags()) {
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
                            variableArgumentDescription(variableString, stringParser.getIdentifier()));
                }
            }
            if (variable.getRequiredNumbers() != null) {
                for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                    // Positional (non-greedy): these required numbers (e.g. a Block variable's x/y/z)
                    // are followed by further arguments, so they must not greedily swallow the rest.
                    newBuilder = newBuilder.required(
                            numberParser.getIdentifier(),
                            numberVariableArgument(numberParser.getIdentifier(), variable, false),
                            variableArgumentDescription(variableString, numberParser.getIdentifier()));
                }
            }
            if (variable.getRequiredBooleans() != null) {
                for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                    newBuilder = newBuilder.required(
                            booleanParser.getIdentifier(),
                            booleanVariableArgument(booleanParser.getIdentifier(), variable, false),
                            variableArgumentDescription(variableString, booleanParser.getIdentifier()));
                }
            }
            if (variable.getRequiredBooleanFlags() != null) {
                for (com.notquests.paper.commands.framework.NQFlag commandFlag : variable.getRequiredBooleanFlags()) {
                    final NQDescription description = commandFlag.description().isEmpty()
                            ? NQDescription.of("Optional toggle for the " + variableString + " variable: " + commandFlag.name() + ".")
                            : commandFlag.description();
                    newBuilder = newBuilder.flag(com.notquests.paper.commands.framework.NQFlag.presence(commandFlag.name(), description));
                }
            }
        }
        return newBuilder;
    }

    private NQDescription variableArgumentDescription(
            final String variableString, final String identifier) {
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
            case "package" -> "BetonQuest package that contains the condition to check.";
            case "condition" -> "BetonQuest condition name to check inside the selected package.";
            default -> throw new IllegalStateException(
                    "Missing variable argument description for "
                            + variableString
                            + " argument "
                            + identifier);
        };
        return NQDescription.of(description);
    }

    private NQDescription variableLiteralDescription(final String variableString) {
        final VariableType variableType = getVariableTypeDefinition(variableString);
        if (variableType == null) {
            throw new IllegalStateException("Variable " + variableString + " is not registered.");
        }
        return NQDescription.of(variableType.description());
    }

    public void registerVariable(
            final String identifier,
            final VariableType.VariableFactory factory,
            final String description) {
        registerVariable(new VariableType(main, identifier, identifier, description, factory));
    }

    public void registerVariable(final VariableType variableType) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering variable <highlight>" + variableType.id());
        }
        variableTypes.put(variableType.id(), variableType);

        if (!main.getDataManager().isCurrentlyLoading()) {
            if (main.getConditionCatalog() != null) {
                main.getConditionCatalog().updateVariableConditions();
            }
            if (main.getActionCatalog() != null) {
                main.getActionCatalog().updateVariableActions();
            }
            if (main.getObjectiveCatalog() != null) {
                main.getObjectiveCatalog().updateVariableObjectives();
            }
            alreadyFullRegisteredVariables.add(variableType.id());
        }
    }

    public final VariableType getVariableTypeDefinition(final String type) {
        return variableTypes.get(type);
    }

    public final String getVariableType(final Variable<?> variable) {
        for (final VariableType variableType : variableTypes.values()) {
            if (variableType.matches(variable)) {
                return variableType.id();
            }
        }
        return null;
    }

    public final HashMap<String, VariableType> getVariableTypesAndIdentifiers() {
        return variableTypes;
    }

    public final Collection<VariableType> getVariables() {
        return variableTypes.values();
    }

    public final Collection<String> getVariableIdentifiers() {
        return variableTypes.keySet();
    }

    public void addVariable(Variable<?> Variable, NQCommandContext context) {
    }

    public final Variable<?> getVariableFromString(final String variableString) {
        final VariableType variableType = getVariableTypeDefinition(variableString);
        return variableType == null ? null : variableType.createVariable();
    }
}
