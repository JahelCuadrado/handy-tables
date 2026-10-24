package com.einsteinmods.handytables.menu;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The inventory slot of the station item in use, while it holds an item of that station. Exactly
 * one item is reserved; vanilla's own checks keep it in place ({@code
 * AbstractContainerMenu#doClick} and {@code #moveItemStackTo} take through {@code Slot#tryRemove} /
 * {@code safeTake} → {@code remove}, place after {@code mayPlace}, merge up to {@code
 * getMaxStackSize}):
 *
 * <ul>
 *   <li>At rest: takes stop one short of the whole stack ({@code remove} is capped), so the
 *       reserved item never leaves; only the very same item (same components) can be added, up to a
 *       full stack, by a click, a drag or a shift-click merge. Vanilla merges onto a slot only when
 *       it {@code mayPickup}, so that stays true.
 *   <li>While the reserved item is set aside for a click ({@link
 *       StationLock#clickWithoutReserved}): the slot shows the spares only; they may be taken, only
 *       the same item added, and one place stays free for the reserved item to come back.
 * </ul>
 *
 * Once it holds anything else it is a plain slot again.
 */
final class LockedStationSlot extends Slot {
  private final StationLock lock;

  /** Takes the place of {@code original}: same container slot, position and menu index. */
  LockedStationSlot(Slot original, StationLock lock) {
    super(original.container, original.getContainerSlot(), original.x, original.y);
    this.index = original.index;
    this.lock = lock;
  }

  @Override
  public boolean mayPlace(ItemStack itemStack) {
    ItemStack reserved = lock.reservedAside();
    if (reserved != null) {
      return ItemStack.isSameItemSameComponents(itemStack, reserved);
    }
    ItemStack held = getItem();
    if (holdsStation(held)) {
      return ItemStack.isSameItemSameComponents(itemStack, held);
    }
    return super.mayPlace(itemStack);
  }

  @Override
  public int getMaxStackSize(ItemStack itemStack) {
    if (lock.reservedAside() != null) {
      return super.getMaxStackSize(itemStack) - 1;
    }
    return super.getMaxStackSize(itemStack);
  }

  /** At rest, a take leaves the reserved item: at most all but one. */
  @Override
  public ItemStack remove(int amount) {
    ItemStack held = getItem();
    if (lock.reservedAside() == null && holdsStation(held)) {
      return super.remove(Math.min(amount, held.getCount() - 1));
    }
    return super.remove(amount);
  }

  private boolean holdsStation(ItemStack held) {
    return StationItems.opens(held, lock.lockedStation());
  }
}
