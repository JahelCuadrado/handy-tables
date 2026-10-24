package com.einsteinmods.handytables.core;

import org.jspecify.annotations.Nullable;

/**
 * Workstations that open from their item. Each {@link #id} is stable: the network sends it as a bit
 * of {@link StationPolicy}, so never reuse or renumber one.
 */
public enum Station {
  CRAFTING_TABLE(0),
  STONECUTTER(1),
  LOOM(2),
  CARTOGRAPHY_TABLE(3),
  SMITHING_TABLE(4),
  GRINDSTONE(5),
  /** Anvil, chipped anvil and damaged anvil. */
  ANVIL(6);

  private static final Station[] BY_ID = values();

  private final int id;

  Station(int id) {
    this.id = id;
  }

  public int id() {
    return id;
  }

  /** This station's bit in a {@link StationPolicy} mask. */
  public int mask() {
    return 1 << id;
  }

  /** The station with {@code id}, or null for an unknown id. */
  public static @Nullable Station byId(int id) {
    for (Station station : BY_ID) {
      if (station.id == id) {
        return station;
      }
    }
    return null;
  }
}
