package rocks.gravili.notquests.paper.managers.registering;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.registry.DefinedObjective;
import rocks.gravili.notquests.paper.registry.ObjectiveType;
import rocks.gravili.notquests.paper.structs.objectives.*;
import rocks.gravili.notquests.paper.structs.objectives.hooks.betonquest.BetonQuestObjectiveStateChangeObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.citizens.EscortNPCObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.elitemobs.KillEliteMobsObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.jobsreborn.JobsRebornReachJobLevelObjective;

import rocks.gravili.notquests.paper.structs.objectives.hooks.slimefun.SlimefunResearchObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyNationReachTownCountObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyReachResidentCountObjective;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import rocks.gravili.notquests.paper.objectives.BreakBlocks;

import static rocks.gravili.notquests.paper.commands.arguments.ObjectiveArgument.objectiveArgument;

public class ObjectiveManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Objective>> objectives;
    private final HashMap<String, ObjectiveType> objectiveTypes;

    public ObjectiveManager(final NotQuests main) {
        this.main = main;
        objectives = new HashMap<>();
        objectiveTypes = new HashMap<>();

        registerDefaultObjectives();
    }

    public void registerDefaultObjectives() {
        main.getLogManager().info("Registering objectives...");

        objectives.clear();
        objectiveTypes.clear();
        registerObjective("Condition", ConditionObjective.class);
        BreakBlocks.register(main, this);
        registerObjective("PlaceBlocks", PlaceBlocksObjective.class);
        registerObjective("Harvest", HarvestObjective.class);
        registerObjective("PickupItems", PickupItemsObjective.class);
        registerObjective("FishItems", FishItemsObjective.class);

        registerObjective("TriggerCommand", TriggerCommandObjective.class);
        registerObjective("OtherQuest", OtherQuestObjective.class);
        registerObjective("KillMobs", KillMobsObjective.class);
        registerObjective("ConsumeItems", ConsumeItemsObjective.class);
        registerObjective("DeliverItems", DeliverItemsObjective.class);
        registerObjective("TradeWithVillager", TradeWithVillagerObjective.class);
        registerObjective("TalkToNPC", TalkToNPCObjective.class);
        registerObjective("EscortNPC", EscortNPCObjective.class);
        registerObjective("CraftItems", CraftItemsObjective.class);
        registerObjective("Enchant", EnchantObjective.class);

        registerObjective(
                "KillEliteMobs", KillEliteMobsObjective.class); // TODO: only if EliteMobs enabled?
        registerObjective("ReachLocation", ReachLocationObjective.class);
        registerObjective("BreedMobs", BreedObjective.class);
        registerObjective("FeedMobs", FeedMobsObjective.class);
        registerObjective("TameMobs", TameMobsObjective.class);
        registerObjective("SlimefunResearch", SlimefunResearchObjective.class);
        registerObjective("RunCommand", RunCommandObjective.class);
        registerObjective("Interact", InteractObjective.class);
        registerObjective("ShootArrow", ShootArrowObjective.class);
        registerObjective("Jump", JumpObjective.class);
        registerObjective("Sneak", SneakObjective.class);
        registerObjective("Die", DieObjective.class);
        registerObjective("SmeltItems", SmeltObjective.class);
        registerObjective("BrewItems", BrewItemsObjective.class);
        registerObjective("SmithItems", SmithItemsObjective.class);
        registerObjective("OpenBuriedTreasure", OpenBuriedTreasureObjective.class);
        registerObjective("ShearSheep", ShearSheepObjective.class);
        registerObjective("MilkCow", MilkCowObjective.class);
        registerObjective("Objective", ObjectiveObjective.class);

        registerObjective("NumberVariable", NumberVariableObjective.class); //Special

        // Towny
        registerObjective("TownyReachResidentCount", TownyReachResidentCountObjective.class);
        registerObjective("TownyNationReachTownCount", TownyNationReachTownCountObjective.class);

        // Jobs
        registerObjective("JobsRebornReachJobLevel", JobsRebornReachJobLevelObjective.class);

        if (main.getIntegrationsManager().isBetonQuestEnabled()) {
            registerObjective("BetonQuestObjectiveStateChange", BetonQuestObjectiveStateChangeObjective.class);
        }

        // registerObjectiveCommandCompletionHandler("KillMobs", this::eee);
    }

    public ObjectiveType.Builder objective(final String identifier) {
        return new ObjectiveType.Builder(main, this, identifier);
    }

  /* public void registerObjectiveCommandCompletionHandler(final String identifier, final String commandCompletionHandler){
      main.getLogManager().info("Registering command completions for objective <highlight>" + identifier);
      objectiveCommandCompletionHandlers.put(identifier, commandCompletionHandler);

  }*/

    public void registerObjective(
            final String identifier, final Class<? extends Objective> objective) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering objective <highlight>" + identifier);
        }
        objectives.put(identifier, objective);

        try {
            final Method commandHandler = objective.getMethod("handleCommands", main.getClass(), NQCommandManager.class, NQCommandBuilder.class, int.class);
            final NQDescription typeDescription = NQDescription.of(objectiveLiteralDescription(identifier));

            //Level 0
            final NQCommandBuilder objectivesBuilder = main.getCommandManager().getAdminEditCommandBuilder().literal("objectives", NQDescription.of("Manages objectives on the selected quest."), "o");
            final NQCommandBuilder adminEditAddObjectiveCommandBuilder =
                    objectivesBuilder.literal("add", NQDescription.of("Adds a new objective to the selected quest."));

            commandHandler.invoke(objective, main, main.getCommandManager().getNQCommandManager(), adminEditAddObjectiveCommandBuilder
                    .literal(identifier, typeDescription)
                    .flag(main.getCommandManager().taskDescription), 0);

            //Level 1
            final String objectiveIDIdentifier = "objectiveId";
            final NQCommandBuilder objectivesBuilderLevel1 = objectivesBuilder.literal("edit", NQDescription.of("Opens subcommands for editing a specific objective on the selected quest.")).required(
                    objectiveIDIdentifier,
                    objectiveArgument(main, 0),
                    NQDescription.of("Objective ID shown by this quest's objectives list."));


            final NQCommandBuilder adminEditAddObjectiveCommandBuilderLevel1 =
                    objectivesBuilderLevel1.literal("objectives", NQDescription.of("Manages child objectives inside the selected objective."), "o").literal("add", NQDescription.of("Adds a child objective to the selected objective."));

            //Level 1
            commandHandler.invoke(objective, main, main.getCommandManager().getNQCommandManager(), adminEditAddObjectiveCommandBuilderLevel1
                    .literal(identifier, typeDescription)
                    .flag(main.getCommandManager().taskDescription), 1);


            final NQCommandBuilder objectivesBuilder2 = objectivesBuilderLevel1.literal("objectives", NQDescription.of("Manages child objectives inside the selected objective."), "");
            final String objectiveIDIdentifier2 = "objectiveId2";
            final int level2 = 2;
            final NQCommandBuilder objectivesBuilderLevel2 = objectivesBuilder2
                    .literal("edit", NQDescription.of("Opens subcommands for editing a specific child objective."))
                    .required(objectiveIDIdentifier2, objectiveArgument(main, 1),
                            NQDescription.of("Child objective ID shown inside the selected parent objective."));


            final NQCommandBuilder adminEditAddObjectiveCommandBuilderLevel2 =
                    objectivesBuilderLevel2.literal("objectives", NQDescription.of("Manages child objectives inside the selected nested objective."), "o").literal("add", NQDescription.of("Adds a child objective to the selected nested objective."));

            //Level 2
            commandHandler.invoke(
                    objective,
                    main,
                    main.getCommandManager().getNQCommandManager(),
                    adminEditAddObjectiveCommandBuilderLevel2
                            .literal(identifier, typeDescription)
                            .flag(main.getCommandManager().taskDescription), 2);

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void registerObjective(final ObjectiveType definition) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering objective <highlight>" + definition.id());
        }
        objectives.put(definition.id(), DefinedObjective.class);
        objectiveTypes.put(definition.id(), definition);

        final NQCommandBuilder objectivesBuilder = main.getCommandManager()
                .getAdminEditCommandBuilder()
                .literal("objectives", NQDescription.of("Manages objectives on the selected quest."), "o");
        final NQCommandBuilder adminEditAddObjectiveCommandBuilder =
                objectivesBuilder.literal("add", NQDescription.of("Adds a new objective to the selected quest."));

        definition.registerCommands(adminEditAddObjectiveCommandBuilder
                .literal(definition.id(), NQDescription.of(definition.description()))
                .flag(main.getCommandManager().taskDescription), 0);

        final String objectiveIDIdentifier = "objectiveId";
        final NQCommandBuilder objectivesBuilderLevel1 = objectivesBuilder
                .literal("edit", NQDescription.of("Opens subcommands for editing a specific objective on the selected quest."))
                .required(
                        objectiveIDIdentifier,
                        objectiveArgument(main, 0),
                        NQDescription.of("Objective ID shown by this quest's objectives list."));
        definition.registerCommands(
                objectivesBuilderLevel1
                        .literal("objectives", NQDescription.of("Manages child objectives inside the selected objective."), "o")
                        .literal("add", NQDescription.of("Adds a child objective to the selected objective."))
                        .literal(definition.id(), NQDescription.of(definition.description()))
                        .flag(main.getCommandManager().taskDescription),
                1);

        final NQCommandBuilder objectivesBuilder2 = objectivesBuilderLevel1
                .literal("objectives", NQDescription.of("Manages child objectives inside the selected objective."), "");
        definition.registerCommands(
                objectivesBuilder2
                        .literal("edit", NQDescription.of("Opens subcommands for editing a specific child objective."))
                        .required("objectiveId2", objectiveArgument(main, 1),
                                NQDescription.of("Child objective ID shown inside the selected parent objective."))
                        .literal("objectives", NQDescription.of("Manages child objectives inside the selected nested objective."), "o")
                        .literal("add", NQDescription.of("Adds a child objective to the selected nested objective."))
                        .literal(definition.id(), NQDescription.of(definition.description()))
                        .flag(main.getCommandManager().taskDescription),
                2);

        definition.registerEventListeners();
    }

    public static String objectiveLiteralDescription(final String identifier) {
        return switch (identifier) {
            case "TradeWithVillager" -> "Counts matching result items taken from villager trade windows.";
            case "TameMobs" -> "Counts mobs tamed by the player.";
            case "SmithItems" -> "Counts matching result items taken from a smithing table.";
            case "Die" -> "Counts player deaths, optionally filtered by damage cause.";
            case "ShootArrow" -> "Counts arrows shot by the player that land inside a target region.";
            case "Interact" -> "Counts player left-clicks or right-clicks on a configured block or location.";
            default -> "Creates a new " + identifier + " objective.";
        };
    }

    public final Class<? extends Objective> getObjectiveClass(@NotNull final String type) {
        return objectives.get(type);
    }

    public final Objective createObjective(@NotNull final String type)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final ObjectiveType definition = objectiveTypes.get(type);
        if (definition != null) {
            return definition.createObjective();
        }
        final Class<? extends Objective> objective = getObjectiveClass(type);
        return objective == null ? null : objective.getDeclaredConstructor(NotQuests.class).newInstance(main);
    }

    public final String getObjectiveType(final Class<? extends Objective> objective) {
        for (final String objectiveType : objectives.keySet()) {
            if (objectives.get(objectiveType).equals(objective)) {
                return objectiveType;
            }
        }
        return null;
    }

    public final String getObjectiveType(final Objective objective) {
        if (objective instanceof final DefinedObjective definedObjective) {
            return definedObjective.definition().id();
        }
        return getObjectiveType(objective.getClass());
    }

    public final boolean objectiveMatchesType(final Objective objective, final String type) {
        return type != null && type.equals(getObjectiveType(objective));
    }

    public final HashMap<String, Class<? extends Objective>> getObjectivesAndIdentifiers() {
        return objectives;
    }

    public final HashMap<String, ObjectiveType> getObjectiveTypesAndIdentifiers() {
        return objectiveTypes;
    }

    public final Collection<Class<? extends Objective>> getObjectives() {
        return objectives.values();
    }

    public final Collection<String> getObjectiveIdentifiers() {
        return objectives.keySet();
    }

    public void addObjective(Objective objective, NQCommandContext context, int level) {


        final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);
        final Optional<Component> _taskDescription = context.flags().getValue(main.getCommandManager().taskDescription);
        objective.setObjectiveHolder(objectiveHolder);
        objective.setObjectiveID(objectiveHolder.getFreeObjectiveID());
        if(_taskDescription.isPresent()){
            String taskDescription = MiniMessage.builder().build().serialize(_taskDescription.get());
            if (!taskDescription.isBlank()) {
                objective.setTaskDescription(taskDescription, true);
            }
        }
        context.sender().sendMessage(main.parse("<success>" + getObjectiveType(objective) + " Objective successfully added to Quest <highlight>" + objectiveHolder.getIdentifier() + "</highlight>!"));
        objectiveHolder.addObjective(objective, true);
    }

    public void updateVariableObjectives() {
        try {
            for (final java.util.Map.Entry<String, Class<? extends Objective>> entry : getObjectivesAndIdentifiers().entrySet()) {
                final String identifier = entry.getKey();
                if (objectiveTypes.containsKey(identifier)) {
                    continue;
                }
                final Class<? extends Objective> objective = entry.getValue();

                final Method commandHandler =
                        objective.getMethod(
                                "handleCommands",
                                main.getClass(),
                                NQCommandManager.class,
                                NQCommandBuilder.class);
                if (objective == NumberVariableObjective.class) {

                    main.getLogManager()
                            .info("Re-registering objective " + identifier + " due to variable changes...");

                    commandHandler.invoke(
                            objective,
                            main,
                            main.getCommandManager().getNQCommandManager(),
                            main.getCommandManager()
                                    .getAdminEditAddObjectiveCommandBuilder()
                                    .literal(identifier, NQDescription.of("Creates a new " + identifier + " objective")));

                    //TODO Check if right? Why action stuff?
                    //TODO: Maybe remove everything below? Why is that there?
                    //TODO: I removed it for now.
          /*
          commandHandler.invoke(
              objective,
              main,
              main.getCommandManager().getNQCommandManager(),
              main.getCommandManager()
                  .getAdminEditAddRewardCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
          );
          commandHandler.invoke(
              objective,
              main,
              main.getCommandManager().getNQCommandManager(),
              main.getCommandManager()
                  .getAdminEditObjectiveAddRewardCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
          );
          commandHandler.invoke(
              objective,
              main,
              main.getCommandManager().getNQCommandManager(),
              main.getCommandManager()
                  .getAdminAddActionCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
                  .flag(main.getCommandManager().categoryFlag)
          ); // For Actions.yml*/
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
