package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.menu.PortableAnvilMenu;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/**
 * While a station opened from its item is open, the inventory slot it came from is locked: every
 * vanilla click input aimed at it, or pulling it in from elsewhere, leaves it untouched and the
 * menu open; every other slot works as usual. Clicks go through the menu's {@code clicked}, as
 * {@code ServerGamePacketListenerImpl#handleContainerClick} sends them.
 */
public final class StationLockGameTest {
  private static final int STATION_SLOT = 0;
  private static final int OTHER_HOTBAR_SLOT = 4;
  private static final int EMPTY_MAIN_SLOT = 20;
  private static final int DIRT_SLOT = 12;
  private static final int SPARE_TABLES_SLOT = 15;
  private static final int PRIMARY = AbstractContainerMenu.CONTAINER_CLICK_PRIMARY;
  private static final int SECONDARY = AbstractContainerMenu.CONTAINER_CLICK_SECONDARY;
  private static final int THROW_ONE = 0;
  private static final int THROW_ALL = 1;
  private static final int HOTBAR_SIZE = 9;
  private static final int CRAFTING_RESULT = 0;
  private static final int[] CRAFTING_GRID_2X2 = {1, 2, 4, 5};
  private static final int GRID_SLOT = 1;
  private static final int GRID_PLANKS = 5;
  private static final int ANVIL_INPUT = 0;
  private static final int FULL_STACK = 64;
  private static final int TWO = 2;
  private static final double DROP_SEARCH_RADIUS = 3.0;

  @GameTest
  public void craftingTableSlotRefusesEveryInputInSurvival(GameTestHelper helper) {
    refusesEveryInput(helper, GameType.SURVIVAL, Station.CRAFTING_TABLE, Items.CRAFTING_TABLE);
  }

  @GameTest
  public void craftingTableSlotRefusesEveryInputInCreative(GameTestHelper helper) {
    refusesEveryInput(helper, GameType.CREATIVE, Station.CRAFTING_TABLE, Items.CRAFTING_TABLE);
  }

  @GameTest
  public void anvilSlotRefusesEveryInputInSurvival(GameTestHelper helper) {
    refusesEveryInput(helper, GameType.SURVIVAL, Station.ANVIL, Items.ANVIL);
  }

  @GameTest
  public void anvilSlotRefusesEveryInputInCreative(GameTestHelper helper) {
    refusesEveryInput(helper, GameType.CREATIVE, Station.ANVIL, Items.ANVIL);
  }

