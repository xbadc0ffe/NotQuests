package rocks.gravili.notquests.paper.structs.variables;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.block.Biome;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerCurrentBiomeVariable extends Variable<String> {
  public PlayerCurrentBiomeVariable(NotQuests main) {
    super(main);
  }

  @Override
  public String getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      final Biome biome = questPlayer.getPlayer().getLocation().getBlock().getBiome();
      return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
          .getKeyOrThrow(biome).getKey();
    } else {
      return null;
    }
  }

  @Override
  public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    List<String> possibleValues = new ArrayList<>();
    final var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
    for (Biome biome : biomeRegistry) {
      possibleValues.add(biomeRegistry.getKeyOrThrow(biome).getKey());
    }
    return possibleValues;
  }

  @Override
  public String getPlural() {
    return "Biomes";
  }

  @Override
  public String getSingular() {
    return "Biome";
  }
}
