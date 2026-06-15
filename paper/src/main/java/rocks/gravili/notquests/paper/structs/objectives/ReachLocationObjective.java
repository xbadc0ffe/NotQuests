package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReachLocationObjective extends Objective {
    private Location min, max;
    private String locationName;

    public ReachLocationObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(ObjectiveRegionCommandPart
                .centerRadius(
                        addObjectiveBuilder,
                        "reach-location region",
                        "Radius in blocks around the center that counts as reaching this location.")
                .required("Location Name", NQArguments.greedyStringArgument(), NQDescription.of("Name shown to players for this location in objective task text."), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("<Enter new Location name>");
                    return completions;
                })
                .handler((context) -> {
                    final String locationName = context.get("Location Name");
                    final ObjectiveRegion region = ObjectiveRegionCommandPart.centerRadius(context).asRegion();
                    addReachLocationObjective(main, context, level, locationName, region);
                }));

        if (main.getIntegrationsManager().isWorldEditEnabled()) {
            manager.command(addObjectiveBuilder
                    .literal(
                            ObjectiveRegionCommandPart.WORLD_EDIT_SELECTION,
                            NQDescription.of("Uses your current WorldEdit selection as the target region."))
                    .required("Location Name", NQArguments.greedyStringArgument(), NQDescription.of("Name shown to players for this location in objective task text."), (context, input) -> {
                        List<String> completions = new ArrayList<>();
                        completions.add("<Enter new Location name>");
                        return completions;
                    })
                    .handler((context) -> {
                        if (!(context.sender() instanceof final Player player)) {
                            context.sender().sendMessage(main.parse(
                                    "<error>This shortcut can only be used by a player. Use the coordinate form from console."));
                            return;
                        }
                        final String locationName = context.get("Location Name");
                        final ObjectiveRegion region =
                                main.getIntegrationsManager().getWorldEditManager().getSelectionRegionOrNull(player);
                        if (region == null) {
                            context.sender().sendMessage(
                                    main.parse("<error>Please make a region selection using WorldEdit first."));
                            return;
                        }
                        addReachLocationObjective(main, context, level, locationName, region);
                    }));
        }
    }

    private static void addReachLocationObjective(
            final NotQuests main,
            final NQCommandContext context,
            final int level,
            final String locationName,
            final ObjectiveRegion region) {
        ReachLocationObjective reachLocationObjective = new ReachLocationObjective(main);
        reachLocationObjective.setLocationName(locationName);
        reachLocationObjective.setMinLocation(region.min());
        reachLocationObjective.setMaxLocation(region.max());

        main.getObjectiveManager().addObjective(reachLocationObjective, context, level);
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.reachLocation.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%LOCATIONNAME%", getLocationName()));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.minLocation", getMinLocation());
        configuration.set(initialPath + ".specifics.maxLocation", getMaxLocation());
        configuration.set(initialPath + ".specifics.locationName", getLocationName());
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }

    public final Location getMinLocation() {
        return min;
    }

    public void setMinLocation(final Location minLocation) {
        this.min = minLocation;
        if (getLocation() == null) {
            setLocation(minLocation, false);
        }
    }

    public final Location getMaxLocation() {
        return max;
    }

    public void setMaxLocation(final Location maxLocation) {
        this.max = maxLocation;
    }

    public final String getLocationName() {
        return locationName;
    }

    public void setLocationName(final String locationName) {
        this.locationName = locationName;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        setMinLocation(configuration.getLocation(initialPath + ".specifics.minLocation"));
        setMaxLocation(configuration.getLocation(initialPath + ".specifics.maxLocation"));
        locationName = configuration.getString(initialPath + ".specifics.locationName");
    }
}
