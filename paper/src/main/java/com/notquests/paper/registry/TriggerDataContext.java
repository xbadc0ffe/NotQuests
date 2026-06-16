package com.notquests.paper.registry;

public record TriggerDataContext(DefinedTrigger trigger) {
    public String text(final String name) {
        return trigger.text(name);
    }

    public int integer(final String name, final int fallback) {
        return trigger.value(name, fallback);
    }
}
