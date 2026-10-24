package com.einsteinmods.handytables.menu;

import com.einsteinmods.handytables.core.AnvilWear;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Wears the anvil item a portable anvil was opened from by one step, as vanilla {@code
 * AnvilMenu#onTake} wears the placed block with {@code AnvilBlock#damage}: anvil → chipped anvil →
 * damaged anvil → gone. Only that item changes; no block is ever read or written.
 *
 * <p>The item is the one {@link StationItemCheck} tracks (inventory slot first, else the cursor).
 * One item of a stack wears: a lone anvil is replaced in place; from a larger stack one is split
 * off, and the worn one goes where vanilla {@code Inventory#placeItemBackInInventory} would put it
 * and is the anvil in use from then on (tracked, so the same anvil keeps wearing), or is dropped
 * when the inventory is full. Components (a custom name) are kept, as {@code
 * ItemStack#transmuteCopy} does.
 */
final class AnvilItemWear {
  /** Where the anvil in use is after one wear step. */
  enum Outcome {
    /** Worn in its slot or on the cursor (or there was no anvil to wear). */
    STAYED,
    /** Split off its stack into another inventory slot, now tracked. */
    MOVED,
    /** Split off its stack and dropped: the inventory was full. */
    DROPPED,
    /** It was a damaged anvil and broke. */
    BROKE
  }

  private AnvilItemWear() {}

  /** Wears one anvil item. */
  static Outcome wear(Player player, StationItemCheck check, AbstractContainerMenu menu) {
    Inventory inventory = player.getInventory();
    int slot = check.slotOf(inventory);
    boolean onCursor = slot == StationItemCheck.NO_SLOT;
    ItemStack stack = onCursor ? menu.getCarried() : inventory.getItem(slot);
    AnvilWear wear = StationItems.anvilWear(stack);
    if (wear == null) {
      return Outcome.STAYED;
    }
    AnvilWear next = wear.next();
    if (next == null) {
      stack.shrink(1);
      synchronize(player, inventory, slot, onCursor);
      return Outcome.BROKE;
    }
    ItemStack worn = stack.transmuteCopy(StationItems.anvilItem(next), 1);
    if (stack.getCount() == 1) {
      if (onCursor) {
        menu.setCarried(worn);
      } else {
        inventory.setItem(slot, worn);
        synchronize(player, inventory, slot, false);
      }
      return Outcome.STAYED;
    }
    stack.shrink(1);
    synchronize(player, inventory, slot, onCursor);
    return placeAndTrack(player, inventory, worn, check);
  }

  /** Same slot choice as {@code Inventory#placeItemBackInInventory}; dropped when full. */
  private static Outcome placeAndTrack(
      Player player, Inventory inventory, ItemStack worn, StationItemCheck check) {
    int target = inventory.getSlotWithRemainingSpace(worn);
    if (target == Inventory.NOT_FOUND_INDEX) {
      target = inventory.getFreeSlot();
    }
    if (target == Inventory.NOT_FOUND_INDEX) {
      player.drop(worn, false, Prediction.SERVER_ONLY);
      return Outcome.DROPPED;
    }
    inventory.add(target, worn);
    check.remember(target);
    synchronize(player, inventory, target, false);
    return Outcome.MOVED;
  }

  /**
   * Sends the changed inventory slot as {@code Inventory#placeItemBackInInventory} does: the anvil
   * menu only syncs the 36 main slots, not the offhand. The cursor is synced by the menu itself.
   */
  private static void synchronize(Player player, Inventory inventory, int slot, boolean onCursor) {
    if (!onCursor && player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(inventory.createInventoryUpdatePacket(slot));
    }
  }
}
