package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.menu.ReturnTarget;
import com.einsteinmods.handytables.menu.StationMenus;
import com.einsteinmods.handytables.network.StationReturns;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Closing a station with its own gesture goes back where it was opened from: the container block
 * (through the block's own menu provider, in reach, still there), else the inventory. The station
 * closes as vanilla closes a menu, so its inputs come back. Fabric's {@code FakePlayer#openMenu}
 * opens nothing, so the reopened menu is checked through the provider the server would open.
 */
public final class StationReturnGameTest {
  private static final int STATION_SLOT = 0;
  private static final int GRID_SLOT = 1;
  private static final int GRID_PLANKS = 3;
  private static final int CHEST_MENU_ID = 2;
  private static final int SINGLE_CHEST_ROWS = 3;
  private static final int DOUBLE_CHEST_ROWS = 6;
  private static final double FAR_AWAY = 12.0;

  /** Next to the player's feet ({@code HandyFixtures} puts the player at (0.5, 1, 0.5)). */
  private static final BlockPos CHEST = new BlockPos(1, 1, 0);

  private static final BlockPos CHEST_OTHER_HALF = CHEST.east();

  @GameTest
  public void fromTheInventoryItClosesBackToTheInventory(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu station =
        HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);
    station.getSlot(GRID_SLOT).set(new ItemStack(Items.OAK_PLANKS, GRID_PLANKS));

    StationReturns.close(player, station.containerId);

    helper.assertTrue(player.containerMenu == player.inventoryMenu, "the inventory is open");
    helper.assertTrue(
        player.getInventory().countItem(Items.OAK_PLANKS) == GRID_PLANKS, "the grid comes back");
    helper.succeed();
  }

  @GameTest
  public void aWrongMenuIdChangesNothing(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu station =
        HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    StationReturns.close(player, station.containerId + 1);

    helper.assertTrue(player.containerMenu == station, "the station stays open");
    helper.succeed();
  }

  @GameTest
  public void fromAChestInReachItReopensThatChest(GameTestHelper helper) {
    helper.setBlock(CHEST, Blocks.CHEST);
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu station = openFromChest(helper, player);

    ChestMenu reopened = reopened(helper, player, station);

    helper.assertTrue(reopened != null, "the chest opens again");
    helper.assertTrue(
        reopened.getContainer() == helper.getBlockEntity(CHEST, ChestBlockEntity.class),
        "the same chest");
    helper.assertTrue(reopened.getRowCount() == SINGLE_CHEST_ROWS, "a single chest");
    helper.succeed();
  }

  @GameTest
  public void aDoubleChestReopensWhole(GameTestHelper helper) {
    BlockState chest =
        Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
    helper.setBlock(CHEST, chest.setValue(ChestBlock.TYPE, ChestType.LEFT));
    helper.setBlock(CHEST_OTHER_HALF, chest.setValue(ChestBlock.TYPE, ChestType.RIGHT));
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu station = openFromChest(helper, player);

    ChestMenu reopened = reopened(helper, player, station);

    helper.assertTrue(
        reopened != null && reopened.getRowCount() == DOUBLE_CHEST_ROWS, "both halves open");
    helper.succeed();
  }

  @GameTest
  public void aChestOutOfReachGoesBackToTheInventory(GameTestHelper helper) {
    helper.setBlock(CHEST, Blocks.CHEST);
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu station = openFromChest(helper, player);

    player.setPos(player.position().add(new Vec3(FAR_AWAY, 0.0, 0.0)));

    helper.assertTrue(reopened(helper, player, station) == null, "out of reach");
    helper.succeed();
  }

  @GameTest
  public void aBrokenChestGoesBackToTheInventory(GameTestHelper helper) {
    helper.setBlock(CHEST, Blocks.CHEST);
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu station = openFromChest(helper, player);

    helper.setBlock(CHEST, Blocks.AIR);

    helper.assertTrue(reopened(helper, player, station) == null, "the chest is gone");
    StationReturns.close(player, station.containerId);
    helper.assertTrue(player.containerMenu == player.inventoryMenu, "the station closes anyway");
    helper.succeed();
  }

  @GameTest
  public void aStationOpenedFromAnotherReturnsWhereThatOneWould(GameTestHelper helper) {
    helper.setBlock(CHEST, Blocks.CHEST);
    ServerPlayer player = playerWithTable(helper);
    player.getInventory().setItem(STATION_SLOT + 1, new ItemStack(Items.LOOM));
    AbstractContainerMenu table = openFromChest(helper, player);

    ReturnTarget fromTable = ReturnTarget.of(table, player);
    player.doCloseContainer();
    AbstractContainerMenu loom =
        HandyFixtures.open(
            player, StationMenus.provider(Station.LOOM, STATION_SLOT + 1, fromTable));

    helper.assertTrue(reopened(helper, player, loom) != null, "back to the chest");
    helper.succeed();
  }

  /** Opens the chest at {@link #CHEST} as a click would, then the table from its screen. */
  private static AbstractContainerMenu openFromChest(GameTestHelper helper, ServerPlayer player) {
    BlockPos pos = helper.absolutePos(CHEST);
    MenuProvider chest =
        helper.getLevel().getBlockState(pos).getMenuProvider(helper.getLevel(), pos);
    helper.assertTrue(chest != null, "the chest has a menu");
    AbstractContainerMenu chestMenu = HandyFixtures.open(player, chest);
    ReturnTarget target = ReturnTarget.of(chestMenu, player);
    player.doCloseContainer();
    return HandyFixtures.open(
        player, StationMenus.provider(Station.CRAFTING_TABLE, STATION_SLOT, target));
  }

  /** The chest menu closing {@code station} would open, or null for the inventory. */
  private static @Nullable ChestMenu reopened(
      GameTestHelper helper, ServerPlayer player, AbstractContainerMenu station) {
    player.containerMenu = station;
    MenuProvider provider = StationReturns.returnProvider(player, station.containerId);
    if (provider == null) {
      return null;
    }
    AbstractContainerMenu menu = provider.createMenu(CHEST_MENU_ID, player.getInventory(), player);
    helper.assertTrue(menu instanceof ChestMenu, "a chest menu: " + menu);
    menu.removed(player);
    return (ChestMenu) menu;
  }

  private static ServerPlayer playerWithTable(GameTestHelper helper) {
    ServerPlayer player = HandyFixtures.player(helper, GameType.SURVIVAL);
    player.getInventory().setItem(STATION_SLOT, new ItemStack(Items.CRAFTING_TABLE));
    return player;
  }
}
