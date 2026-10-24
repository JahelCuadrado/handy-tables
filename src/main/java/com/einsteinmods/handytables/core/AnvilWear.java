package com.einsteinmods.handytables.core;

import org.jspecify.annotations.Nullable;

/**
 * How worn an anvil is, and the vanilla odds of wearing it further. Mirrors {@code
 * AnvilBlock#damage} (anvil → chipped → damaged → destroyed) and the roll in {@code
 * AnvilMenu#onTake} ({@code player.getRandom().nextFloat() < 0.12F}, never with infinite
 * materials). Pure, free of Minecraft types.
 */
public enum AnvilWear {
  INTACT,
  CHIPPED,
  DAMAGED;

  /** Chance that one use wears the anvil one step, as vanilla {@code AnvilMenu#onTake}. */
  public static final float CHANCE_PER_USE = 0.12F;

  /** The next step, or null when this anvil breaks. */
  public @Nullable AnvilWear next() {
    return switch (this) {
      case INTACT -> CHIPPED;
      case CHIPPED -> DAMAGED;
      case DAMAGED -> null;
    };
  }

  /** True when a use with this {@code roll} (uniform in [0, 1)) wears the anvil. */
  public static boolean wears(float roll) {
    return roll < CHANCE_PER_USE;
  }
}
