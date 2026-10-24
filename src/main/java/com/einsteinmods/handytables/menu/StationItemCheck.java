package com.einsteinmods.handytables.menu;

import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Replaces the vanilla "block still there and in reach" check ({@code
 * AbstractContainerMenu#stillValid(ContainerLevelAccess, Player, Block)}, {@code
 * ItemCombinerMenu#stillValid}) for a station opened from its item: the menu stays open while the
 * player, in the same level, still has an item of that station ({@link StationItems#opens}) in the
 * inventory or on the cursor. Once it is gone {@code ServerPlayer#tick} closes the menu and vanilla
 * {@code removed} gives the input items back.
 *
 * <p>A dead player keeps the menu, as with a placed station (whose check only looks at the block):
 * {@code ServerPlayer#die} drops the inventory, then {@code LivingEntity#tickDeath} (or the
 * respawn) removes the player and {@code Player#remove} closes the menu, so {@code removed} drops
 * the inputs at the death spot ({@code AbstractContainerMenu#dropOrPlaceInInventory}: a removed
 * player drops). Closing it earlier would put them back into the dead player's inventory, which is
 * never dropped again (lost on respawn).
 *
 * <p>Cost: {@code ServerPlayer#tick} calls this every tick while the menu is open, for that player
 * only. Steady state is one slot read and one item identity check (three for the anvil); the
 * inventory is scanned (41 slots at most, no allocation) only on the tick the item left the
 * remembered slot, which then points to its new slot.
 */
public final class StationItemCheck {
  /** {@link #slotOf} found no station item in the inventory. */
  public static final int NO_SLOT = -1;

  private final Level level;
  private final Station station;
  private final int openedSlot;
  private final ReturnTarget returnTarget;
  private int rememberedSlot;

  public StationItemCheck(
      Level level, Station station, int inventorySlot, ReturnTarget returnTarget) {
    this.level = level;
    this.station = station;
    this.openedSlot = inventorySlot;
    this.returnTarget = returnTarget;
    this.rememberedSlot = inventorySlot;
  }

  /** True while the menu may stay open. {@code carried} is the menu's cursor stack. */
  public boolean holds(Player player, ItemStack carried) {
    if (player.isDeadOrDying()) {
      return true;
    }
    if (player.level() != level) {
      return false;
    }
    Inventory inventory = player.getInventory();
    return StationItems.opens(inventory.getItem(rememberedSlot), station)
        || StationItems.opens(carried, station)
        || slotOf(inventory) != NO_SLOT;
  }

  /**
   * The inventory slot holding the station item: the remembered one while it still does, else the
   * first hotbar/main/offhand slot that does (then remembered), else {@link #NO_SLOT}.
   */
  public int slotOf(Inventory inventory) {
    if (StationItems.opens(inventory.getItem(rememberedSlot), station)) {
      return rememberedSlot;
    }
    for (int slot = 0; slot < InventorySlots.MAIN_SIZE; slot++) {
      if (StationItems.opens(inventory.getItem(slot), station)) {
        rememberedSlot = slot;
        return slot;
      }
    }
    if (StationItems.opens(inventory.getItem(InventorySlots.OFFHAND), station)) {
      rememberedSlot = InventorySlots.OFFHAND;
      return InventorySlots.OFFHAND;
    }
    return NO_SLOT;
  }

  public Station station() {
    return station;
  }

  /** The inventory slot the station was opened from; {@link StationLock} locks it. */
  public int openedSlot() {
    return openedSlot;
  }

  /** Where closing the station with its own gesture goes back to. */
  public ReturnTarget returnTarget() {
    return returnTarget;
  }

  /** Follows the station item to {@code inventorySlot} (a worn anvil split off its stack). */
  public void remember(int inventorySlot) {
    rememberedSlot = inventorySlot;
  }
}
