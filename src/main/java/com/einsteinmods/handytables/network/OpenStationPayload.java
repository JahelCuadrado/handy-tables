package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.HandyTables;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: open the station whose item is in this player inventory slot ({@code Inventory}
 * index, not a menu slot). The server checks everything again.
 */
public record OpenStationPayload(int inventorySlot) implements CustomPacketPayload {
  public static final Type<OpenStationPayload> TYPE = new Type<>(HandyTables.id("open_station"));

  public static final StreamCodec<FriendlyByteBuf, OpenStationPayload> CODEC =
      ByteBufCodecs.VAR_INT
          .<FriendlyByteBuf>cast()
          .map(OpenStationPayload::new, OpenStationPayload::inventorySlot);

  @Override
  public Type<OpenStationPayload> type() {
    return TYPE;
  }
}
