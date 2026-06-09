/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code ObjectiveParser}: resolves an {@link Objective} (by id) within the
 * {@link ObjectiveHolder} carried by a prior positional argument at the configured {@code level}.
 *
 * <p>NOTE: the Cloud parser read the holder from the command context inside {@code parse}. Brigadier
 * does not hand {@code convert} a context, so the holder must be supplied to {@link #convert} via the
 * Brigadier context in {@link #suggest}. See {@link #convert} for the implication on resolution.
 */
public final class ObjectiveArgument extends NQArgumentType<Objective> {
    private final NotQuests main;
    private final int level;

    public ObjectiveArgument(final NotQuests main, final int level) {
        this.main = main;
        this.level = level;
    }

    public static ObjectiveArgument objectiveArgument(final NotQuests main, final int level) {
        return new ObjectiveArgument(main, level);
    }

    @Override
    public Objective convert(final String input) throws CommandSyntaxException {
        // The owning ObjectiveHolder lives on a prior positional argument; without a CommandContext
        // here we cannot reach it, so resolution is delegated to convert(context, input).
        throw fail("No Objective found: " + input);
    }

    public Objective convert(final CommandContext<?> context, final String input) throws CommandSyntaxException {
        final List<Objective> entries = getObjectiveHolderForLevel(context, level).getObjectives();
        for (final Objective objective : entries) {
            if (String.valueOf(objective.getObjectiveID()).equalsIgnoreCase(input)) {
                return objective;
            }
        }
        throw fail("No Objective found: " + input);
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        for (final Objective objective : getObjectiveHolderForLevel(context, level).getObjectives()) {
            entries.add(String.valueOf(objective.getObjectiveID()));
        }
        return entries;
    }

    private ObjectiveHolder getObjectiveHolderForLevel(final CommandContext<?> context, final int level) {
        final ObjectiveHolder objectiveHolder;
        if (level == 0) {
            objectiveHolder = (ObjectiveHolder) context.getArgument("quest", Object.class);
        } else if (level == 1) {
            objectiveHolder = (ObjectiveHolder) context.getArgument("objectiveId", Object.class);
        } else {
            objectiveHolder = (ObjectiveHolder) context.getArgument("objectiveId" + level, Object.class);
        }
        return objectiveHolder;
    }
}
