package com.einsteinmods.handytables.core;

/**
 * Which clicks a station's locked inventory slot refuses while its menu is open. Exactly one
 * station item of the slot is in use (reserved); the rest of a stack (the spares) stays usable.
 * Mirrors the inputs of vanilla {@code AbstractContainerMenu#doClick}:
 *
 * <ul>
 *   <li>Only the reserved item (no spares): every input aimed at the slot except a creative copy
 *       ({@code CLONE}, which leaves the slot untouched), and every number-key or offhand swap
 *       ({@code SWAP} with button 0-8 or 40, the {@code Inventory} index of the swapped slot) whose
 *       source is the slot.
 *   <li>With spares: the clicks run with the reserved item set aside, so vanilla moves spares only.
 *       Refused: a swap from the slot onto a slot that is not empty, since vanilla would put that
 *       slot's stack into the locked one, and a swap of the slot with itself.
 * </ul>
 *
 * <p>Pure, free of Minecraft types.
 *
 * @param menuSlot the locked slot's menu index, or {@link #NO_MENU_SLOT} when the menu does not
 *     show it (the offhand)
 * @param inventorySlot the locked slot's {@code Inventory} index
 */
public record StationSlotLock(int menuSlot, int inventorySlot) {
  /** The locked inventory slot has no slot in the menu. */
  public static final int NO_MENU_SLOT = -1;

  /**
   * True when the click must be refused.
   *
   * @param clickedSlot the menu slot clicked
   * @param button the click's button; for a swap, the {@code Inventory} index it swaps with
   * @param swap the click is a number-key or offhand swap
   * @param clone the click is a creative copy
   * @param spares the slot holds more than the reserved item
   * @param clickedSlotEmpty the clicked menu slot is empty
   * @param placing the click cannot take from the slot: a pick-up click with an item on the cursor,
   *     a drag step, or a double-click gathering into the cursor (the lone reserved item is then
   *     set aside, so the slot counts as empty, as vanilla's would be after the take)
   */
  public boolean refuses(
      int clickedSlot,
      int button,
      boolean swap,
      boolean clone,
      boolean spares,
      boolean clickedSlotEmpty,
      boolean placing) {
    boolean onSlot = menuSlot != NO_MENU_SLOT && clickedSlot == menuSlot;
    boolean swapFromSlot = swap && button == inventorySlot;
    if (!spares) {
      return swapFromSlot || (onSlot && !clone && !placing);
    }
    return swapFromSlot && (onSlot || !clickedSlotEmpty);
  }
}
