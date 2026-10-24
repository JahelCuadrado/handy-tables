package com.einsteinmods.handytables.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;

/**
 * Vanilla grindstone menu opened from the item; only {@code stillValid} differs (vanilla {@code
 * GrindstoneMenu#stillValid} wants the block at the access position), see {@link StationItemCheck}.
 * The result slot's {@code onTake} awards the experience ({@code ExperienceOrb#award} at the centre
 * of the access position, the player's feet when opened) and plays the grindstone sound there;
 * {@code removed} gives the inputs back through the access.
 */
public final class PortableGrindstoneMenu extends GrindstoneMenu implements PortableStationMenu {
  private final StationItemCheck check;
  private final StationLock lock;

  public PortableGrindstoneMenu(
      int containerId, Inventory inventory, ContainerLevelAccess access, StationItemCheck check) {
    super(containerId, inventory, access);
    this.check = check;
    this.lock = StationLock.install(this, inventory, check.station(), check.openedSlot());
  }

  @Override
  public StationLock stationLock() {
    return lock;
  }

  @Override
  public StationItemCheck stationCheck() {
    return check;
  }

  @Override
  public boolean stillValid(Player player) {
    return check.holds(player, getCarried());
  }

  /** Refuses or guards clicks on the locked station slot, see {@link StationLock}. */
  @Override
  public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
    Inventory inventory = player.getInventory();
    if (!lock.refuses(this, slotIndex, buttonNum, containerInput, inventory)) {
      lock.clickWithoutReserved(
          inventory,
          containerInput,
          () -> super.clicked(slotIndex, buttonNum, containerInput, player));
    }
  }
}
