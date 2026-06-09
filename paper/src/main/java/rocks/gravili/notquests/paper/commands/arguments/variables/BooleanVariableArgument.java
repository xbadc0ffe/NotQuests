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

package rocks.gravili.notquests.paper.commands.arguments.variables;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.List;

/**
 * Native-framework port of {@code BooleanVariableValueParser}: a generic "variable value" argument
 * whose value is the raw string (a boolean or a boolean expression). Backed by a <b>greedy</b>
 * {@code greedyString()} (mirroring the Cloud mapping in {@code CommandManager.preSetupCommands()})
 * so that special symbols like a comma do not trigger Brigadier's red "invalid" colouring.
 */
public final class BooleanVariableArgument extends NQArgumentType<String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    public BooleanVariableArgument(final String identifier, final Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static BooleanVariableArgument booleanVariableArgument(final String identifier, final Variable<?> variable) {
        return new BooleanVariableArgument(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.greedyString();
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        return List.of();
    }
}
