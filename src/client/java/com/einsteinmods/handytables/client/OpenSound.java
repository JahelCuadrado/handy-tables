package com.einsteinmods.handytables.client;

import com.einsteinmods.handytables.client.config.ClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * The placing sound of the station's block when it opens from its item, as vanilla {@code
 * BlockItem#place} plays it ({@code SoundType#getPlaceSound}, volume {@code (volume + 1) / 2},
 * pitch {@code pitch * 0.8}). Only this player hears it ({@code ClientLevel#playPlayerSound}):
 * nothing is placed in the world, so nearby players get no sound for it. Played when the server
 * confirms the opening ({@code StationLockPayload}), so a refused request plays nothing. Off with
 * the "Open sound" setting.
 */
final class OpenSound {
  private static final float HALF = 2.0F;
  private static final float PLACE_PITCH_FACTOR = 0.8F;

  private OpenSound() {}

  static void play(Minecraft minecraft, ItemStack station) {
    ClientLevel level = minecraft.level;
    if (level == null || !ClientSettings.get().playsOpenSound()) {
      return;
    }
    SoundType sound = Block.byItem(station.getItem()).defaultBlockState().getSoundType();
    level.playPlayerSound(
        sound.getPlaceSound(),
        SoundSource.BLOCKS,
        (sound.getVolume() + 1.0F) / HALF,
        sound.getPitch() * PLACE_PITCH_FACTOR);
  }
}
