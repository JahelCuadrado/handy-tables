package com.einsteinmods.handytables.menu;

import com.einsteinmods.handytables.core.AnvilWear;
import com.einsteinmods.handytables.core.Station;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Which items open each station, and the title vanilla gives its screen. Side-agnostic. */
public final class StationItems {
  // Titles as the vanilla blocks' CONTAINER_TITLE (CraftingTableBlock, StonecutterBlock, ...).
  private static final Component CRAFTING_TITLE = Component.translatable("container.crafting");
  private static final Component STONECUTTER_TITLE =
      Component.translatable("container.stonecutter");
  private static final Component LOOM_TITLE = Component.translatable("container.loom");
  private static final Component CARTOGRAPHY_TITLE =
      Component.translatable("container.cartography_table");
  private static final Component SMITHING_TITLE = Component.translatable("container.upgrade");
  private static final Component GRINDSTONE_TITLE =
      Component.translatable("container.grindstone_title");
  private static final Component ANVIL_TITLE = Component.translatable("container.repair");

  private StationItems() {}

  /** The station {@code stack} opens, or null. One identity check per item, no allocation. */
  public static @Nullable Station stationOf(ItemStack stack) {
    if (stack.is(Items.CRAFTING_TABLE)) {
      return Station.CRAFTING_TABLE;
    }
    if (stack.is(Items.STONECUTTER)) {
      return Station.STONECUTTER;
    }
    if (stack.is(Items.LOOM)) {
      return Station.LOOM;
    }
    if (stack.is(Items.CARTOGRAPHY_TABLE)) {
      return Station.CARTOGRAPHY_TABLE;
    }
    if (stack.is(Items.SMITHING_TABLE)) {
      return Station.SMITHING_TABLE;
    }
    if (stack.is(Items.GRINDSTONE)) {
      return Station.GRINDSTONE;
    }
    if (anvilWear(stack) != null) {
      return Station.ANVIL;
    }
    return null;
  }

  /**
   * True when {@code stack} opens {@code station}: one identity check, three for the anvil (any
   * wear state, as vanilla {@code AnvilMenu#isValidBlock} accepts any block in {@code #anvil}).
   */
  public static boolean opens(ItemStack stack, Station station) {
    return switch (station) {
      case CRAFTING_TABLE -> stack.is(Items.CRAFTING_TABLE);
      case STONECUTTER -> stack.is(Items.STONECUTTER);
      case LOOM -> stack.is(Items.LOOM);
      case CARTOGRAPHY_TABLE -> stack.is(Items.CARTOGRAPHY_TABLE);
      case SMITHING_TABLE -> stack.is(Items.SMITHING_TABLE);
      case GRINDSTONE -> stack.is(Items.GRINDSTONE);
      case ANVIL -> anvilWear(stack) != null;
    };
  }

  /** How worn the anvil in {@code stack} is, or null when it is no anvil. */
  public static @Nullable AnvilWear anvilWear(ItemStack stack) {
    if (stack.is(Items.ANVIL)) {
      return AnvilWear.INTACT;
    }
    if (stack.is(Items.CHIPPED_ANVIL)) {
      return AnvilWear.CHIPPED;
    }
    if (stack.is(Items.DAMAGED_ANVIL)) {
      return AnvilWear.DAMAGED;
    }
    return null;
  }

  /** The anvil item for {@code wear}, as the blocks {@code AnvilBlock#damage} steps through. */
  public static Item anvilItem(AnvilWear wear) {
    return switch (wear) {
      case INTACT -> Items.ANVIL;
      case CHIPPED -> Items.CHIPPED_ANVIL;
      case DAMAGED -> Items.DAMAGED_ANVIL;
    };
  }

  public static Component title(Station station) {
    return switch (station) {
      case CRAFTING_TABLE -> CRAFTING_TITLE;
      case STONECUTTER -> STONECUTTER_TITLE;
      case LOOM -> LOOM_TITLE;
      case CARTOGRAPHY_TABLE -> CARTOGRAPHY_TITLE;
      case SMITHING_TABLE -> SMITHING_TITLE;
      case GRINDSTONE -> GRINDSTONE_TITLE;
      case ANVIL -> ANVIL_TITLE;
    };
  }
}
