package com.einsteinmods.handytables.menu;

import com.einsteinmods.handytables.mixin.CompoundContainerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Where closing a station with its own gesture goes back to: the container block whose screen it
 * was opened from, or the player's inventory. Server side.
 *
 * <p>The block is found through the open menu's slots: a container block's menu shows the block
 * entity itself ({@code ChestMenu}, {@code ShulkerBoxMenu}, furnaces, hoppers, dispensers, brewing
 * stands, crafters...), a double chest a {@code CompoundContainer} of its two halves. Anything else
 * (the inventory, the creative inventory, entity containers, the ender chest, placed crafting
 * tables and other menus without a stored container) returns to the inventory. A station opened
 * from another station's screen returns where that one would have.
 */
public final class ReturnTarget {
  /** Back to the player's inventory screen. */
  public static final ReturnTarget INVENTORY = new ReturnTarget(null);

  private final @Nullable BlockEntity blockEntity;

  private ReturnTarget(@Nullable BlockEntity blockEntity) {
    this.blockEntity = blockEntity;
  }

  /** Where to return from a station opened while {@code previous} was the player's open menu. */
  public static ReturnTarget of(AbstractContainerMenu previous, Player player) {
    if (previous == player.inventoryMenu) {
      return INVENTORY;
    }
    if (previous instanceof PortableStationMenu station) {
      return station.stationCheck().returnTarget();
    }
    for (Slot slot : previous.slots) {
      BlockEntity blockEntity = blockEntityOf(slot.container);
      if (blockEntity != null) {
        return new ReturnTarget(blockEntity);
      }
    }
    return INVENTORY;
  }

  /**
   * The block's own menu provider when its screen may open again, as a click on the block would
   * ({@code BlockState#getMenuProvider}: a blocked chest gives none, a double chest both halves);
   * null to go back to the inventory. The block entity must still be the one in the level and in
   * reach, the rule vanilla menus keep checking ({@code Container#stillValidBlockEntity}).
   */
  public @Nullable MenuProvider reopen(ServerPlayer player) {
    BlockEntity target = blockEntity;
    if (target == null) {
      return null;
    }
    Level level = target.getLevel();
    if (level == null
        || level != player.level()
        || target.isRemoved()
        || !Container.stillValidBlockEntity(target, player)) {
      return null;
    }
    BlockPos pos = target.getBlockPos();
    return level.getBlockState(pos).getMenuProvider(level, pos);
  }

  private static @Nullable BlockEntity blockEntityOf(Container container) {
    if (container instanceof BlockEntity blockEntity) {
      return blockEntity;
    }
    if (container instanceof CompoundContainer compound
        && ((Object) compound instanceof CompoundContainerAccessor halves)
        && halves.handytables$first() instanceof BlockEntity blockEntity) {
      return blockEntity;
    }
    return null;
  }
}
