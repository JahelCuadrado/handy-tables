package com.einsteinmods.handytables.network;

import com.einsteinmods.handytables.HandyTables;
import com.einsteinmods.handytables.core.StationPolicy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client, on join: stations this player may open from their item, as {@link
 * StationPolicy#allowedMask()}. Its arrival also tells the client the server has the mod; until
 * then every middle click stays vanilla.
 */
public record ServerPolicyPayload(StationPolicy policy) implements CustomPacketPayload {
  public static final Type<ServerPolicyPayload> TYPE = new Type<>(HandyTables.id("policy"));

  public static final StreamCodec<FriendlyByteBuf, ServerPolicyPayload> CODEC =
      ByteBufCodecs.VAR_INT
          .<FriendlyByteBuf>cast()
          .map(
              mask -> new ServerPolicyPayload(new StationPolicy(mask)),
              payload -> payload.policy().allowedMask());

  @Override
  public Type<ServerPolicyPayload> type() {
    return TYPE;
  }
}
