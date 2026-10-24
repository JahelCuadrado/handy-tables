package com.einsteinmods.handytables.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.core.StationPolicy;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;

class PayloadCodecTest {
  private static final int CONTAINER_ID = 42;

  private static <T> T roundTrip(StreamCodec<FriendlyByteBuf, T> codec, T payload) {
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    codec.encode(buffer, payload);
    return codec.decode(buffer);
  }

  @Test
  void openStationKeepsTheInventorySlot() {
    OpenStationPayload payload = new OpenStationPayload(InventorySlots.OFFHAND);

    assertEquals(payload, roundTrip(OpenStationPayload.CODEC, payload));
  }

  @Test
  void serverPolicyKeepsEveryStationBit() {
    ServerPolicyPayload payload =
        new ServerPolicyPayload(StationPolicy.UNRESTRICTED.with(Station.CARTOGRAPHY_TABLE, false));

    assertEquals(payload, roundTrip(ServerPolicyPayload.CODEC, payload));
  }

  @Test
  void stationLockKeepsMenuSlotAndStation() {
    StationLockPayload payload =
        new StationLockPayload(CONTAINER_ID, InventorySlots.OFFHAND, Station.ANVIL.id());

    assertEquals(payload, roundTrip(StationLockPayload.CODEC, payload));
  }
}
