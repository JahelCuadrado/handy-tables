package com.einsteinmods.handytables.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StationSlotLockTest {
  private static final int MENU_SLOT = 30;
  private static final int HOTBAR_SLOT = 3;
  private static final int OTHER_MENU_SLOT = 12;
  private static final int PRIMARY = 0;
  private static final int OTHER_HOTBAR_SLOT = 5;

  private final StationSlotLock lock = new StationSlotLock(MENU_SLOT, HOTBAR_SLOT);

  @Test
  void aloneItRefusesEveryClickOnTheSlot() {
    assertTrue(lock.refuses(MENU_SLOT, PRIMARY, false, false, false, false, false));
    assertTrue(lock.refuses(MENU_SLOT, OTHER_HOTBAR_SLOT, true, false, false, false, false));
  }

  @Test
  void aloneItLetsItemsBePlacedOntoIt() {
    assertFalse(lock.refuses(MENU_SLOT, PRIMARY, false, false, false, false, true));
  }

  @Test
  void aloneItLetsACreativeCopyThrough() {
    assertFalse(lock.refuses(MENU_SLOT, PRIMARY, false, true, false, false, false));
  }

  @Test
  void aloneItRefusesSwapsFromTheSlotEvenOntoAnEmptySlot() {
    assertTrue(lock.refuses(OTHER_MENU_SLOT, HOTBAR_SLOT, true, false, false, true, false));
    assertFalse(lock.refuses(OTHER_MENU_SLOT, OTHER_HOTBAR_SLOT, true, false, false, false, false));
  }

  @Test
  void withSparesClicksOnTheSlotRunWithTheReservedOneSetAside() {
    assertFalse(lock.refuses(MENU_SLOT, PRIMARY, false, false, true, false, false));
    assertFalse(lock.refuses(MENU_SLOT, OTHER_HOTBAR_SLOT, true, false, true, false, false));
  }

  @Test
  void withSparesSwapsFromTheSlotOnlyOntoAnEmptySlot() {
    assertFalse(lock.refuses(OTHER_MENU_SLOT, HOTBAR_SLOT, true, false, true, true, false));
    assertTrue(lock.refuses(OTHER_MENU_SLOT, HOTBAR_SLOT, true, false, true, false, false));
    assertTrue(lock.refuses(MENU_SLOT, HOTBAR_SLOT, true, false, true, false, false));
  }

  @Test
  void leavesOtherSlotsAlone() {
    assertFalse(lock.refuses(OTHER_MENU_SLOT, PRIMARY, false, false, false, false, false));
    assertFalse(lock.refuses(OTHER_MENU_SLOT, HOTBAR_SLOT, false, false, false, false, false));
  }

  @Test
  void anOffhandLockOnlyRefusesItsSwap() {
    StationSlotLock offhand =
        new StationSlotLock(StationSlotLock.NO_MENU_SLOT, InventorySlots.OFFHAND);

    assertTrue(
        offhand.refuses(OTHER_MENU_SLOT, InventorySlots.OFFHAND, true, false, false, true, false));
    assertFalse(
        offhand.refuses(StationSlotLock.NO_MENU_SLOT, PRIMARY, false, false, false, false, false));
    assertFalse(offhand.refuses(OTHER_MENU_SLOT, PRIMARY, false, false, false, false, false));
  }
}
