package rocks.gravili.notquests.paper.registry;

import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;

public final class ObjectiveDataContext {
    private final DefinedObjective objective;

    ObjectiveDataContext(final DefinedObjective objective) {
        this.objective = objective;
    }

    public ItemStackSelection itemSelection(final String name) {
        return objective.value(name, ItemStackSelection.class);
    }

    public boolean flag(final String name) {
        return Boolean.TRUE.equals(objective.value(name, Boolean.class));
    }
}
