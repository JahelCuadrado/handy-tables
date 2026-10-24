package com.einsteinmods.handytables.menu;

import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.core.StationSlotLock;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Locks the inventory slot of the station item in use while its menu is open, as long as that slot
 * still holds an item of the station: the slot it was opened from, until anvil wear splits the
 * anvil in use off its stack ({@link #moveTo}: the worn one is in use, the rest is free).
 * Side-agnostic: the server's {@code Portable*Menu} installs it when built, the client on the
 * vanilla menu its screen shows once the server names the slot, so both run the same checks and the
 * client predicts exactly what the server does.
 *
 * <p>Exactly one item of the slot is in use (reserved); the rest of a stack (the spares) stays
 * usable with vanilla's own gestures. Three parts: {@link #refuses} drops clicks before vanilla
 * {@code AbstractContainerMenu#doClick} runs (see {@link StationSlotLock}); {@link
 * #clickWithoutReserved} runs every other click with the reserved item set aside, so vanilla's
 * pick-up, half pick-up, shift-click, drop, gathering, drag and swap code only ever sees and moves
 * the spares (vanilla's quick move works on the slot's stack directly, so no take cap could do
 * this); and the menu's {@code Slot} for that inventory index is a {@link LockedStationSlot}, whose
 * {@code mayPickup} / {@code mayPlace} / {@code getMaxStackSize} answer for the reserved item.
 *
 * <p>Cost: per click one record compare and one item identity check (three for the anvil); with
 * spares, one split and one merge around the click. Nothing per tick or frame.
 */
public final class StationLock {
  /** One station item is in use: the recipe book counts and takes the rest of the stack only. */
  private static final int RESERVED = 1;

  private final Station station;
  private StationSlotLock slots = new StationSlotLock(StationSlotLock.NO_MENU_SLOT, 0);

  /** The reserved item while it is set aside ({@link #placeWithoutReserved}), else null. */
  private @Nullable ItemStack reservedAside;

  private StationLock(Station station) {
    this.station = station;
  }

  /**
   * Locks {@code inventorySlot} of {@code inventory} in {@code menu}: finds the menu slot showing
   * it (vanilla {@code AbstractContainerMenu#addStandardInventorySlots} adds the 36 main slots as
   * plain {@code Slot}s; the offhand has none) and replaces it with a {@link LockedStationSlot} at
   * the same index and position.
   */
  public static StationLock install(
      AbstractContainerMenu menu, Inventory inventory, Station station, int inventorySlot) {
    StationLock lock = new StationLock(station);
    lock.slots = lock.lockSlot(menu, inventory, inventorySlot);
    return lock;
  }

  /**
   * Moves the lock to {@code inventorySlot} (the anvil in use was split off its stack): the old
   * menu slot becomes a plain {@code Slot} again, as vanilla built it.
   */
  public void moveTo(AbstractContainerMenu menu, Inventory inventory, int inventorySlot) {
    int previous = slots.menuSlot();
    if (previous != StationSlotLock.NO_MENU_SLOT) {
      Slot locked = menu.slots.get(previous);
      Slot plain = new Slot(locked.container, locked.getContainerSlot(), locked.x, locked.y);
      plain.index = locked.index;
      menu.slots.set(previous, plain);
    }
    slots = lockSlot(menu, inventory, inventorySlot);
  }

  private StationSlotLock lockSlot(
      AbstractContainerMenu menu, Inventory inventory, int inventorySlot) {
    for (int index = 0; index < menu.slots.size(); index++) {
      Slot slot = menu.slots.get(index);
      if (slot.container == inventory && slot.getContainerSlot() == inventorySlot) {
        menu.slots.set(index, new LockedStationSlot(slot, this));
        return new StationSlotLock(index, inventorySlot);
      }
    }
    return new StationSlotLock(StationSlotLock.NO_MENU_SLOT, inventorySlot);
  }

  /**
   * The recipe book's count without the station item in use: vanilla counts the inventory with
   * {@code Inventory#fillStackedContents} (main slots, items {@code
   * Inventory#isUsableForCrafting}), so one station item of the locked slot is taken off again. For
   * the client's {@code RecipeBookComponent}, whose count must match what {@link
   * #placeWithoutReserved} lets the server take.
   */
  public void reserveFromCount(Inventory inventory, StackedItemContents contents) {
    ItemStack stack = inventory.getItem(slots.inventorySlot());
    if (isReservable(stack)) {
      contents.accountStack(stack, -RESERVED);
    }
  }

  /**
   * Runs a recipe book placement ({@code ServerPlaceRecipe#placeRecipe}) as if the station item in
   * use were not in the inventory, so both its count ({@code Inventory#fillStackedContents}) and
   * its take ({@code ServerPlaceRecipe#moveItemToGrid} → {@code
   * Inventory#findSlotMatchingCraftingIngredient}) skip it, and the rest of a stack stays usable.
   * The reserved item is split off for the call; a slot left empty holds a renamed copy meanwhile
   * (not usable for crafting, merges with nothing, not a free slot for {@code clearGrid}), so the
   * grid's items cannot land in it. Nothing is synced in between: the placement sends no inventory
   * packet and the menu broadcasts after it. Afterwards the item goes back to the slot (merged, or
   * set when the rest was used); if the slot was filled to the limit by tables from the grid, it
   * goes where {@code Inventory#placeItemBackInInventory} puts it.
   */
  public <T> T placeWithoutReserved(Inventory inventory, Supplier<T> placement) {
    ItemStack stack = inventory.getItem(slots.inventorySlot());
    if (!isReservable(stack)) {
      return placement.get();
    }
    return setAside(inventory, stack, true, placement);
  }

  private void setAside(Inventory inventory, ItemStack stack, Runnable action) {
    setAside(
        inventory,
        stack,
        false,
        () -> {
          action.run();
          return null;
        });
  }

  private <T> T setAside(
      Inventory inventory, ItemStack stack, boolean holdTheSlot, Supplier<T> action) {
    int inventorySlot = slots.inventorySlot();
    ItemStack reserved = stack.split(RESERVED);
    ItemStack placeholder = ItemStack.EMPTY;
    if (holdTheSlot && stack.isEmpty()) {
      placeholder = reserved.copy();
      placeholder.set(DataComponents.CUSTOM_NAME, Component.empty());
      inventory.setItem(inventorySlot, placeholder);
    }
    reservedAside = reserved;
    try {
      return action.get();
    } finally {
      reservedAside = null;
      ItemStack current = inventory.getItem(inventorySlot);
      if (current.isEmpty() || current == placeholder) {
        inventory.setItem(inventorySlot, reserved);
      } else if (ItemStack.isSameItemSameComponents(current, reserved)
          && current.getCount() < current.getMaxStackSize()) {
        current.grow(RESERVED);
      } else {
        inventory.placeItemBackInInventory(reserved, Prediction.SERVER_ONLY);
      }
    }
  }

  /**
   * A main inventory slot (the only ones vanilla's recipe book reads) holding the station in a
   * state the recipe book would use.
   */
  private boolean isReservable(ItemStack stack) {
    return slots.inventorySlot() < InventorySlots.MAIN_SIZE
        && StationItems.opens(stack, station)
        && Inventory.isUsableForCrafting(stack);
  }

  /** True when the click must not happen (see {@link StationSlotLock}). */
  public boolean refuses(
      AbstractContainerMenu menu,
      int slotIndex,
      int buttonNum,
      ContainerInput input,
      Inventory inventory) {
    ItemStack held = inventory.getItem(slots.inventorySlot());
    if (!StationItems.opens(held, station)) {
      return false;
    }
    boolean clickedSlotEmpty =
        menu.isValidSlotIndex(slotIndex)
            && slotIndex >= 0
            && menu.getSlot(slotIndex).getItem().isEmpty();
    return slots.refuses(
        slotIndex,
        buttonNum,
        input == ContainerInput.SWAP,
        input == ContainerInput.CLONE,
        held.getCount() > RESERVED,
        clickedSlotEmpty,
        (input == ContainerInput.PICKUP && !menu.getCarried().isEmpty())
            || input == ContainerInput.QUICK_CRAFT
            || input == ContainerInput.PICKUP_ALL);
  }

  /**
   * Runs a vanilla click with the reserved item set aside when the slot has spares, so the click
   * can only move spares. A lone reserved item stays put, except for a double-click ({@code
   * PICKUP_ALL}): then the slot is left empty for the click, as vanilla's slot would be after the
   * take, so vanilla gathers the same item from the other slots into the cursor (a gathering only
   * takes from slots; nothing can land in the emptied one).
   */
  public void clickWithoutReserved(Inventory inventory, ContainerInput input, Runnable click) {
    ItemStack held = inventory.getItem(slots.inventorySlot());
    if (StationItems.opens(held, station)
        && (held.getCount() > RESERVED || input == ContainerInput.PICKUP_ALL)) {
      setAside(inventory, held, click);
    } else {
      click.run();
    }
  }

  /** The reserved item while it is set aside for a click or a placement, else null. */
  @Nullable ItemStack reservedAside() {
    return reservedAside;
  }

  Station lockedStation() {
    return station;
  }

  /** True while the locked slot holds an item of the station. */
  public boolean isLocked(Inventory inventory) {
    return StationItems.opens(inventory.getItem(slots.inventorySlot()), station);
  }

  /** The locked slot's menu index, or {@link StationSlotLock#NO_MENU_SLOT} (the offhand). */
  public int menuSlot() {
    return slots.menuSlot();
  }

  public int inventorySlot() {
    return slots.inventorySlot();
  }

  public Station station() {
    return station;
  }
}
