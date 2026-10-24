package com.einsteinmods.handytables.client;

import com.einsteinmods.handytables.client.mixin.AbstractContainerScreenAccessor;
import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.core.StationPolicy;
import com.einsteinmods.handytables.menu.StationItems;
import com.einsteinmods.handytables.network.CloseStationPayload;
import com.einsteinmods.handytables.network.OpenStationPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The pick-item mouse button (middle click by default, rebinding respected) on a station item in
 * the player's own inventory, in any container screen, asks the server to open that station; on the
 * locked slot of an open station ({@link ClientStationLock}) it asks to close it and go back where
 * it was opened from. In creative, where vanilla clones with that button ({@code
 * AbstractContainerScreen#mouseClicked}), Ctrl (Cmd on macOS) must be held too.
 *
 * <p>Cost: nothing per frame or tick. One listener per container screen init; per mouse press in a
 * container screen a policy check first (servers without the mod stop there), then a key match and
 * a few field reads. No allocation unless a station opens.
 */
public final class StationClick {
  private static final int NO_SLOT = -1;

  private static volatile StationPolicy policy = StationPolicy.NONE;

  private StationClick() {}

  public static void register() {
    ScreenEvents.AFTER_INIT.register(
        (minecraft, screen, width, height) -> {
          if (screen instanceof AbstractContainerScreen<?>) {
            ScreenMouseEvents.allowMouseClick(screen).register(StationClick::allowClick);
          }
        });
  }

  /** From the server's {@code ServerPolicyPayload}; {@link StationPolicy#NONE} on disconnect. */
  public static void setPolicy(StationPolicy serverPolicy) {
    policy = serverPolicy;
  }

  /** False (cancel vanilla's click) only when the click opened a station. */
  private static boolean allowClick(Screen screen, MouseButtonEvent event) {
    StationPolicy current = policy;
    if (current.allowsNone()) {
      return true;
    }
    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null
        || !minecraft.options.keyPickItem.matchesMouse(event)
        || (player.hasInfiniteMaterials()
            && !event.hasControlDown()
            && !event.hasControlDownWithQuirk())
        || !(screen instanceof AbstractContainerScreen<?> containerScreen)
        || !containerScreen.getMenu().getCarried().isEmpty()
        || !((Object) screen instanceof AbstractContainerScreenAccessor access)) {
      return true;
    }
    Slot slot = access.handytables$hoveredSlot();
    if (slot == null
        || !(slot.container instanceof Inventory inventory)
        || inventory.player != player) {
      return true;
    }
    if (ClientStationLock.isLockedSlot(containerScreen.getMenu(), slot)) {
      ClientPlayNetworking.send(new CloseStationPayload(containerScreen.getMenu().containerId));
      return false;
    }
    ItemStack stack = slot.getItem();
    Station station = StationItems.stationOf(stack);
    if (station == null || !current.allows(station)) {
      return true;
    }
    int inventorySlot = inventorySlot(slot, inventory, stack, player);
    if (!InventorySlots.holdsStations(inventorySlot)) {
      return true;
    }
    CursorKeeper.rememberBeforeOpening(minecraft, station);
    ClientPlayNetworking.send(new OpenStationPayload(inventorySlot));
    return false;
  }

  /**
   * The {@code Inventory} index of the hovered slot. The creative inventory tab wraps the inventory
   * menu's slots ({@code CreativeModeInventoryScreen#selectTab} builds a {@code SlotWrapper} per
   * inventory menu slot) keeping the inventory container but with the inventory menu index as
   * container slot, so that index is looked up in the inventory menu. Identity checks make sure the
   * index really holds the hovered stack; {@link #NO_SLOT} otherwise.
   */
  private static int inventorySlot(
      Slot slot, Inventory inventory, ItemStack stack, LocalPlayer player) {
    int containerSlot = slot.getContainerSlot();
    if (InventorySlots.holdsStations(containerSlot) && inventory.getItem(containerSlot) == stack) {
      return containerSlot;
    }
    if (containerSlot >= 0 && containerSlot < player.inventoryMenu.slots.size()) {
      Slot wrapped = player.inventoryMenu.getSlot(containerSlot);
      if (wrapped.container == inventory && wrapped.getItem() == stack) {
        return wrapped.getContainerSlot();
      }
    }
    return NO_SLOT;
  }
}
