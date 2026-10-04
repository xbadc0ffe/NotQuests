package com.notquests.builtin.objectives;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.items.ItemStackSelection;

import java.util.List;
import java.util.Map;

/**
 * FORK DIVERGENCE: the delivery chat note names what was delivered ("Delivered 64 Wheat
 * to Trader Joe"), so material ids are title-cased for display. Matching never uses this.
 */
class DeliverItemsPrettyNamesTest {

    @Test
    void materialIdsTitleCaseWithSpaces() {
        assertEquals("Wheat", DeliverItemsObjective.prettyItemNames(selection(List.of("WHEAT"))));
        assertEquals("Oak Log", DeliverItemsObjective.prettyItemNames(selection(List.of("OAK_LOG"))));
        assertEquals("Wheat, Oak Log",
                DeliverItemsObjective.prettyItemNames(selection(List.of("WHEAT", "OAK_LOG"))));
    }

    @Test
    void anyAndEmptySelectionsFallBackToItems() {
        assertEquals("items", DeliverItemsObjective.prettyItemNames(null));
        assertEquals("items", DeliverItemsObjective.prettyItemNames(
                ItemStackSelection.of(List.of(), List.of(), List.of(), true, -1)));
        assertEquals("items", DeliverItemsObjective.prettyItemNames(
                ItemStackSelection.of(List.of(), List.of(), List.of(), false, -1)));
    }

    private static com.notquests.core.items.ItemSelection selection(final List<String> materials) {
        return ItemStackSelection.of(materials, List.of(), List.<Map<String, Object>>of(), false, -1);
    }
}
