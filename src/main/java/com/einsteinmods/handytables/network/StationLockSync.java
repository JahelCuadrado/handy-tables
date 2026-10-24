package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.menu.StationLock;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Tells the client which slot a station menu locks ({@link StationLockPayload}). */
public final class StationLockSync {
  private StationLockSync() {}

  /** When the menu opened, and again whenever the lock moves (anvil wear split). */
  public static void send(ServerPlayer player, AbstractContainerMenu menu, StationLock lock) {
    if (ServerPlayNetworking.canSend(player, StationLockPayload.TYPE)) {
      ServerPlayNetworking.send(
          player,
          new StationLockPayload(menu.containerId, lock.inventorySlot(), lock.station().id()));
    }
  }
}
