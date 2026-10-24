package com.einsteinmods.handytables.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InventorySlotsTest {
  private static final int LAST_MAIN_SLOT = 35;
  private static final int FIRST_ARMOR_SLOT = 36;
  private static final int LAST_ARMOR_SLOT = 39;
  private static final int BODY_ARMOR_SLOT = 41;

  @Test
  void hotbarMainAndOffhandHoldStations() {
    assertTrue(InventorySlots.holdsStations(0));
    assertTrue(InventorySlots.holdsStations(LAST_MAIN_SLOT));
    assertTrue(InventorySlots.holdsStations(InventorySlots.OFFHAND));
  }

  @Test
  void armorEquipmentAndOutOfRangeSlotsDoNot() {
    assertFalse(InventorySlots.holdsStations(-1));
    assertFalse(InventorySlots.holdsStations(FIRST_ARMOR_SLOT));
    assertFalse(InventorySlots.holdsStations(LAST_ARMOR_SLOT));
    assertFalse(InventorySlots.holdsStations(BODY_ARMOR_SLOT));
    assertFalse(InventorySlots.holdsStations(Integer.MAX_VALUE));
  }
}
