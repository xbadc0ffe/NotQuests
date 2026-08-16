package com.notquests.paper.metadata;

import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import com.notquests.paper.NotQuests;
import com.notquests.paper.metadata.NQMetadataSchema.MetadataIndex;
import com.notquests.paper.metadata.NQMetadataSchema.RegistryIndex;
import com.notquests.paper.metadata.NQMetadataSchema.TypeInfo;
import com.notquests.paper.metadata.NQMetadataSchema.VariableInfo;

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
                conditionInfo(),
                triggerInfo(),
                variableInfo());
    }

    private List<TypeInfo> objectiveInfo() {
        return main.getObjectiveCatalog().getObjectiveTypesAndIdentifiers().values().stream()
                .map(objectiveType -> objectiveType.metadata(null, false))
                .sorted(Comparator.comparing(TypeInfo::id))
                .toList();
    }

    private List<TypeInfo> actionInfo() {
        return main.getActionCatalog().getActionTypesAndIdentifiers().values().stream()
                .map(actionType -> actionType.metadata(null, false))
                .sorted(Comparator.comparing(TypeInfo::id))
                .toList();
    }

    private List<TypeInfo> conditionInfo() {
        return main.getConditionCatalog().getConditionTypesAndIdentifiers().values().stream()
                .map(conditionType -> conditionType.metadata(null, false))
                .sorted(Comparator.comparing(TypeInfo::id))
                .toList();
    }

    private List<TypeInfo> triggerInfo() {
        return main.getTriggerCatalog().getTriggerTypesAndIdentifiers().values().stream()
                .map(triggerType -> triggerType.metadata(null, false))
                .sorted(Comparator.comparing(TypeInfo::id))
                .toList();
    }

    private List<VariableInfo> variableInfo() {
        return main.getVariableCatalog().getVariableTypesAndIdentifiers().values().stream()
                .map(variableType -> variableType.metadata())
                .sorted(Comparator.comparing(VariableInfo::id))
                .toList();
    }
}
