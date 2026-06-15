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

package rocks.gravili.notquests.paper.metadata;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.managers.registering.ActionManager;
import rocks.gravili.notquests.paper.managers.registering.ConditionsManager;
import rocks.gravili.notquests.paper.managers.registering.ObjectiveManager;
import rocks.gravili.notquests.paper.managers.registering.TriggerManager;
import rocks.gravili.notquests.paper.managers.registering.VariablesManager;
import rocks.gravili.notquests.paper.metadata.NQMetadataSchema.MetadataIndex;
import rocks.gravili.notquests.paper.metadata.NQMetadataSchema.RegistryIndex;
import rocks.gravili.notquests.paper.metadata.NQMetadataSchema.TypeInfo;
import rocks.gravili.notquests.paper.metadata.NQMetadataSchema.VariableInfo;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;
import rocks.gravili.notquests.paper.structs.variables.Variable;

/** Builds the exported metadata bundle from the currently enabled NotQuests runtime. */
public final class NQMetadataExporter {
    private final NotQuests main;

    public NQMetadataExporter(final NotQuests main) {
        this.main = main;
    }

    public MetadataIndex metadataIndex() {
        return new MetadataIndex(
                main.getMain().getDescription().getVersion(),
                Bukkit.getMinecraftVersion(),
                main.getCommandManager().getNQCommandManager().commandIndex(),
                registryIndex());
    }

    private RegistryIndex registryIndex() {
        return new RegistryIndex(
                objectiveInfo(),
                actionInfo(),
                typeInfo(
                        main.getConditionsManager().getConditionsAndIdentifiers(),
                        ConditionsManager::conditionLiteralDescription),
                typeInfo(
                        main.getTriggerManager().getTriggersAndIdentifiers(),
                        TriggerManager::triggerLiteralDescription),
                variableInfo());
    }

    private List<TypeInfo> objectiveInfo() {
        return main.getObjectiveManager().getObjectivesAndIdentifiers().entrySet().stream()
                .map(entry -> {
                    final String source = integrationSource(entry.getValue());
                    final var objectiveType =
                            main.getObjectiveManager().getObjectiveTypesAndIdentifiers().get(entry.getKey());
                    if (objectiveType != null) {
                        return objectiveType.metadata(source, source != null);
                    }
                    return new TypeInfo(
                            entry.getKey(),
                            entry.getValue().getName(),
                            ObjectiveManager.objectiveLiteralDescription(entry.getKey()),
                            source,
                            source != null);
                })
                .sorted(Comparator.comparing(TypeInfo::id))
                .toList();
    }

    private List<TypeInfo> actionInfo() {
        return main.getActionManager().getActionsAndIdentifiers().entrySet().stream()
                .map(entry -> {
                    final String source = integrationSource(entry.getValue());
                    return new TypeInfo(
                            entry.getKey(),
                            entry.getValue().getName(),
                            ActionManager.actionLiteralDescription(entry.getKey()),
                            source,
                            source != null);
                })
                .sorted(Comparator.comparing(TypeInfo::id))
                .toList();
    }

    private <T> List<TypeInfo> typeInfo(
            final Map<String, Class<? extends T>> entries,
            final java.util.function.Function<String, String> describe) {
        return entries.entrySet().stream()
                .map(entry -> new TypeInfo(
                        entry.getKey(),
                        entry.getValue().getName(),
                        describe.apply(entry.getKey()),
                        integrationSource(entry.getValue()),
                        integrationSource(entry.getValue()) != null))
                .sorted(Comparator.comparing(TypeInfo::id))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<VariableInfo> variableInfo() {
        return main.getVariablesManager().getVariablesAndIdentifiers().entrySet().stream()
                .map(entry -> {
                    final Variable<?> variable = main.getVariablesManager().getVariableFromString(entry.getKey());
                    if (variable == null) {
                        return new VariableInfo(
                                entry.getKey(),
                                entry.getValue().getName(),
                                VariablesManager.variableLiteralDescriptionText(entry.getKey()),
                                null,
                                false,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of());
                    }
                    return new VariableInfo(
                            entry.getKey(),
                            entry.getValue().getName(),
                            VariablesManager.variableLiteralDescriptionText(entry.getKey()),
                            variable.getVariableDataType().name(),
                            variable.isCanSetValue(),
                            variable.getRequiredStrings().stream()
                                    .map((StringVariableValueParser<CommandSender> parser) -> parser.getIdentifier())
                                    .toList(),
                            variable.getRequiredNumbers().stream()
                                    .map((NumberVariableValueParser<CommandSender> parser) -> parser.getIdentifier())
                                    .toList(),
                            variable.getRequiredBooleans().stream()
                                    .map((BooleanVariableValueParser<CommandSender> parser) -> parser.getIdentifier())
                                    .toList(),
                            variable.getRequiredBooleanFlags().stream().map(NQFlag::name).toList());
                })
                .sorted(Comparator.comparing(VariableInfo::id))
                .toList();
    }

    private static String integrationSource(final Class<?> type) {
        final String packageName = type.getPackageName();
        final String marker = ".hooks.";
        final int markerIndex = packageName.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        final String afterHooks = packageName.substring(markerIndex + marker.length());
        final int nextDot = afterHooks.indexOf('.');
        return nextDot < 0 ? afterHooks : afterHooks.substring(0, nextDot);
    }
}
