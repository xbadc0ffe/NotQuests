package com.notquests.paper.commands;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandManager;

public abstract class BaseCommand {
    protected final NotQuests notQuests;
    protected NQCommandBuilder builder;

    public BaseCommand(NotQuests notQuests, NQCommandBuilder builder) {
        this.notQuests = notQuests;
        this.builder = builder;
    }

    public abstract void apply(NQCommandManager commandManager);
}
