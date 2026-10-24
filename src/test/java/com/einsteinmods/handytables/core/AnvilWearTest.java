package com.einsteinmods.handytables.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnvilWearTest {
  private static final float JUST_BELOW_CHANCE = Math.nextDown(AnvilWear.CHANCE_PER_USE);

  /** As vanilla {@code AnvilBlock#damage}: anvil → chipped → damaged → destroyed. */
  @Test
  void wearStepsLikeThePlacedBlock() {
    assertEquals(AnvilWear.CHIPPED, AnvilWear.INTACT.next());
    assertEquals(AnvilWear.DAMAGED, AnvilWear.CHIPPED.next());
    assertNull(AnvilWear.DAMAGED.next());
  }

  /** As vanilla {@code AnvilMenu#onTake}: {@code nextFloat() < 0.12F}. */
  @Test
  void rollsBelowTheVanillaChanceWear() {
    assertEquals(0.12F, AnvilWear.CHANCE_PER_USE);
    assertTrue(AnvilWear.wears(0.0F));
    assertTrue(AnvilWear.wears(JUST_BELOW_CHANCE));
    assertFalse(AnvilWear.wears(AnvilWear.CHANCE_PER_USE));
    assertFalse(AnvilWear.wears(Math.nextDown(1.0F)));
  }
}
