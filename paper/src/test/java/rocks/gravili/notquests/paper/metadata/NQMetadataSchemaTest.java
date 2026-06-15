package rocks.gravili.notquests.paper.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import rocks.gravili.notquests.paper.commands.framework.NQCommandSchema.CommandIndex;

class NQMetadataSchemaTest {
    @Test
    void serializesRuntimeRegistryAndCommandGraphTogether() {
        final CommandIndex commandIndex = new CommandIndex("6.3.0", List.of());
        final NQMetadataSchema.RegistryIndex registry = new NQMetadataSchema.RegistryIndex(
                List.of(new NQMetadataSchema.TypeInfo(
                        "Harvest",
                        "rocks.gravili.notquests.paper.structs.objectives.HarvestObjective",
                        "Counts fully grown crops harvested by the player.",
                        null,
                        false)),
                List.of(),
                List.of(),
                List.of(),
                List.of(new NQMetadataSchema.VariableInfo(
                        "FoodLevel",
                        "rocks.gravili.notquests.paper.structs.variables.PlayerFoodLevelVariable",
                        "Reads the target player's hunger bar.",
                        "NUMBER",
                        true,
                        List.of("player"),
                        List.of(),
                        List.of(),
                        List.of())));

        final String json = new NQMetadataSchema.MetadataIndex(
                        "6.3.0", "26.1.2", commandIndex, registry)
                .toJson();

        assertTrue(json.contains("\"commands\""));
        assertTrue(json.contains("\"registry\""));
        assertTrue(json.contains("\"objectives\""));
        assertTrue(json.contains("\"variables\""));
        assertTrue(json.contains("\"Harvest\""));
        assertTrue(json.contains("\"FoodLevel\""));
        assertTrue(json.contains("\"valueType\": \"NUMBER\""));
    }

    @Test
    void escapesJsonStringsDeterministically() {
        final NQMetadataSchema.TypeInfo info = new NQMetadataSchema.TypeInfo(
                "Quote\"Slash\\Line\n",
                "example.Type",
                "Description with \"quotes\" and a newline\ninside.",
                null,
                false);

        final StringBuilder json = new StringBuilder();
        info.appendJson(json, 0);

        assertEquals(
                "{\n"
                        + "  \"id\": \"Quote\\\"Slash\\\\Line\\n\",\n"
                        + "  \"displayName\": \"Quote\\\"Slash\\\\Line\\n\",\n"
                        + "  \"className\": \"example.Type\",\n"
                        + "  \"description\": \"Description with \\\"quotes\\\" and a newline\\ninside.\",\n"
                        + "  \"source\": null,\n"
                        + "  \"integrationOnly\": false,\n"
                        + "  \"fields\": [\n"
                        + "  ],\n"
                        + "  \"flags\": [\n"
                        + "  ]\n"
                        + "}",
                json.toString());
    }
}
