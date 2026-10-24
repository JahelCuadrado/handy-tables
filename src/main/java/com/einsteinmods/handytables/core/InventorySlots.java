package com.einsteinmods.handytables.core;

/**
 * Player inventory slots a station item may be opened from. Mirrors vanilla {@code
 * Inventory#INVENTORY_SIZE} and {@code Inventory#SLOT_OFFHAND} (core stays free of Minecraft
 * types); armor, body and saddle slots never hold a station.
 */
public final class InventorySlots {
  /** Hotbar (0-8) and main inventory (9-35). */
  public static final int MAIN_SIZE = 36;

  public static final int OFFHAND = 40;

  private InventorySlots() {}

  /** True for a hotbar, main inventory or offhand slot. */
  public static boolean holdsStations(int slot) {
    return (slot >= 0 && slot < MAIN_SIZE) || slot == OFFHAND;
  }
}
