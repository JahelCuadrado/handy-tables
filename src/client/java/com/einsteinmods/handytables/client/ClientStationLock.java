package com.einsteinmods.handytables.client;

import com.einsteinmods.handytables.HandyTables;
import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.core.StationSlotLock;
import com.einsteinmods.handytables.menu.StationItems;
import com.einsteinmods.handytables.menu.StationLock;
import com.einsteinmods.handytables.network.StationLockPayload;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/**
 * The client's half of {@link StationLock}: when the server names the locked slot ({@link
 * StationLockPayload}) the same lock is installed on the vanilla menu the screen shows, clicks it
 * refuses are dropped before vanilla predicts them or sends them ({@code
 * MultiPlayerGameMode#handleContainerInput}), and a padlock is drawn over the slot.
 *
 * <p>Cost: nothing until a station opens from its item. While no such menu is open, one null check
 * per container screen frame and per click. While it is open, per frame one identity compare, one
 * item identity check (three for the anvil) and one 7x7 sprite blit; no allocation. State is
 * cleared when that screen is removed. Render thread only.
 */
public final class ClientStationLock {
  private static final Identifier PADLOCK = HandyTables.id("station_lock");
  private static final int PADLOCK_SIZE = 7;

  /**
   * An item is drawn 16x16; the padlock sits in its top right corner (the count is bottom right).
   */
  private static final int PADLOCK_LEFT = 16 - PADLOCK_SIZE;

  private static @Nullable AbstractContainerMenu lockedMenu;
  private static @Nullable StationLock lock;
  private static @Nullable Slot lockedSlot;

  private ClientStationLock() {}

  /**
   * Locks the named slot in the open menu when it is the one the server opened; a second payload
   * for the same menu moves the lock.
   */
  public static void receive(StationLockPayload payload, Minecraft minecraft) {
    LocalPlayer player = minecraft.player;
    Station station = Station.byId(payload.stationId());
    Screen screen = minecraft.gui.screen();
    if (player == null
        || station == null
        || screen == null
        || player.containerMenu.containerId != payload.containerId()
        || !InventorySlots.holdsStations(payload.inventorySlot())) {
      return;
    }
    AbstractContainerMenu menu = player.containerMenu;
    StationLock current = lock;
    if (current != null && menu == lockedMenu) {
      // The anvil in use moved (split off its stack by wear): the lock and padlock follow.
      current.moveTo(menu, player.getInventory(), payload.inventorySlot());
    } else {
      current = StationLock.install(menu, player.getInventory(), station, payload.inventorySlot());
      lockedMenu = menu;
      lock = current;
      ScreenEvents.remove(screen).register(closed -> clear());
      OpenSound.play(minecraft, player.getInventory().getItem(payload.inventorySlot()));
    }
    lockedSlot =
        current.menuSlot() == StationSlotLock.NO_MENU_SLOT
            ? null
            : menu.getSlot(current.menuSlot());
    // An open recipe book recounts only when Inventory#getTimesChanged moves.
    player.getInventory().setChanged();
  }

  /** True when the click must not happen: same checks as the server's {@code Portable*Menu}. */
  public static boolean refuses(
      int containerId, int slotNum, int buttonNum, ContainerInput input, Player player) {
    StationLock current = lock;
    return current != null
        && player.containerMenu == lockedMenu
        && containerId == player.containerMenu.containerId
        && current.refuses(player.containerMenu, slotNum, buttonNum, input, player.getInventory());
  }

  /**
   * The padlock in the top right corner of the locked slot (the count sits bottom right), while it
   * holds the station. Called at the end of vanilla {@code AbstractContainerScreen#extractSlots},
   * where the pose is already offset to the screen's corner, so slot coordinates apply as they are.
   */
  public static void extractPadlock(AbstractContainerMenu menu, GuiGraphicsExtractor graphics) {
    Slot slot = lockedSlot;
    StationLock current = lock;
    if (slot == null
        || current == null
        || menu != lockedMenu
        || !StationItems.opens(slot.getItem(), current.station())) {
      return;
    }
    graphics.blitSprite(
        RenderPipelines.GUI_TEXTURED,
        PADLOCK,
        slot.x + PADLOCK_LEFT,
        slot.y,
        PADLOCK_SIZE,
        PADLOCK_SIZE);
  }

  /** True when {@code menu} is the menu whose slot is locked. */
  public static boolean guards(AbstractContainerMenu menu) {
    return lock != null && menu == lockedMenu;
  }

  /**
   * Runs the client's prediction of a click ({@code AbstractContainerMenu#clicked} inside {@code
   * MultiPlayerGameMode#handleContainerInput}) as the server runs it: with the reserved item set
   * aside when the slot has spares.
   */
  public static void clickWithoutReserved(
      AbstractContainerMenu menu, Player player, ContainerInput input, Runnable click) {
    StationLock current = lock;
    if (current == null || menu != lockedMenu) {
      click.run();
      return;
    }
    current.clickWithoutReserved(player.getInventory(), input, click);
  }

  /** The recipe book count of the locked menu leaves out the station item in use. */
  public static void reserveFromCount(AbstractContainerMenu menu, StackedItemContents contents) {
    StationLock current = lock;
    if (current == null || menu != lockedMenu) {
      return;
    }
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      current.reserveFromCount(player.getInventory(), contents);
    }
  }

  /** True when {@code slot} of {@code menu} is the locked slot and still holds the station. */
  public static boolean isLockedSlot(AbstractContainerMenu menu, Slot slot) {
    StationLock current = lock;
    return current != null
        && menu == lockedMenu
        && slot == lockedSlot
        && StationItems.opens(slot.getItem(), current.station());
  }

  /** The locked menu slot, for tests; null when none. */
  public static @Nullable Slot lockedSlot() {
    return lockedSlot;
  }

  public static void clear() {
    lockedMenu = null;
    lock = null;
    lockedSlot = null;
  }
}
