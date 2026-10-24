package com.einsteinmods.handytables.client;

import com.einsteinmods.handytables.core.StationPolicy;
import com.einsteinmods.handytables.network.ServerPolicyPayload;
import com.einsteinmods.handytables.network.ShowInventoryPayload;
import com.einsteinmods.handytables.network.StationLockPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

/** Client-only entrypoint. */
public final class HandyTablesClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    StationClick.register();
    ClientPlayNetworking.registerGlobalReceiver(
        ServerPolicyPayload.TYPE, (payload, context) -> StationClick.setPolicy(payload.policy()));
    ClientPlayNetworking.registerGlobalReceiver(
        StationLockPayload.TYPE,
        (payload, context) -> ClientStationLock.receive(payload, context.client()));
    ClientPlayNetworking.registerGlobalReceiver(
        ShowInventoryPayload.TYPE, (payload, context) -> ShowInventory.show(context.client()));
    ScreenEvents.AFTER_INIT.register(
        (minecraft, screen, width, height) -> CursorKeeper.afterScreenInit(minecraft, screen));
    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> StationClick.setPolicy(StationPolicy.NONE));
  }
}
