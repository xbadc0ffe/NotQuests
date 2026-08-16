package com.notquests.paper.objectives;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQCommandBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import com.notquests.paper.NotQuests;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.ObjectiveType;
import com.notquests.paper.objectives.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import com.notquests.paper.builtin.objectives.BetonQuestObjectiveStateChange;
import com.notquests.paper.builtin.objectives.BreedMobs;
import com.notquests.paper.builtin.objectives.BrewItems;
import com.notquests.paper.builtin.objectives.BreakBlocks;
import com.notquests.paper.builtin.objectives.ConditionWatch;
import com.notquests.paper.builtin.objectives.ConsumeItems;
import com.notquests.paper.builtin.objectives.CraftItems;
import com.notquests.paper.builtin.objectives.DeliverItems;
import com.notquests.paper.builtin.objectives.Die;
import com.notquests.paper.builtin.objectives.Enchant;
import com.notquests.paper.builtin.objectives.EscortNPC;
import com.notquests.paper.builtin.objectives.FeedMobs;
import com.notquests.paper.builtin.objectives.FishItems;
import com.notquests.paper.builtin.objectives.Harvest;
import com.notquests.paper.builtin.objectives.Interact;
import com.notquests.paper.builtin.objectives.Jump;
import com.notquests.paper.builtin.objectives.JobsRebornReachJobLevel;
import com.notquests.paper.builtin.objectives.KillEliteMobs;
import com.notquests.paper.builtin.objectives.KillMobs;
import com.notquests.paper.builtin.objectives.MilkCow;
import com.notquests.paper.builtin.objectives.NumberVariable;
import com.notquests.paper.builtin.objectives.ObjectiveGroup;
import com.notquests.paper.builtin.objectives.OpenBuriedTreasure;
import com.notquests.paper.builtin.objectives.OtherQuest;
import com.notquests.paper.builtin.objectives.PickupItems;
import com.notquests.paper.builtin.objectives.PlaceBlocks;
import com.notquests.paper.builtin.objectives.ReachLocation;
import com.notquests.paper.builtin.objectives.RunCommand;
import com.notquests.paper.builtin.objectives.ShearSheep;
import com.notquests.paper.builtin.objectives.ShootArrow;
import com.notquests.paper.builtin.objectives.SmeltItems;
import com.notquests.paper.builtin.objectives.SmithItems;
import com.notquests.paper.builtin.objectives.Sneak;
import com.notquests.paper.builtin.objectives.SlimefunResearch;
import com.notquests.paper.builtin.objectives.TameMobs;
import com.notquests.paper.builtin.objectives.TalkToNPC;
import com.notquests.paper.builtin.objectives.TownyNationReachTownCount;
import com.notquests.paper.builtin.objectives.TownyReachResidentCount;
import com.notquests.paper.builtin.objectives.TradeWithVillager;
import com.notquests.paper.builtin.objectives.TriggerCommand;

import static com.notquests.paper.commands.arguments.ObjectiveArgument.objectiveArgument;

public class ObjectiveCatalog {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Objective>> objectives;
    private final HashMap<String, ObjectiveType> objectiveTypes;

    public ObjectiveCatalog(final NotQuests main) {
        this.main = main;
        objectives = new HashMap<>();
        objectiveTypes = new HashMap<>();

        registerDefaultObjectives();
    }

    public void registerDefaultObjectives() {
        main.getLogManager().info("Registering objectives...");

        objectives.clear();
        objectiveTypes.clear();
        ConditionWatch.register(main, this);
        BreakBlocks.register(main, this);
        PlaceBlocks.register(main, this);
        Harvest.register(main, this);
        PickupItems.register(main, this);
        FishItems.register(main, this);

        TriggerCommand.register(main, this);
        OtherQuest.register(main, this);
        KillMobs.register(main, this);
        ConsumeItems.register(main, this);
        DeliverItems.register(main, this);
        TradeWithVillager.register(main, this);
        TalkToNPC.register(main, this);
        EscortNPC.register(main, this);
        CraftItems.register(main, this);
        Enchant.register(main, this);

        KillEliteMobs.register(main, this);
        ReachLocation.register(main, this);
        BreedMobs.register(main, this);
        FeedMobs.register(main, this);
        TameMobs.register(main, this);
        SlimefunResearch.register(main, this);
        RunCommand.register(main, this);
        Interact.register(main, this);
        ShootArrow.register(main, this);
        Jump.register(main, this);
        Sneak.register(main, this);
        Die.register(main, this);
        SmeltItems.register(main, this);
        BrewItems.register(main, this);
        SmithItems.register(main, this);
        OpenBuriedTreasure.register(main, this);
        ShearSheep.register(main, this);
        MilkCow.register(main, this);
        ObjectiveGroup.register(main, this);

        NumberVariable.register(main, this);

        // Towny
        TownyReachResidentCount.register(main, this);
        TownyNationReachTownCount.register(main, this);

        // Jobs
        JobsRebornReachJobLevel.register(main, this);

        if (main.getIntegrationsManager().isBetonQuestEnabled()) {
            BetonQuestObjectiveStateChange.register(main, this);
        }

        // registerObjectiveCommandCompletionHandler("KillMobs", this::eee);
    }

    public ObjectiveType.Builder objective(final String identifier) {
        return new ObjectiveType.Builder(main, this, identifier);
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

    public final Class<? extends Objective> getObjectiveClass(@NotNull final String type) {
        return objectiveTypes.containsKey(type) ? DefinedObjective.class : null;
    }

    public final Objective createObjective(@NotNull final String type) {
        final ObjectiveType definition = objectiveTypes.get(type);
        return definition == null ? null : definition.createObjective();
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
        main.getLogManager().info("Re-registering NumberVariable objective commands due to variable changes...");
        NumberVariable.register(main, this);
    }
}
