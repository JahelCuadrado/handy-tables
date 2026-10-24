package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.HandyTables;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: the open gesture on the locked station slot of menu {@code containerId}: close
 * the station and go back where it was opened from. The server checks everything again.
 */
public record CloseStationPayload(int containerId) implements CustomPacketPayload {
  public static final Type<CloseStationPayload> TYPE = new Type<>(HandyTables.id("close_station"));

  public static final StreamCodec<FriendlyByteBuf, CloseStationPayload> CODEC =
      ByteBufCodecs.VAR_INT
          .<FriendlyByteBuf>cast()
          .map(CloseStationPayload::new, CloseStationPayload::containerId);

  @Override
  public Type<CloseStationPayload> type() {
    return TYPE;
  }
}
