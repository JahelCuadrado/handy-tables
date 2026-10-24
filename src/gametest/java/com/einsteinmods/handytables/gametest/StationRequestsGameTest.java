package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.InventorySlots;
import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.core.StationPolicy;
import com.einsteinmods.handytables.menu.PortableAnvilMenu;
import com.einsteinmods.handytables.menu.PortableCraftingMenu;
import com.einsteinmods.handytables.menu.PortableSmithingMenu;
import com.einsteinmods.handytables.network.StationRequests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** What the server accepts in an {@code OpenStationPayload}. */
public final class StationRequestsGameTest {
  private static final int STATION_SLOT = 7;
  private static final int FIRST_ARMOR_SLOT = 36;
  private static final int BEYOND_INVENTORY = 99;

  @GameTest
  public void stationInTheInventoryOpens(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.CRAFTING_TABLE);

    MenuProvider provider =
        StationRequests.provider(player, STATION_SLOT, StationPolicy.UNRESTRICTED);

    helper.assertTrue(provider != null, "a crafting table in the inventory opens");
    AbstractContainerMenu menu = HandyFixtures.open(player, provider);
    helper.assertTrue(menu instanceof PortableCraftingMenu, "as a crafting menu");
    helper.succeed();
  }

  @GameTest
  public void stationInTheOffhandOpensInCreative(GameTestHelper helper) {
    ServerPlayer player = HandyFixtures.player(helper, GameType.CREATIVE);
    player.getInventory().setItem(InventorySlots.OFFHAND, new ItemStack(Items.SMITHING_TABLE));

    MenuProvider provider =
        StationRequests.provider(player, InventorySlots.OFFHAND, StationPolicy.UNRESTRICTED);

    helper.assertTrue(provider != null, "the offhand slot is accepted");
    helper.assertTrue(
        HandyFixtures.open(player, provider) instanceof PortableSmithingMenu, "a smithing menu");
    helper.succeed();
  }

  @GameTest
  public void badSlotsAreRejected(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.CRAFTING_TABLE);
    player.getInventory().setItem(FIRST_ARMOR_SLOT, new ItemStack(Items.CRAFTING_TABLE));

    for (int slot : new int[] {-1, FIRST_ARMOR_SLOT, BEYOND_INVENTORY}) {
      helper.assertTrue(
          StationRequests.provider(player, slot, StationPolicy.UNRESTRICTED) == null,
          "slot " + slot + " is rejected");
    }
    helper.succeed();
  }

  @GameTest
  public void nonStationItemsAreRejected(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.FURNACE);

    helper.assertTrue(
        StationRequests.provider(player, STATION_SLOT, StationPolicy.UNRESTRICTED) == null,
        "a furnace is no station");
    helper.assertTrue(
        StationRequests.provider(player, STATION_SLOT + 1, StationPolicy.UNRESTRICTED) == null,
        "an empty slot is no station");
    helper.succeed();
  }

  @GameTest
  public void stationsForbiddenByPolicyAreRejected(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.LOOM);
    StationPolicy noLoom = StationPolicy.UNRESTRICTED.with(Station.LOOM, false);

    helper.assertTrue(
        StationRequests.provider(player, STATION_SLOT, noLoom) == null, "the loom is forbidden");
    helper.succeed();
  }

  @GameTest
  public void aForbiddenGrindstoneIsRejected(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.GRINDSTONE);
    StationPolicy noGrindstone = StationPolicy.UNRESTRICTED.with(Station.GRINDSTONE, false);

    helper.assertTrue(
        StationRequests.provider(player, STATION_SLOT, noGrindstone) == null,
        "the grindstone is forbidden");
    helper.assertTrue(
        StationRequests.provider(player, STATION_SLOT, StationPolicy.UNRESTRICTED) != null,
        "and opens when allowed");
    helper.succeed();
  }

  @GameTest
  public void everyAnvilOpensUnlessForbidden(GameTestHelper helper) {
    StationPolicy noAnvil = StationPolicy.UNRESTRICTED.with(Station.ANVIL, false);
    for (Item anvil : new Item[] {Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL}) {
      ServerPlayer player = playerWith(helper, GameType.SURVIVAL, anvil);
      MenuProvider provider =
          StationRequests.provider(player, STATION_SLOT, StationPolicy.UNRESTRICTED);

      helper.assertTrue(provider != null, anvil + " opens when allowed");
      helper.assertTrue(
          HandyFixtures.open(player, provider) instanceof PortableAnvilMenu, "as an anvil menu");
      helper.assertTrue(
          StationRequests.provider(player, STATION_SLOT, noAnvil) == null,
          anvil + " is rejected when forbidden");
    }
    helper.succeed();
  }

  @GameTest
  public void spectatorsAreRejected(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SPECTATOR, Items.CRAFTING_TABLE);

    helper.assertTrue(
        StationRequests.provider(player, STATION_SLOT, StationPolicy.UNRESTRICTED) == null,
        "spectators cannot open stations");
    helper.succeed();
  }

  @GameTest
  public void aCarriedItemIsRejected(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.CRAFTING_TABLE);
    player.containerMenu.setCarried(new ItemStack(Items.DIRT));

    helper.assertTrue(
        StationRequests.provider(player, STATION_SLOT, StationPolicy.UNRESTRICTED) == null,
        "the cursor must be empty");
    player.containerMenu.setCarried(ItemStack.EMPTY);
    helper.succeed();
  }

  @GameTest
  public void openingFromTheInventoryGivesThe2x2GridBack(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, Items.CRAFTING_TABLE);
    player.containerMenu = player.inventoryMenu;
    player.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(Items.OAK_PLANKS));

    StationRequests.open(player, STATION_SLOT, StationPolicy.UNRESTRICTED);

    helper.assertTrue(
        player.inventoryMenu.getCraftSlots().isEmpty(), "the 2x2 grid is emptied first");
    helper.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS) == 1, "into the inventory");
    helper.succeed();
  }

  private static ServerPlayer playerWith(GameTestHelper helper, GameType gameType, Item item) {
    ServerPlayer player = HandyFixtures.player(helper, gameType);
    player.getInventory().setItem(STATION_SLOT, new ItemStack(item));
    return player;
  }
}
