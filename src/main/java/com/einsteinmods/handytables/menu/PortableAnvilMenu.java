package com.einsteinmods.handytables.menu;

import com.einsteinmods.handytables.core.AnvilWear;
import com.einsteinmods.handytables.network.StationLockSync;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;

/**
 * Vanilla anvil menu opened from an anvil item (any wear state). Cost, "too expensive", repair cost
 * and renaming ({@code ServerGamePacketListenerImpl#handleRenameItem} targets any {@code
 * AnvilMenu}) stay vanilla; two things differ:
 *
 * <ul>
 *   <li>{@code stillValid}: the anvil item instead of the block in reach ({@code
 *       ItemCombinerMenu#stillValid}), see {@link StationItemCheck}; false once the anvil broke (or
 *       a worn one split off its stack had to be dropped), so {@code ServerPlayer#tick} closes the
 *       menu and {@code removed} gives the inputs back, as when a placed anvil breaks.
 *   <li>wear: vanilla {@code AnvilMenu#onTake} ends with {@code access.execute} that damages the
 *       anvil block at the access position. Here that block step is skipped (the access is paused
 *       for it, so a real anvil at the player's feet is never touched) and the same roll wears the
 *       anvil item instead ({@link AnvilItemWear}), with the same sounds at the player.
 * </ul>
 */
public final class PortableAnvilMenu extends AnvilMenu implements PortableStationMenu {
  private static final int NO_EVENT_DATA = 0;

  private final StationItemCheck check;
  private final StationLock lock;
  private final PausableLevelAccess levelAccess;
  private boolean anvilGone;

  /** Uses taken during the current click, worn right after it. */
  private int pendingUses;

  public PortableAnvilMenu(
      int containerId, Inventory inventory, ContainerLevelAccess access, StationItemCheck check) {
    this(containerId, inventory, new PausableLevelAccess(access), check);
  }

  private PortableAnvilMenu(
      int containerId, Inventory inventory, PausableLevelAccess access, StationItemCheck check) {
    super(containerId, inventory, access);
    this.levelAccess = access;
    this.check = check;
    this.lock = StationLock.install(this, inventory, check.station(), check.openedSlot());
  }

  @Override
  public StationLock stationLock() {
    return lock;
  }

  @Override
  public StationItemCheck stationCheck() {
    return check;
  }

  @Override
  public boolean stillValid(Player player) {
    return !anvilGone && check.holds(player, getCarried());
  }

  /** Refuses or guards clicks on the locked station slot, see {@link StationLock}. */
  @Override
  public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
    Inventory inventory = player.getInventory();
    if (!lock.refuses(this, slotIndex, buttonNum, containerInput, inventory)) {
      lock.clickWithoutReserved(
          inventory,
          containerInput,
          () -> super.clicked(slotIndex, buttonNum, containerInput, player));
      for (; pendingUses > 0; pendingUses--) {
        afterUse(player, rollsWear(player));
      }
    }
  }

  /**
   * Vanilla {@code onTake} (XP, inputs, name filter) without its block step. The item wear runs
   * once the click is over ({@link #clicked}): during it a stacked anvil's reserved item is set
   * aside, and the wear must see the whole stack to split the worn one off.
   */
  @Override
  protected void onTake(Player player, ItemStack carried) {
    levelAccess.paused = true;
    try {
      super.onTake(player, carried);
    } finally {
      levelAccess.paused = false;
    }
    pendingUses++;
  }

  /**
   * Vanilla's roll: never with infinite materials (creative), else {@code
   * player.getRandom().nextFloat()} against {@link AnvilWear#CHANCE_PER_USE}.
   */
  public static boolean rollsWear(Player player) {
    return !player.hasInfiniteMaterials() && AnvilWear.wears(player.getRandom().nextFloat());
  }

  /**
   * After one use: wears the anvil item when {@code wears}, then plays vanilla's level event at the
   * player ({@code SOUND_ANVIL_BROKEN} when it broke, else {@code SOUND_ANVIL_USED}). Public so
   * GameTests can force the wear.
   *
   * <p>A worn anvil split off its stack is the anvil in use from then on: the lock moves to its
   * slot (the rest of the stack is a plain slot again) and the client is told, so its lock and
   * padlock follow. When it had to be dropped (full inventory) the anvil in use has left the
   * inventory, as when it breaks: the menu closes and {@code removed} gives the inputs back.
   */
  public void afterUse(Player player, boolean wears) {
    AnvilItemWear.Outcome outcome =
        wears ? AnvilItemWear.wear(player, check, this) : AnvilItemWear.Outcome.STAYED;
    if (outcome == AnvilItemWear.Outcome.BROKE || outcome == AnvilItemWear.Outcome.DROPPED) {
      anvilGone = true;
    } else if (outcome == AnvilItemWear.Outcome.MOVED) {
      Inventory inventory = player.getInventory();
      lock.moveTo(this, inventory, check.slotOf(inventory));
      if (player instanceof ServerPlayer serverPlayer) {
        StationLockSync.send(serverPlayer, this, lock);
      }
    }
    boolean broke = outcome == AnvilItemWear.Outcome.BROKE;
    player
        .level()
        .levelEvent(
            broke ? LevelEvent.SOUND_ANVIL_BROKEN : LevelEvent.SOUND_ANVIL_USED,
            player.blockPosition(),
            NO_EVENT_DATA);
  }

  /**
   * The real access, except while {@link #paused}: then it acts as {@code
   * ContainerLevelAccess.NULL} (no level, nothing runs). Only {@code onTake} pauses it; {@code
   * removed} still clears the inputs through the real one.
   */
  private static final class PausableLevelAccess implements ContainerLevelAccess {
    private final ContainerLevelAccess delegate;
    private boolean paused;

    PausableLevelAccess(ContainerLevelAccess delegate) {
      this.delegate = delegate;
    }

    @Override
    public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> action) {
      return paused ? Optional.empty() : delegate.evaluate(action);
    }
  }
}
