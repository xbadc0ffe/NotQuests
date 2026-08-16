package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class PlaySound {
    private static final String SOUND = "sound";
    private static final String STOP_OTHER_SOUNDS = "stopOtherSounds";
    private static final String PLAY_FOR_EVERYONE_AT_SET_LOCATION = "playForEveryoneAtSetLocation";
    private static final String PLAY_FOR_EVERYONE_AT_THEIR_LOCATION = "playForEveryoneAtTheirLocation";
    private static final String WORLD_NAME = "worldName";
    private static final String LOCATION_X = "locationX";
    private static final String LOCATION_Y = "locationY";
    private static final String LOCATION_Z = "locationZ";
    private static final String VOLUME = "volume";
    private static final String PITCH = "pitch";
    private static final String SOUND_CATEGORY = "SoundCategory";

    private PlaySound() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("PlaySound")
                .displayName("Play Sound")
                .description("Plays a Minecraft sound for the target player or all online players.")
                .field(SOUND, FieldTypes.text((context, input) -> soundSuggestions()).config("specifics.soundName"), "Minecraft sound key to play.")
                .field(STOP_OTHER_SOUNDS, FieldTypes.presenceFlag().config("specifics.stopOtherSounds"), "Stops all currently playing sounds before playing this one.")
                .field(PLAY_FOR_EVERYONE_AT_SET_LOCATION, FieldTypes.presenceFlag().config("specifics.playForEveryoneAtSetLocation"), "All online players near the configured location hear the sound.")
                .field(PLAY_FOR_EVERYONE_AT_THEIR_LOCATION, FieldTypes.presenceFlag().config("specifics.playForEveryoneAtTheirLocation"), "All online players hear the sound at their own current location.")
                .field(WORLD_NAME, FieldTypes.text().config("specifics.worldName"), "World used when playing the sound at a fixed location.")
                .field(LOCATION_X, FieldTypes.optionalDouble().config("specifics.locationX"), "X coordinate used when playing the sound at a fixed location.")
                .field(LOCATION_Y, FieldTypes.optionalDouble().config("specifics.locationY"), "Y coordinate used when playing the sound at a fixed location.")
                .field(LOCATION_Z, FieldTypes.optionalDouble().config("specifics.locationZ"), "Z coordinate used when playing the sound at a fixed location.")
                .field(VOLUME, FieldTypes.optionalDouble().config("specifics.volume"), "Sound volume, usually between 0 and 1.")
                .field(PITCH, FieldTypes.optionalDouble().config("specifics.pitch"), "Sound pitch multiplier.")
                .field(SOUND_CATEGORY, FieldTypes.text().config("specifics.soundCategory"), "Minecraft sound category. Defaults to master.")
                .commands((type, builder, actionFor) -> main.getCommandManager().getNQCommandManager().command(builder
                        .required(SOUND, NQArguments.stringArgument(), NQDescription.of("Minecraft sound key to play."), (context, input) -> soundSuggestions())
                        .flag(NQFlag.presence(STOP_OTHER_SOUNDS, NQDescription.of("Stops all currently playing sounds before playing this one.")))
                        .flag(NQFlag.presence(PLAY_FOR_EVERYONE_AT_SET_LOCATION, NQDescription.of("All online players near the configured location hear the sound.")))
                        .flag(NQFlag.presence(PLAY_FOR_EVERYONE_AT_THEIR_LOCATION, NQDescription.of("All online players hear the sound at their own current location.")))
                        .flag(main.getCommandManager().world)
                        .flag(main.getCommandManager().locationX)
                        .flag(main.getCommandManager().locationY)
                        .flag(main.getCommandManager().locationZ)
                        .flag(NQFlag.builder(VOLUME, NQDescription.of("Sound volume, usually between 0 and 1.")).withArgument(NQArguments.doubleArgument()).build())
                        .flag(NQFlag.builder(PITCH, NQDescription.of("Sound pitch multiplier.")).withArgument(NQArguments.doubleArgument()).build())
                        .flag(NQFlag.builder(SOUND_CATEGORY, NQDescription.of("Minecraft sound category. Default: master."))
                                .withArgument(NQArguments.stringArgument())
                                .withSuggestions((context, input) -> soundCategorySuggestions())
                                .build())
                        .handler(context -> {
                            final var action = type.createAction();
                            action.setValue(SOUND, context.get(SOUND));
                            action.setValue(STOP_OTHER_SOUNDS, context.flags().isPresent(STOP_OTHER_SOUNDS));
                            action.setValue(PLAY_FOR_EVERYONE_AT_SET_LOCATION, context.flags().isPresent(PLAY_FOR_EVERYONE_AT_SET_LOCATION));
                            action.setValue(PLAY_FOR_EVERYONE_AT_THEIR_LOCATION, context.flags().isPresent(PLAY_FOR_EVERYONE_AT_THEIR_LOCATION));
                            final World world = context.flags().getValue("world", null);
                            final Double x = context.flags().getValue("locationX", null);
                            final Double y = context.flags().getValue("locationY", null);
                            final Double z = context.flags().getValue("locationZ", null);
                            action.setValue(WORLD_NAME, world == null ? "" : world.getName());
                            action.setValue(LOCATION_X, x);
                            action.setValue(LOCATION_Y, y);
                            action.setValue(LOCATION_Z, z);
                            action.setValue(VOLUME, context.flags().getValue(VOLUME, null));
                            action.setValue(PITCH, context.flags().getValue(PITCH, null));
                            action.setValue(SOUND_CATEGORY, context.flags().getValue(SOUND_CATEGORY, "master"));
                            main.getActionCatalog().addAction(action, context, actionFor);
                        })))
                .singleLine((action, arguments) -> {
                    action.setValue(SOUND, arguments.get(0));
                    final String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT);
                    action.setValue(STOP_OTHER_SOUNDS, joined.contains("--stopothersounds"));
                    action.setValue(PLAY_FOR_EVERYONE_AT_SET_LOCATION, joined.contains("--playforeveryoneatsetlocation"));
                    action.setValue(PLAY_FOR_EVERYONE_AT_THEIR_LOCATION, joined.contains("--playforeveryoneattheirlocation"));
                    action.setValue(SOUND_CATEGORY, "master");
                })
                .execute((action, questPlayer, objects) -> {
                    final Player player = questPlayer == null ? null : questPlayer.getPlayer();
                    if (player == null) {
                        return;
                    }
                    if (action.flag(STOP_OTHER_SOUNDS)) {
                        player.stopAllSounds();
                    }
                    Location location = player.getLocation();
                    final String worldName = action.text(WORLD_NAME);
                    final Double x = action.action().value(LOCATION_X, Double.class);
                    final Double y = action.action().value(LOCATION_Y, Double.class);
                    final Double z = action.action().value(LOCATION_Z, Double.class);
                    if (!worldName.isBlank() && x != null && y != null && z != null) {
                        final World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            location = new Location(world, x, y, z);
                        }
                    }
                    final SoundCategory category = soundCategory(action.text(SOUND_CATEGORY));
                    final Double volumeValue = action.action().value(VOLUME, Double.class);
                    final Double pitchValue = action.action().value(PITCH, Double.class);
                    final float volume = volumeValue == null ? 1f : volumeValue.floatValue();
                    final float pitch = pitchValue == null ? 1f : pitchValue.floatValue();
                    if (action.flag(PLAY_FOR_EVERYONE_AT_THEIR_LOCATION)) {
                        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            onlinePlayer.playSound(onlinePlayer.getLocation(), action.text(SOUND), category, volume, pitch);
                        }
                    } else if (action.flag(PLAY_FOR_EVERYONE_AT_SET_LOCATION)) {
                        location.getWorld().playSound(location, action.text(SOUND), category, volume, pitch);
                    } else {
                        player.playSound(location, action.text(SOUND), category, volume, pitch);
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Plays sound: " + action.text(SOUND))
                .register();
    }

    private static SoundCategory soundCategory(final String value) {
        try {
            return SoundCategory.valueOf((value == null || value.isBlank() ? "master" : value).toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return SoundCategory.MASTER;
        }
    }

    private static List<String> soundCategorySuggestions() {
        final List<String> completions = new ArrayList<>();
        for (final SoundCategory category : SoundCategory.values()) {
            completions.add(category.name().toLowerCase(Locale.ROOT));
        }
        return completions;
    }

    private static List<String> soundSuggestions() {
        final List<String> completions = new ArrayList<>();
        final var soundRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT);
        for (final Sound sound : soundRegistry) {
            completions.add(soundRegistry.getKeyOrThrow(sound).asString());
        }
        return completions;
    }
}
