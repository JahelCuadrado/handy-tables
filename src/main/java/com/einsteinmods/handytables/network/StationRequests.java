package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.core.StationPolicy;
import com.einsteinmods.handytables.menu.PortableStationMenu;
import com.einsteinmods.handytables.menu.ReturnTarget;
import com.einsteinmods.handytables.menu.StationItems;
import com.einsteinmods.handytables.menu.StationMenus;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.jspecify.annotations.Nullable;

/** Server side of {@link OpenStationPayload}. Runs on the server thread. */
public final class StationRequests {
  private StationRequests() {}

  /**
   * Opens the requested station. When the inventory is the open menu, it is closed first as vanilla
   * does when the inventory screen closes ({@code ServerPlayer#doCloseContainer}, so {@code
   * InventoryMenu#removed} gives the 2x2 crafting grid back); any other menu is closed by {@code
   * ServerPlayer#openMenu} itself. Then the client is told which slot the menu locked ({@link
   * StationLockPayload}), after vanilla's open-screen packet.
   */
  public static void open(ServerPlayer player, int inventorySlot, StationPolicy policy) {
    MenuProvider provider = provider(player, inventorySlot, policy);
    if (provider == null) {
      return;
    }
    if (player.containerMenu == player.inventoryMenu) {
      player.doCloseContainer();
    }
    OptionalInt containerId = player.openMenu(provider);
    if (containerId.isPresent()
        && player.containerMenu.containerId == containerId.getAsInt()
        && player.containerMenu instanceof PortableStationMenu station) {
      StationLockSync.send(player, player.containerMenu, station.stationLock());
    }
  }

  /**
   * The menu to open, or null when the request is refused: a spectator or dead player, an item on
   * the cursor (it would be dropped by the menu change), a slot that cannot hold a station, an item
   * that is no station or a station the policy forbids.
   */
  public static @Nullable MenuProvider provider(
      ServerPlayer player, int inventorySlot, StationPolicy policy) {
    if (player.isSpectator()
        || !player.isAlive()
        || !InventorySlots.holdsStations(inventorySlot)
        || !player.containerMenu.getCarried().isEmpty()) {
      return null;
    }
    Station station = StationItems.stationOf(player.getInventory().getItem(inventorySlot));
    if (station == null || !policy.allows(station)) {
      return null;
    }
    return StationMenus.provider(
        station, inventorySlot, ReturnTarget.of(player.containerMenu, player));
  }
}
