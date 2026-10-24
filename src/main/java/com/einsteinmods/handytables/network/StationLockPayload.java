package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.HandyTables;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client, right after a station opened from its item: the menu ({@code containerId}) and
 * the {@code Inventory} slot the server locked in it, with the station's id, so the client locks
 * the same slot in its copy of the menu and draws the padlock. Sent once per opening, after the
 * vanilla open-screen packet on the same connection, so the client already shows that menu.
 */
public record StationLockPayload(int containerId, int inventorySlot, int stationId)
    implements CustomPacketPayload {
  public static final Type<StationLockPayload> TYPE = new Type<>(HandyTables.id("station_lock"));

  public static final StreamCodec<FriendlyByteBuf, StationLockPayload> CODEC =
      StreamCodec.composite(
          ByteBufCodecs.VAR_INT,
          StationLockPayload::containerId,
          ByteBufCodecs.VAR_INT,
          StationLockPayload::inventorySlot,
          ByteBufCodecs.VAR_INT,
          StationLockPayload::stationId,
          StationLockPayload::new);

  @Override
  public Type<StationLockPayload> type() {
    return TYPE;
  }
}
