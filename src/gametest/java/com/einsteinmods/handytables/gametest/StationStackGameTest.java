package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.Station;
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
 * A locked slot holding a stack of stations keeps exactly one (the one in use); the spares answer
 * vanilla's gestures as any stack would: take all, take half, shift-click, drop one or all, gather,
 * add the same item, drag, and number-key swaps that cannot move the reserved one.
 */
public final class StationStackGameTest {
  private static final int STATION_SLOT = 0;
  private static final int SPARE_SLOT = 20;
  private static final int EMPTY_MAIN_SLOT = 21;
  private static final int DIRT_SLOT = 22;
  private static final int EMPTY_HOTBAR_SLOT = 5;
  private static final int PRIMARY = AbstractContainerMenu.CONTAINER_CLICK_PRIMARY;
  private static final int SECONDARY = AbstractContainerMenu.CONTAINER_CLICK_SECONDARY;
  private static final int THROW_ONE = 0;
  private static final int THROW_ALL = 1;
  private static final int SMALL_STACK = 3;
  private static final int FULL_STACK = 64;
  private static final int ODD_STACK = 5;
  private static final int TWO = 2;
  private static final double DROP_SEARCH_RADIUS = 3.0;

  @GameTest
  public void leftClickTakesAllButOne(GameTestHelper helper) {
    for (int count : new int[] {SMALL_STACK, FULL_STACK}) {
      Fixture fixture = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, count);
      fixture.click(fixture.locked, PRIMARY, ContainerInput.PICKUP);
      fixture.expectCarried(Items.CRAFTING_TABLE, count - 1);
      fixture.expectReservedOnly();
      fixture.clearCursor();
    }
    Fixture anvil = new Fixture(helper, Items.ANVIL, Station.ANVIL, SMALL_STACK);
    anvil.click(anvil.locked, PRIMARY, ContainerInput.PICKUP);
    anvil.expectCarried(Items.ANVIL, SMALL_STACK - 1);
    anvil.expectReservedOnly();
    anvil.clearCursor();
    helper.succeed();
  }

  @GameTest
  public void rightClickTakesHalfOfTheSpares(GameTestHelper helper) {
    for (int count : new int[] {SMALL_STACK, ODD_STACK, FULL_STACK}) {
      Fixture fixture = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, count);
      int spares = count - 1;
      int half = (spares + 1) / 2;
      fixture.click(fixture.locked, SECONDARY, ContainerInput.PICKUP);
      fixture.expectCarried(Items.CRAFTING_TABLE, half);
      fixture.expectSlot(count - half);
      fixture.clearCursor();
    }
    helper.succeed();
  }

  @GameTest
  public void shiftClickMovesAllButOne(GameTestHelper helper) {
    for (int count : new int[] {SMALL_STACK, FULL_STACK}) {
      Fixture fixture = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, count);
      fixture.click(fixture.locked, PRIMARY, ContainerInput.QUICK_MOVE);
      fixture.expectReservedOnly();
      helper.assertTrue(
          fixture.total(Items.CRAFTING_TABLE) == count, "the spares moved, none was lost");
    }
    Fixture anvil = new Fixture(helper, Items.ANVIL, Station.ANVIL, SMALL_STACK);
    anvil.click(anvil.locked, PRIMARY, ContainerInput.QUICK_MOVE);
    anvil.expectReservedOnly();
    helper.succeed();
  }

  @GameTest
  public void dropKeyThrowsOneSpareAndCtrlDropAllButOne(GameTestHelper helper) {
    Fixture one = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, SMALL_STACK);
    one.click(one.locked, THROW_ONE, ContainerInput.THROW);
    one.expectSlot(SMALL_STACK - 1);
    helper.assertTrue(one.dropped(Items.CRAFTING_TABLE) == 1, "one is thrown");
    one.discardDrops();

    Fixture all = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, FULL_STACK);
    all.click(all.locked, THROW_ALL, ContainerInput.THROW);
    all.expectReservedOnly();
    helper.assertTrue(all.dropped(Items.CRAFTING_TABLE) == FULL_STACK - 1, "all but one");
    all.discardDrops();
    helper.succeed();
  }

  @GameTest
  public void doubleClickGathersTheSpares(GameTestHelper helper) {
    for (int count : new int[] {SMALL_STACK, FULL_STACK}) {
      Fixture fixture = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, count);
      fixture.inventory.setItem(SPARE_SLOT, new ItemStack(Items.CRAFTING_TABLE));
      int spare = fixture.menuSlotOf(SPARE_SLOT);
      fixture.click(spare, PRIMARY, ContainerInput.PICKUP);
      fixture.click(spare, PRIMARY, ContainerInput.PICKUP_ALL);
      fixture.expectCarried(Items.CRAFTING_TABLE, Math.min(count, FULL_STACK));
      fixture.expectReservedOnly();
      fixture.clearCursor();
    }
    helper.succeed();
  }

  @GameTest
  public void theSameItemCanBeAddedOtherItemsCannot(GameTestHelper helper) {
    Fixture fixture =
        new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, SMALL_STACK);
    fixture.menu.setCarried(new ItemStack(Items.CRAFTING_TABLE, TWO));
    fixture.click(fixture.locked, PRIMARY, ContainerInput.PICKUP);
    fixture.expectSlot(SMALL_STACK + TWO);
    helper.assertTrue(fixture.menu.getCarried().isEmpty(), "both are added");

    fixture.menu.setCarried(new ItemStack(Items.DIRT));
    fixture.click(fixture.locked, PRIMARY, ContainerInput.PICKUP);
    fixture.click(fixture.locked, SECONDARY, ContainerInput.PICKUP);
    fixture.expectSlot(SMALL_STACK + TWO);
    fixture.expectCarried(Items.DIRT, 1);
    fixture.clearCursor();

    Fixture full = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, FULL_STACK);
    full.menu.setCarried(new ItemStack(Items.CRAFTING_TABLE, TWO));
    full.click(full.locked, PRIMARY, ContainerInput.PICKUP);
    full.expectSlot(FULL_STACK);
    full.expectCarried(Items.CRAFTING_TABLE, TWO);
    full.clearCursor();
    helper.succeed();
  }

  @GameTest
  public void draggingTheSameItemAddsToTheStack(GameTestHelper helper) {
    Fixture fixture =
        new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, SMALL_STACK);
    fixture.menu.setCarried(new ItemStack(Items.CRAFTING_TABLE, TWO));
    int empty = fixture.menuSlotOf(EMPTY_MAIN_SLOT);

    fixture.drag(fixture.locked, empty);

    fixture.expectSlot(SMALL_STACK + 1);
    helper.assertTrue(fixture.inventory.getItem(EMPTY_MAIN_SLOT).getCount() == 1, "one each");
    helper.succeed();
  }

  @GameTest
  public void swapsMoveSparesButNeverTheReservedOne(GameTestHelper helper) {
    Fixture onto = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, SMALL_STACK);
    onto.click(onto.locked, EMPTY_HOTBAR_SLOT, ContainerInput.SWAP);
    onto.expectReservedOnly();
    helper.assertTrue(
        onto.inventory.getItem(EMPTY_HOTBAR_SLOT).getCount() == SMALL_STACK - 1,
        "the spares go to the number key's slot");

    Fixture from = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, SMALL_STACK);
    from.click(from.menuSlotOf(EMPTY_MAIN_SLOT), STATION_SLOT, ContainerInput.SWAP);
    from.expectReservedOnly();
    helper.assertTrue(
        from.inventory.getItem(EMPTY_MAIN_SLOT).getCount() == SMALL_STACK - 1,
        "the spares go to the hovered empty slot");

    Fixture blocked =
        new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, SMALL_STACK);
    blocked.click(blocked.menuSlotOf(DIRT_SLOT), STATION_SLOT, ContainerInput.SWAP);
    blocked.expectSlot(SMALL_STACK);
    helper.assertTrue(blocked.inventory.getItem(DIRT_SLOT).is(Items.DIRT), "the dirt stays");
    helper.succeed();
  }

  @GameTest
  public void aLoneTableTakesTablesBackButNeverGivesItsOwn(GameTestHelper helper) {
    Fixture fixture = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, 1);
    fixture.menu.setCarried(new ItemStack(Items.CRAFTING_TABLE, TWO));
    fixture.click(fixture.locked, SECONDARY, ContainerInput.PICKUP);
    fixture.expectSlot(TWO);
    fixture.expectCarried(Items.CRAFTING_TABLE, 1);
    fixture.click(fixture.locked, PRIMARY, ContainerInput.PICKUP);
    fixture.expectSlot(SMALL_STACK);
    helper.assertTrue(fixture.menu.getCarried().isEmpty(), "all placed");

    Fixture alone = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, 1);
    alone.inventory.setItem(SPARE_SLOT, new ItemStack(Items.CRAFTING_TABLE));
    int spare = alone.menuSlotOf(SPARE_SLOT);
    alone.click(spare, PRIMARY, ContainerInput.PICKUP);
    alone.click(spare, PRIMARY, ContainerInput.PICKUP_ALL);
    alone.expectReservedOnly();
    alone.expectCarried(Items.CRAFTING_TABLE, 1);
    alone.clearCursor();
    for (ContainerInput take :
        new ContainerInput[] {
          ContainerInput.PICKUP, ContainerInput.QUICK_MOVE, ContainerInput.THROW
        }) {
      alone.click(alone.locked, PRIMARY, take);
      alone.click(alone.locked, SECONDARY, take);
    }
    alone.expectReservedOnly();
    helper.assertTrue(alone.menu.getCarried().isEmpty(), "nothing taken");

    Fixture other = new Fixture(helper, Items.CRAFTING_TABLE, Station.CRAFTING_TABLE, 1);
    other.menu.setCarried(new ItemStack(Items.DIRT));
    other.click(other.locked, PRIMARY, ContainerInput.PICKUP);
    other.click(other.locked, SECONDARY, ContainerInput.PICKUP);
    other.drag(other.locked);
    other.expectReservedOnly();
    other.expectCarried(Items.DIRT, 1);
    other.clearCursor();
    helper.succeed();
  }

  /** A player with a stack of a station in slot 0 and its menu open. */
  private static final class Fixture {
    private final GameTestHelper helper;
    private final ServerPlayer player;
    private final Inventory inventory;
    private final AbstractContainerMenu menu;
    private final Item item;
    private final int locked;

    Fixture(GameTestHelper helper, Item item, Station station, int count) {
      this.helper = helper;
      this.item = item;
      this.player = HandyFixtures.player(helper, GameType.SURVIVAL);
      this.inventory = player.getInventory();
      inventory.setItem(STATION_SLOT, new ItemStack(item, count));
      inventory.setItem(DIRT_SLOT, new ItemStack(Items.DIRT));
      this.menu = HandyFixtures.open(player, station, STATION_SLOT);
      this.locked = menuSlotOf(STATION_SLOT);
    }

    void click(int slot, int button, ContainerInput input) {
      menu.clicked(slot, button, input, player);
    }

    void drag(int... slots) {
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

    void expectSlot(int count) {
      ItemStack stack = inventory.getItem(STATION_SLOT);
      helper.assertTrue(
          stack.is(item) && stack.getCount() == count, "the station slot holds " + stack);
      helper.assertTrue(player.containerMenu == menu && menu.stillValid(player), "menu open");
    }

    void expectReservedOnly() {
      expectSlot(1);
    }

    void expectCarried(Item carriedItem, int count) {
      ItemStack carried = menu.getCarried();
      helper.assertTrue(
          carried.is(carriedItem) && carried.getCount() == count, "the cursor holds " + carried);
    }

    void clearCursor() {
      menu.setCarried(ItemStack.EMPTY);
    }

    int total(Item counted) {
      int count = inventory.countItem(counted);
      for (int slot = 0; slot < menu.slots.size(); slot++) {
        if (menu.slots.get(slot).container != inventory
            && menu.slots.get(slot).getItem().is(counted)) {
          count += menu.slots.get(slot).getItem().getCount();
        }
      }
      return count;
    }

    int dropped(Item counted) {
      int count = 0;
      for (ItemEntity drop : drops(counted)) {
        count += drop.getItem().getCount();
      }
      return count;
    }

    void discardDrops() {
      for (ItemEntity drop : drops(item)) {
        drop.discard();
      }
    }

    private List<ItemEntity> drops(Item counted) {
      return helper
          .getLevel()
          .getEntitiesOfClass(
              ItemEntity.class,
              new AABB(player.blockPosition()).inflate(DROP_SEARCH_RADIUS),
              entity -> entity.getItem().is(counted));
    }

    int menuSlotOf(int inventorySlot) {
      for (int index = 0; index < menu.slots.size(); index++) {
        if (menu.slots.get(index).container == inventory
            && menu.slots.get(index).getContainerSlot() == inventorySlot) {
          return index;
        }
      }
      throw new IllegalArgumentException("no menu slot for inventory slot " + inventorySlot);
    }
  }
}
