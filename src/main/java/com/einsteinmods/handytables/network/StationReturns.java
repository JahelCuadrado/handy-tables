package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.menu.PortableStationMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.jspecify.annotations.Nullable;

/** Server side of {@link CloseStationPayload}. Runs on the server thread. */
public final class StationReturns {
  private StationReturns() {}

  /**
   * Closes the open station menu {@code containerId} and goes back where it was opened from ({@link
   * com.einsteinmods.handytables.menu.ReturnTarget}). The station is closed as vanilla closes a
   * menu ({@code ServerPlayer#doCloseContainer}: {@code removed} gives the inputs back) but without
   * the close packet, so the client switches screens directly and the cursor stays: the container
   * block's menu opens through {@code ServerPlayer#openMenu} (which sends no close either, the
   * inventory menu being open by then), else the client is told to show the inventory.
   */
  public static void close(ServerPlayer player, int containerId) {
    PortableStationMenu station = openStation(player, containerId);
    if (station == null) {
      return;
    }
    MenuProvider reopen = station.stationCheck().returnTarget().reopen(player);
    player.doCloseContainer();
    if (reopen == null || player.openMenu(reopen).isEmpty()) {
      ServerPlayNetworking.send(player, ShowInventoryPayload.INSTANCE);
    }
  }

  /** The menu {@link #close} would open, or null (the inventory, or no matching station open). */
  public static @Nullable MenuProvider returnProvider(ServerPlayer player, int containerId) {
    PortableStationMenu station = openStation(player, containerId);
    return station == null ? null : station.stationCheck().returnTarget().reopen(player);
  }

  private static @Nullable PortableStationMenu openStation(ServerPlayer player, int containerId) {
    if (player.isSpectator()
        || !player.isAlive()
        || player.containerMenu.containerId != containerId
        || !(player.containerMenu instanceof PortableStationMenu station)) {
      return null;
    }
    return station;
  }
}
