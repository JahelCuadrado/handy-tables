package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.core.PlayerPolicyResolver;
import com.einsteinmods.handytables.core.StationPolicy;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Registers payloads and server-side handlers. Handlers run on the server thread. */
public final class HandyTablesNetworking {
  /** The running server's policy file; only read and written on server start/stop and requests. */
  private static volatile StationPolicy filePolicy = StationPolicy.UNRESTRICTED;

  private HandyTablesNetworking() {}

  public static void register() {
    PayloadTypeRegistry.serverboundPlay()
        .register(OpenStationPayload.TYPE, OpenStationPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(ServerPolicyPayload.TYPE, ServerPolicyPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(StationLockPayload.TYPE, StationLockPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(CloseStationPayload.TYPE, CloseStationPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(ShowInventoryPayload.TYPE, ShowInventoryPayload.CODEC);

    ServerPlayNetworking.registerGlobalReceiver(
        OpenStationPayload.TYPE,
        (payload, context) ->
            StationRequests.open(
                context.player(), payload.inventorySlot(), policyFor(context.player())));

    ServerPlayNetworking.registerGlobalReceiver(
        CloseStationPayload.TYPE,
        (payload, context) -> StationReturns.close(context.player(), payload.containerId()));

    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          if (ServerPlayNetworking.canSend(handler.player, ServerPolicyPayload.TYPE)) {
            sender.sendPacket(new ServerPolicyPayload(policyFor(handler.player)));
          }
        });
  }

  /** Set on server start from the policy file; reset to unrestricted when the server stops. */
  public static void setFilePolicy(StationPolicy policy) {
    filePolicy = policy;
  }

  /** The singleplayer/LAN host is never limited; everyone else follows the file. */
  public static StationPolicy policyFor(ServerPlayer player) {
    return PlayerPolicyResolver.forPlayer(
        player.level().getServer().isSingleplayerOwner(player.nameAndId()), filePolicy);
  }
}
