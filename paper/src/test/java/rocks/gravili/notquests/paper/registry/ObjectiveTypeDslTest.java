/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.registering.ObjectiveManager;
import rocks.gravili.notquests.paper.metadata.NQMetadataSchema.TypeInfo;

class ObjectiveTypeDslTest {
    @Test
    void objectiveDslProducesRuntimeAndMetadataDefinitionFromOneSource() {
        final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        final ObjectiveManager manager = mock(ObjectiveManager.class);

        final ObjectiveType type = new ObjectiveType.Builder(main, manager, "BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field(
                        "materials",
                        FieldTypes.itemSelection().config("specifics.itemStackSelection"),
                        "Blocks or custom items that count when broken.")
                .field(
                        "amount",
                        FieldTypes.numberExpression().progressNeeded(),
                        "Number of matching blocks the player must break.")
                .flag(
                        "doNotDeductIfBlockIsPlaced",
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.deductIfBlockPlaced"),
                        "Stops NotQuests from removing progress when the player places a matching block again.")
                .register();

        verify(manager).registerObjective(type);

        final TypeInfo metadata = type.metadata(null, false);
        assertEquals("BreakBlocks", metadata.id());
        assertEquals("Break Blocks", metadata.displayName());
        assertEquals(2, metadata.fields().size());
        assertEquals(1, metadata.flags().size());
        assertEquals("materials", metadata.fields().getFirst().name());
        assertEquals("amount", metadata.fields().get(1).name());
        assertEquals("doNotDeductIfBlockIsPlaced", metadata.flags().getFirst().name());
    }

    @Test
    void objectiveDslRejectsUndocumentedArguments() {
        final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        final ObjectiveManager manager = mock(ObjectiveManager.class);

        assertThrows(IllegalArgumentException.class, () -> new ObjectiveType.Builder(main, manager, "Broken")
                .displayName("Broken")
                .description("Counts something.")
                .field("amount", FieldTypes.numberExpression(), "")
                .register());
    }
}
