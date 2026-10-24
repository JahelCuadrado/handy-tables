package com.einsteinmods.handytables.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StationTest {
  private static final int UNKNOWN_ID = 99;

  /** The network sends these ids as policy bits: changing one breaks older peers. */
  @Test
  void idsAreStable() {
    assertEquals(0, Station.CRAFTING_TABLE.id());
    assertEquals(1, Station.STONECUTTER.id());
    assertEquals(2, Station.LOOM.id());
    assertEquals(3, Station.CARTOGRAPHY_TABLE.id());
    assertEquals(4, Station.SMITHING_TABLE.id());
    assertEquals(5, Station.GRINDSTONE.id());
    assertEquals(6, Station.ANVIL.id());
  }

  @Test
  void everyIdMapsBackToItsStation() {
    for (Station station : Station.values()) {
      assertEquals(station, Station.byId(station.id()));
      assertEquals(1 << station.id(), station.mask());
    }
    assertNull(Station.byId(UNKNOWN_ID));
    assertNull(Station.byId(-1));
  }
}
