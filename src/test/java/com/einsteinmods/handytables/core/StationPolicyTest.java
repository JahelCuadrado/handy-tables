package com.einsteinmods.handytables.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StationPolicyTest {
  private static final int UNKNOWN_STATION_BIT = 1 << 20;

  @Test
  void unrestrictedAllowsEveryStationAndNoneAllowsNothing() {
    for (Station station : Station.values()) {
      assertTrue(StationPolicy.UNRESTRICTED.allows(station));
      assertFalse(StationPolicy.NONE.allows(station));
    }
    assertTrue(StationPolicy.NONE.allowsNone());
    assertFalse(StationPolicy.UNRESTRICTED.allowsNone());
  }

  @Test
  void withTogglesOnlyThatStation() {
    StationPolicy policy = StationPolicy.UNRESTRICTED.with(Station.LOOM, false);

    assertFalse(policy.allows(Station.LOOM));
    assertTrue(policy.allows(Station.CRAFTING_TABLE));
    assertTrue(policy.allows(Station.SMITHING_TABLE));
    assertEquals(StationPolicy.UNRESTRICTED, policy.with(Station.LOOM, true));
  }

  @Test
  void unknownBitsAreDropped() {
    StationPolicy policy = new StationPolicy(UNKNOWN_STATION_BIT | Station.STONECUTTER.mask());

    assertEquals(Station.STONECUTTER.mask(), policy.allowedMask());
    assertTrue(new StationPolicy(UNKNOWN_STATION_BIT).allowsNone());
  }

  @Test
  void hostIsNeverLimitedAndGuestsFollowTheFile() {
    StationPolicy file = StationPolicy.NONE.with(Station.CRAFTING_TABLE, true);

    assertSame(StationPolicy.UNRESTRICTED, PlayerPolicyResolver.forPlayer(true, file));
    assertSame(file, PlayerPolicyResolver.forPlayer(false, file));
  }
}
