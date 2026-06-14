/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.managers.integrations.betonquest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BetonQuestVersionSupportTest {
  @Test
  void acceptsBetonQuestThreeAndNewer() {
    assertTrue(BetonQuestVersionSupport.isSupported("3.0.0"));
    assertTrue(BetonQuestVersionSupport.isSupported("3.0.0-SNAPSHOT"));
    assertTrue(BetonQuestVersionSupport.isSupported("v3.1.4"));
    assertTrue(BetonQuestVersionSupport.isSupported("4.0.0"));
  }

  @Test
  void rejectsOlderAndUnparseableVersions() {
    assertFalse(BetonQuestVersionSupport.isSupported("2.2.1"));
    assertFalse(BetonQuestVersionSupport.isSupported("1.12.0"));
    assertFalse(BetonQuestVersionSupport.isSupported(""));
    assertFalse(BetonQuestVersionSupport.isSupported("not-a-version"));
  }
}
