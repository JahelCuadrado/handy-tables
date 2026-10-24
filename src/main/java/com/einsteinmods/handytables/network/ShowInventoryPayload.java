package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.HandyTables;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client, after a station closed back to the inventory: the server's open menu is the
 * inventory again; show its screen straight from the station's (no close in between, so the cursor
 * stays put). Vanilla never opens the inventory screen from the server.
 */
public record ShowInventoryPayload() implements CustomPacketPayload {
  public static final ShowInventoryPayload INSTANCE = new ShowInventoryPayload();
  public static final Type<ShowInventoryPayload> TYPE =
      new Type<>(HandyTables.id("show_inventory"));
  public static final StreamCodec<ByteBuf, ShowInventoryPayload> CODEC = StreamCodec.unit(INSTANCE);

  @Override
  public Type<ShowInventoryPayload> type() {
    return TYPE;
  }
}