  @GameTest
  public void otherSlotsStillWork(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.CRAFTING_TABLE);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);
    int dirt = menuSlotOf(menu, player, DIRT_SLOT);
    int empty = menuSlotOf(menu, player, EMPTY_MAIN_SLOT);

    menu.clicked(dirt, PRIMARY, ContainerInput.PICKUP, player);
    helper.assertTrue(menu.getCarried().is(Items.DIRT), "the dirt is picked up");
    menu.clicked(empty, PRIMARY, ContainerInput.PICKUP, player);
    helper.assertTrue(
        player.getInventory().getItem(EMPTY_MAIN_SLOT).is(Items.DIRT), "and put down elsewhere");
    menu.clicked(empty, OTHER_HOTBAR_SLOT, ContainerInput.SWAP, player);
    helper.assertTrue(
        player.getInventory().getItem(OTHER_HOTBAR_SLOT).is(Items.DIRT), "number keys still swap");
    expectLocked(helper, player, menu, Items.CRAFTING_TABLE, 1);
    helper.succeed();
  }

  @GameTest
  public void craftedTablesMergeIntoTheLockedSlot(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.CRAFTING_TABLE);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);
    for (int gridSlot : CRAFTING_GRID_2X2) {
      menu.getSlot(gridSlot).set(new ItemStack(Items.OAK_PLANKS));
    }

    menu.clicked(CRAFTING_RESULT, PRIMARY, ContainerInput.QUICK_MOVE, player);

    expectLocked(helper, player, menu, Items.CRAFTING_TABLE, TWO);
    helper.succeed();
  }

  @GameTest
  public void removingTheStationStillClosesAndGivesTheGridBack(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.CRAFTING_TABLE);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);
    menu.getSlot(GRID_SLOT).set(new ItemStack(Items.OAK_PLANKS, GRID_PLANKS));

    player.getInventory().clearContent();

    helper.assertFalse(menu.stillValid(player), "a /clear closes the menu");
    player.closeContainer();
    helper.assertTrue(
        player.getInventory().countItem(Items.OAK_PLANKS) == GRID_PLANKS, "the grid comes back");
    helper.succeed();
  }

  @GameTest
  public void anvilWearFromAStackMovesTheLockToTheWornAnvil(GameTestHelper helper) {
    ServerPlayer player = HandyFixtures.player(helper, GameType.SURVIVAL);
    Inventory inventory = player.getInventory();
    inventory.setItem(STATION_SLOT, new ItemStack(Items.ANVIL, FULL_STACK));
    PortableAnvilMenu menu =
        (PortableAnvilMenu) HandyFixtures.open(player, Station.ANVIL, STATION_SLOT);

    menu.afterUse(player, true);

    int worn = inventory.findSlotMatchingItem(new ItemStack(Items.CHIPPED_ANVIL));
    helper.assertTrue(worn == STATION_SLOT + 1, "the chipped one goes to the first free slot");
    expectLockedAt(helper, player, menu, worn, Items.CHIPPED_ANVIL, 1);
    int stack = menuSlotOf(menu, player, STATION_SLOT);
    menu.clicked(stack, PRIMARY, ContainerInput.PICKUP, player);
    helper.assertTrue(
        menu.getCarried().is(Items.ANVIL) && menu.getCarried().getCount() == FULL_STACK - 1,
        "the rest of the stack is a plain slot again");
    menu.clicked(stack, PRIMARY, ContainerInput.PICKUP, player);
    helper.assertTrue(menu.stillValid(player), "the menu stays open");
    helper.succeed();
  }

  @GameTest
  public void theWornAnvilKeepsWearingUntilItBreaks(GameTestHelper helper) {
    ServerPlayer player = HandyFixtures.player(helper, GameType.SURVIVAL);
    Inventory inventory = player.getInventory();
    inventory.setItem(STATION_SLOT, new ItemStack(Items.ANVIL, FULL_STACK));
    PortableAnvilMenu menu =
        (PortableAnvilMenu) HandyFixtures.open(player, Station.ANVIL, STATION_SLOT);
    int worn = STATION_SLOT + 1;

    menu.afterUse(player, true);
    menu.afterUse(player, true);

    expectLockedAt(helper, player, menu, worn, Items.DAMAGED_ANVIL, 1);
    helper.assertTrue(
        inventory.getItem(STATION_SLOT).getCount() == FULL_STACK - 1, "the stack is not worn");
    menu.afterUse(player, true);
    helper.assertTrue(inventory.getItem(worn).isEmpty(), "the anvil in use broke");
    helper.assertFalse(menu.stillValid(player), "so the menu closes");
    helper.assertTrue(
        inventory.getItem(STATION_SLOT).getCount() == FULL_STACK - 1, "the stack is untouched");
    helper.succeed();
  }

  @GameTest
  public void aWornAnvilDroppedFromAFullInventoryClosesTheMenu(GameTestHelper helper) {
    ServerPlayer player = HandyFixtures.player(helper, GameType.SURVIVAL);
    Inventory inventory = player.getInventory();
    for (int slot = 0; slot < InventorySlots.MAIN_SIZE; slot++) {
      inventory.setItem(slot, new ItemStack(Items.DIRT, FULL_STACK));
    }
    inventory.setItem(STATION_SLOT, new ItemStack(Items.ANVIL, FULL_STACK));
    PortableAnvilMenu menu =
        (PortableAnvilMenu) HandyFixtures.open(player, Station.ANVIL, STATION_SLOT);
    menu.getSlot(ANVIL_INPUT).set(new ItemStack(Items.DIAMOND_SWORD));

    menu.afterUse(player, true);

    helper.assertTrue(
        droppedCount(helper, player, Items.CHIPPED_ANVIL) == 1, "the chipped one is dropped");
    helper.assertTrue(
        inventory.getItem(STATION_SLOT).getCount() == FULL_STACK - 1, "the rest stays");
    helper.assertFalse(menu.stillValid(player), "the anvil in use left: the menu closes");
    player.closeContainer();
    helper.assertTrue(
        droppedCount(helper, player, Items.DIAMOND_SWORD) == 1,
        "the input comes back (dropped: the inventory is full)");
    helper.succeed();
  }

  /** Sends every vanilla input against the locked slot; it never changes and the menu stays. */
  private static void refusesEveryInput(
      GameTestHelper helper, GameType gameType, Station station, Item stationItem) {
    ServerPlayer player = playerWith(helper, gameType, stationItem);
    Inventory inventory = player.getInventory();
    for (int slot = 1; slot < HOTBAR_SIZE; slot++) {
      inventory.setItem(slot, new ItemStack(Items.STICK));
    }
    inventory.setItem(InventorySlots.OFFHAND, new ItemStack(Items.SHIELD));
    AbstractContainerMenu menu = HandyFixtures.open(player, station, STATION_SLOT);
    int locked = menuSlotOf(menu, player, STATION_SLOT);
    int empty = menuSlotOf(menu, player, EMPTY_MAIN_SLOT);

    menu.clicked(locked, PRIMARY, ContainerInput.PICKUP, player);
    menu.clicked(locked, SECONDARY, ContainerInput.PICKUP, player);
    menu.clicked(locked, PRIMARY, ContainerInput.QUICK_MOVE, player);
    menu.clicked(locked, THROW_ONE, ContainerInput.THROW, player);
    menu.clicked(locked, THROW_ALL, ContainerInput.THROW, player);
    for (int hotbar = 0; hotbar < HOTBAR_SIZE; hotbar++) {
      menu.clicked(locked, hotbar, ContainerInput.SWAP, player);
    }
    menu.clicked(locked, InventorySlots.OFFHAND, ContainerInput.SWAP, player);
    menu.clicked(empty, STATION_SLOT, ContainerInput.SWAP, player);
    expectLocked(helper, player, menu, stationItem, 1);
    helper.assertTrue(menu.getCarried().isEmpty(), "nothing reached the cursor");

    // Carrying the same item: no gathering from it; placing onto it adds, taking leaves one.
    inventory.setItem(SPARE_TABLES_SLOT, new ItemStack(stationItem, TWO));
    int spare = menuSlotOf(menu, player, SPARE_TABLES_SLOT);
    menu.clicked(spare, PRIMARY, ContainerInput.PICKUP, player);
    menu.clicked(locked, PRIMARY, ContainerInput.PICKUP_ALL, player);
    menu.clicked(empty, PRIMARY, ContainerInput.PICKUP_ALL, player);
    expectLocked(helper, player, menu, stationItem, 1);
    helper.assertTrue(menu.getCarried().getCount() == TWO, "nothing gathered from it");
    menu.clicked(locked, PRIMARY, ContainerInput.PICKUP, player);
    expectLocked(helper, player, menu, stationItem, 1 + TWO);
    menu.clicked(locked, PRIMARY, ContainerInput.PICKUP, player);
    expectLocked(helper, player, menu, stationItem, 1);
    drag(menu, player, locked, empty);
    expectLocked(helper, player, menu, stationItem, TWO);
    helper.assertTrue(inventory.getItem(EMPTY_MAIN_SLOT).getCount() == 1, "a drag adds one each");
    menu.clicked(empty, PRIMARY, ContainerInput.PICKUP, player);
    menu.clicked(locked, PRIMARY, ContainerInput.PICKUP, player);
    menu.clicked(locked, PRIMARY, ContainerInput.PICKUP, player);
    expectLocked(helper, player, menu, stationItem, 1);
    menu.setCarried(ItemStack.EMPTY);

    // Carrying something else: no replacing it.
    menu.setCarried(new ItemStack(Items.DIRT));
    menu.clicked(locked, PRIMARY, ContainerInput.PICKUP, player);
    expectLocked(helper, player, menu, stationItem, 1);
    helper.assertTrue(menu.getCarried().is(Items.DIRT), "the dirt stays on the cursor");
    menu.setCarried(ItemStack.EMPTY);
    helper.succeed();
  }

  /** A left-button drag (vanilla {@code QUICK_CRAFT} start, one add per slot, end). */
  private static void drag(AbstractContainerMenu menu, ServerPlayer player, int... slots) {
    menu.clicked(
        AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
        AbstractContainerMenu.getQuickcraftMask(
            AbstractContainerMenu.QUICKCRAFT_HEADER_START,
            AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE),
        ContainerInput.QUICK_CRAFT,
        player);
    for (int slot : slots) {
      menu.clicked(
          slot,
          AbstractContainerMenu.getQuickcraftMask(
              AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE,
              AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE),
          ContainerInput.QUICK_CRAFT,
          player);
    }
    menu.clicked(
        AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
        AbstractContainerMenu.getQuickcraftMask(
            AbstractContainerMenu.QUICKCRAFT_HEADER_END,
            AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE),
        ContainerInput.QUICK_CRAFT,
        player);
  }

  private static void expectLocked(
      GameTestHelper helper,
      ServerPlayer player,
      AbstractContainerMenu menu,
      Item stationItem,
      int count) {
    ItemStack station = player.getInventory().getItem(STATION_SLOT);
    helper.assertTrue(
        station.is(stationItem) && station.getCount() == count,
        "the station slot is unchanged: " + station);
    helper.assertTrue(player.containerMenu == menu && menu.stillValid(player), "the menu is open");
  }

  private static void expectLockedAt(
      GameTestHelper helper,
      ServerPlayer player,
      AbstractContainerMenu menu,
      int inventorySlot,
      Item item,
      int count) {
    int locked = menuSlotOf(menu, player, inventorySlot);
    menu.clicked(locked, PRIMARY, ContainerInput.PICKUP, player);
    menu.clicked(locked, THROW_ALL, ContainerInput.THROW, player);
    ItemStack stack = player.getInventory().getItem(inventorySlot);
    helper.assertTrue(
        stack.is(item) && stack.getCount() == count && menu.getCarried().isEmpty(),
        "slot " + inventorySlot + " is locked: " + stack);
  }

  /** The menu index showing {@code inventorySlot} of the player's inventory. */
  private static int menuSlotOf(
      AbstractContainerMenu menu, ServerPlayer player, int inventorySlot) {
    for (int index = 0; index < menu.slots.size(); index++) {
      if (menu.slots.get(index).container == player.getInventory()
          && menu.slots.get(index).getContainerSlot() == inventorySlot) {
        return index;
      }
    }
    throw new IllegalArgumentException("no menu slot for inventory slot " + inventorySlot);
  }

  private static int droppedCount(GameTestHelper helper, ServerPlayer player, Item item) {
    List<ItemEntity> drops =
        helper
            .getLevel()
            .getEntitiesOfClass(
                ItemEntity.class,
                new AABB(player.blockPosition()).inflate(DROP_SEARCH_RADIUS),
                entity -> entity.getItem().is(item));
    int count = 0;
    for (ItemEntity drop : drops) {
      count += drop.getItem().getCount();
    }
    return count;
  }

  private static ServerPlayer playerWith(GameTestHelper helper, GameType gameType, Item station) {
    ServerPlayer player = HandyFixtures.player(helper, gameType);
    player.getInventory().setItem(STATION_SLOT, new ItemStack(station));
    player.getInventory().setItem(DIRT_SLOT, new ItemStack(Items.DIRT));
    return player;
  }
}
