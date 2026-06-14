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

package rocks.gravili.notquests.paper.managers.integrations.betonquest;

import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.BetonQuestApiService;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.identifier.ActionIdentifier;
import org.betonquest.betonquest.api.identifier.ConditionIdentifier;
import org.betonquest.betonquest.api.identifier.ObjectiveIdentifier;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.FeatureTypeRegistry;
import org.betonquest.betonquest.api.quest.TypeFactory;
import org.betonquest.betonquest.kernel.processor.adapter.ActionAdapter;
import org.betonquest.betonquest.kernel.registry.FactoryRegistry;
import org.betonquest.betonquest.kernel.registry.feature.InterceptorRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.conditions.BQConditionsCondition;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.conversationInterceptors.NotQuestsInterceptorFactory;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQAbortQuestEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQActionEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQFailQuestEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQQuestPointsEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQStartQuestEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQTriggerObjectiveEvent;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BetonQuestManager {
  private final NotQuests main;
  private BetonQuestApi api;
  private boolean registered = false;

  public BetonQuestManager(final NotQuests main) {
    this.main = main;
  }

  public boolean enable() {
    try {
      final Optional<BetonQuestApiService> apiService = BetonQuestApiService.get();
      register(
          apiService
              .map(service -> service.api(main.getMain()))
              .orElseGet(() -> BetonQuest.getInstance().getBetonQuestApi()));
      return registered;
    } catch (final QuestException | RuntimeException exception) {
      main.getLogManager()
          .warn("Could not enable BetonQuest support: " + exception.getMessage());
      return false;
    }
  }

  private void register(final BetonQuestApi api) throws QuestException {
    this.api = api;

    api.actions().registry().register("nq_action", new BQActionEvent(main));
    api.actions().registry().register("nq_triggerobjective", new BQTriggerObjectiveEvent(main));
    api.actions().registry().register("nq_startquest", new BQStartQuestEvent(main));
    api.actions().registry().register("nq_failquest", new BQFailQuestEvent(main));
    api.actions().registry().register("nq_abortquest", new BQAbortQuestEvent(main));
    api.actions().registry().register("nq_questpoints", new BQQuestPointsEvent(main));
    api.conditions().registry().register("nq_condition", new BQConditionsCondition(main));

    registerInterceptor();
    registered = true;
  }

  private void registerInterceptor() {
    BetonQuest.getInstance()
        .getComponentLoader()
        .getOptional(InterceptorRegistry.class)
        .ifPresent(
            registry -> {
              registry.register("notquests", new NotQuestsInterceptorFactory(main));
              main.getLogManager().info("Registered BetonQuest interceptor: notquests");
            });
  }

  public boolean isRegistered() {
    return registered;
  }

  public BetonQuestApi api() throws QuestException {
    if (api == null || !registered) {
      throw new QuestException("BetonQuest support is not enabled.");
    }
    return api;
  }

  public List<String> packageNames() {
    try {
      return new ArrayList<>(api().packages().getPackages().keySet());
    } catch (final QuestException exception) {
      return List.of();
    }
  }

  public List<String> actionNames(final String packageName) {
    return configKeys(packageName, "actions", "events");
  }

  public List<String> conditionNames(final String packageName) {
    return configKeys(packageName, "conditions");
  }

  public List<String> objectiveNames(final String packageName) {
    return configKeys(packageName, "objectives");
  }

  public List<String> objectiveStates() {
    return List.of("NEW", "ACTIVE", "COMPLETED", "PAUSED", "CANCELED");
  }

  public List<String> actionTypes() {
    try {
      if (api().actions().registry() instanceof final FactoryRegistry<?> registry) {
        return new ArrayList<>(registry.keySet());
      }
    } catch (final QuestException ignored) {
      // Return no suggestions when BetonQuest is unavailable.
    }
    return List.of();
  }

  private List<String> configKeys(final String packageName, final String... sections) {
    try {
      final QuestPackage questPackage = questPackage(packageName);
      for (final String sectionName : sections) {
        final ConfigurationSection section =
            questPackage.getConfig().getConfigurationSection(sectionName);
        if (section != null) {
          return new ArrayList<>(section.getKeys(false));
        }
      }
    } catch (final QuestException ignored) {
      // Missing package/section means no completions.
    }
    return List.of();
  }

  public QuestPackage questPackage(final String packageName) throws QuestException {
    final QuestPackage questPackage = api().packages().getPackage(packageName);
    if (questPackage == null) {
      throw new QuestException("BetonQuest package '" + packageName + "' does not exist.");
    }
    return questPackage;
  }

  public ActionIdentifier actionIdentifier(final String packageName, final String actionName)
      throws QuestException {
    return api()
        .identifiers()
        .getFactory(ActionIdentifier.class)
        .parseIdentifier(questPackage(packageName), actionName);
  }

  public ConditionIdentifier conditionIdentifier(final String packageName, final String conditionName)
      throws QuestException {
    return api()
        .identifiers()
        .getFactory(ConditionIdentifier.class)
        .parseIdentifier(questPackage(packageName), conditionName);
  }

  public ObjectiveIdentifier objectiveIdentifier(final String packageName, final String objectiveName)
      throws QuestException {
    return api()
        .identifiers()
        .getFactory(ObjectiveIdentifier.class)
        .parseIdentifier(questPackage(packageName), objectiveName);
  }

  public Profile profileFor(final QuestPlayer questPlayer) throws QuestException {
    if (questPlayer == null) {
      throw new QuestException("A player is required for this BetonQuest action.");
    }
    final OfflinePlayer player =
        questPlayer.getPlayer() != null
            ? questPlayer.getPlayer()
            : Bukkit.getOfflinePlayer(questPlayer.getUniqueId());
    return api().profiles().getProfile(player);
  }

  public void runAction(final QuestPlayer questPlayer, final String packageName, final String actionName)
      throws QuestException {
    api().actions().manager().run(profileFor(questPlayer), actionIdentifier(packageName, actionName));
  }

  public boolean testCondition(
      final QuestPlayer questPlayer, final String packageName, final String conditionName)
      throws QuestException {
    return questPlayer != null
        && api()
            .conditions()
            .manager()
            .test(profileFor(questPlayer), conditionIdentifier(packageName, conditionName));
  }

  @SuppressWarnings("unchecked")
  public void runInlineAction(final QuestPlayer questPlayer, final String actionInstruction)
      throws QuestException {
    final String trimmed = actionInstruction == null ? "" : actionInstruction.trim();
    if (trimmed.isEmpty()) {
      throw new QuestException("BetonQuest inline action is empty.");
    }
    final String type = trimmed.contains(" ") ? trimmed.substring(0, trimmed.indexOf(' ')) : trimmed;
    final String rest = trimmed.length() == type.length() ? "" : trimmed.substring(type.length()).trim();
    final QuestPackage questPackage =
        api().packages().getPackages().values().stream()
            .findFirst()
            .orElseThrow(() -> new QuestException("No BetonQuest packages are loaded."));
    final Instruction instruction = api().instructions().create(questPackage, rest);
    final FeatureTypeRegistry<ActionAdapter> registry =
        (FeatureTypeRegistry<ActionAdapter>) api().actions().registry();
    final TypeFactory<ActionAdapter> factory = registry.getFactory(type);
    factory.parseInstruction(instruction).fire(profileFor(questPlayer));
  }
}
