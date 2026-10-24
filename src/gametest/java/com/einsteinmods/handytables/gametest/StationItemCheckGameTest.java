package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/**
 * A station opened from its item stays open while the player has the item, wherever it moves, and
 * closes (giving the inputs back through vanilla {@code removed}) once it is gone. On death it
 * behaves as a placed station: it closes when the body is removed and the inputs drop there.
 */
public final class StationItemCheckGameTest {
  private static final int STATION_SLOT = 3;
  private static final int OTHER_SLOT = 20;
  private static final int GRID_SLOT = 1;
  private static final int GRID_PLANKS = 5;
  private static final double DROP_SEARCH_RADIUS = 3.0;

  @GameTest
  public void validWhileTheItemStays(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    helper.assertTrue(menu.stillValid(player), "the table is in its slot");
    helper.succeed();
  }

  @GameTest
  public void validWhenTheItemMovesToAnotherSlot(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    moveTable(player, OTHER_SLOT);
    helper.assertTrue(menu.stillValid(player), "the table moved within the inventory");
    moveTable(player, InventorySlots.OFFHAND);
    helper.assertTrue(menu.stillValid(player), "the table moved to the offhand");
    helper.succeed();
  }

  @GameTest
  public void validWhileTheItemIsCarried(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    menu.setCarried(player.getInventory().removeItemNoUpdate(STATION_SLOT));

    helper.assertTrue(menu.stillValid(player), "the table is on the cursor");
    helper.succeed();
  }

  @GameTest
  public void invalidOnceTheItemIsGone(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    player.getInventory().removeItemNoUpdate(STATION_SLOT);

    helper.assertFalse(menu.stillValid(player), "no table left");
    helper.succeed();
  }

  @GameTest
  public void otherStationItemsDoNotKeepItOpen(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    player.getInventory().setItem(STATION_SLOT, new ItemStack(Items.LOOM));

    helper.assertFalse(menu.stillValid(player), "a loom is no crafting table");
    helper.succeed();
  }

  @GameTest
  public void closingGivesTheGridBack(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);
    menu.getSlot(GRID_SLOT).set(new ItemStack(Items.OAK_PLANKS, GRID_PLANKS));

    player.closeContainer();

    helper.assertTrue(menu.getSlot(GRID_SLOT).getItem().isEmpty(), "the grid is emptied");
    helper.assertTrue(
        player.getInventory().countItem(Items.OAK_PLANKS) == GRID_PLANKS,
        "the planks are back in the inventory");
    helper.assertTrue(player.containerMenu == player.inventoryMenu, "the inventory is open again");
    helper.succeed();
  }

  @GameTest
  public void deathDropsTheInputsAtTheDeathSpotAsVanilla(GameTestHelper helper) {
    ServerPlayer player = playerWithTable(helper);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);
    menu.getSlot(GRID_SLOT).set(new ItemStack(Items.OAK_PLANKS, GRID_PLANKS));

    player.setHealth(0.0F);
    player.die(helper.getLevel().damageSources().generic());

    helper.assertTrue(player.getInventory().isEmpty(), "the inventory drops on death");
    helper.assertTrue(menu.stillValid(player), "the menu stays until the body is removed");
    player.remove(Entity.RemovalReason.KILLED);
    helper.assertTrue(player.containerMenu == player.inventoryMenu, "then it closes");
    helper.assertTrue(
        droppedPlanks(helper, player) == GRID_PLANKS, "and the grid drops at the death spot");
    helper.succeed();
  }

  private static int droppedPlanks(GameTestHelper helper, ServerPlayer player) {
    int count = 0;
    for (ItemEntity drop :
        helper
            .getLevel()
            .getEntitiesOfClass(
                ItemEntity.class,
                new AABB(player.blockPosition()).inflate(DROP_SEARCH_RADIUS),
                entity -> entity.getItem().is(Items.OAK_PLANKS))) {
      count += drop.getItem().getCount();
    }
    return count;
  }

  private static ServerPlayer playerWithTable(GameTestHelper helper) {
    ServerPlayer player = HandyFixtures.player(helper, GameType.SURVIVAL);
    player.getInventory().setItem(STATION_SLOT, new ItemStack(Items.CRAFTING_TABLE));
    return player;
  }

  private static void moveTable(ServerPlayer player, int targetSlot) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (stack.is(Items.CRAFTING_TABLE)) {
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.getInventory().setItem(targetSlot, stack);
        return;
      }
    }
  }
}
