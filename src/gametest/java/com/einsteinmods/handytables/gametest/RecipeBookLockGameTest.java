package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.menu.StationLock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;

/**
 * The recipe book never uses the crafting table in use (one item of the locked slot): the crafter
 * (iron, crafting table, redstone, dropper) is placed only with another table to take, the rest of
 * a locked stack included, and the count the client shows agrees. Placement runs as {@code
 * ServerGamePacketListenerImpl#handlePlaceRecipe} calls it.
 */
public final class RecipeBookLockGameTest {
  private static final int STATION_SLOT = 0;
  private static final int FREE_TABLE_SLOT = 20;
  private static final int IRON_SLOT = 10;
  private static final int REDSTONE_SLOT = 11;
  private static final int DROPPER_SLOT = 12;
  private static final int IRON = 5;
  private static final int REDSTONE = 2;
  private static final int TWO_TABLES = 2;
  private static final int CRAFTING_RESULT = 0;
  private static final int FIRST_GRID_SLOT = 1;
  private static final int LAST_GRID_SLOT = 9;
  private static final int CONTAINER_ID = 2;
  private static final ResourceKey<Recipe<?>> CRAFTER =
      ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("crafter"));

  @GameTest
  public void onlyTheLockedTableIsNotPlaced(GameTestHelper helper) {
    ServerPlayer player = playerWithCrafterIngredients(helper, 1);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    RecipeBookMenu.PostPlaceAction action = place(helper, player, menu);

    helper.assertTrue(action == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE, "ghost only");
    helper.assertTrue(gridIsEmpty(menu), "nothing is placed");
    expectTables(helper, player, STATION_SLOT, 1);
    helper.assertTrue(menu.stillValid(player), "the menu stays open");
    helper.succeed();
  }

  @GameTest
  public void aSecondTableIsUsedInstead(GameTestHelper helper) {
    ServerPlayer player = playerWithCrafterIngredients(helper, 1);
    player.getInventory().setItem(FREE_TABLE_SLOT, new ItemStack(Items.CRAFTING_TABLE));
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    place(helper, player, menu);

    helper.assertTrue(menu.getSlot(CRAFTING_RESULT).getItem().is(Items.CRAFTER), "it is placed");
    expectTables(helper, player, STATION_SLOT, 1);
    helper.assertTrue(
        player.getInventory().getItem(FREE_TABLE_SLOT).isEmpty(), "from the free table");
    helper.assertTrue(menu.stillValid(player), "the menu stays open");
    helper.succeed();
  }

  @GameTest
  public void theRestOfALockedStackIsUsable(GameTestHelper helper) {
    ServerPlayer player = playerWithCrafterIngredients(helper, TWO_TABLES);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    place(helper, player, menu);

    helper.assertTrue(menu.getSlot(CRAFTING_RESULT).getItem().is(Items.CRAFTER), "it is placed");
    expectTables(helper, player, STATION_SLOT, 1);
    helper.assertTrue(menu.stillValid(player), "the menu stays open");
    helper.succeed();
  }

  @GameTest
  public void theClientCountAgreesWithThePlacement(GameTestHelper helper) {
    Recipe<?> crafter = crafter(helper).value();
    ServerPlayer alone = playerWithCrafterIngredients(helper, 1);
    ServerPlayer stacked = playerWithCrafterIngredients(helper, TWO_TABLES);

    helper.assertFalse(
        clientCount(alone).canCraft(crafter, null), "the table in use is not counted");
    helper.assertTrue(
        clientCount(stacked).canCraft(crafter, null), "the rest of the stack is counted");
    helper.succeed();
  }

  /**
   * What the client's recipe book counts: a vanilla crafting menu (as the client builds from the
   * menu type) with the lock the client installs, counted as {@code RecipeBookComponent} does.
   */
  private static StackedItemContents clientCount(ServerPlayer player) {
    Inventory inventory = player.getInventory();
    CraftingMenu menu = new CraftingMenu(CONTAINER_ID, inventory, ContainerLevelAccess.NULL);
    StationLock lock = StationLock.install(menu, inventory, Station.CRAFTING_TABLE, STATION_SLOT);
    StackedItemContents contents = new StackedItemContents();
    inventory.fillStackedContents(contents);
    menu.fillCraftSlotsStackedContents(contents);
    lock.reserveFromCount(inventory, contents);
    return contents;
  }

  private static RecipeBookMenu.PostPlaceAction place(
      GameTestHelper helper, ServerPlayer player, AbstractContainerMenu menu) {
    return ((RecipeBookMenu) menu)
        .handlePlacement(false, false, crafter(helper), helper.getLevel(), player.getInventory());
  }

  private static RecipeHolder<?> crafter(GameTestHelper helper) {
    return helper
        .getLevel()
        .getServer()
        .getRecipeManager()
        .byKey(CRAFTER)
        .orElseThrow(() -> new IllegalStateException("no crafter recipe"));
  }

  private static boolean gridIsEmpty(AbstractContainerMenu menu) {
    for (int slot = FIRST_GRID_SLOT; slot <= LAST_GRID_SLOT; slot++) {
      if (!menu.getSlot(slot).getItem().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static void expectTables(
      GameTestHelper helper, ServerPlayer player, int inventorySlot, int count) {
    ItemStack stack = player.getInventory().getItem(inventorySlot);
    helper.assertTrue(
        stack.is(Items.CRAFTING_TABLE) && stack.getCount() == count,
        "the table in use stays: " + stack);
  }

  private static ServerPlayer playerWithCrafterIngredients(GameTestHelper helper, int tables) {
    ServerPlayer player = HandyFixtures.player(helper, GameType.SURVIVAL);
    Inventory inventory = player.getInventory();
    inventory.setItem(STATION_SLOT, new ItemStack(Items.CRAFTING_TABLE, tables));
    inventory.setItem(IRON_SLOT, new ItemStack(Items.IRON_INGOT, IRON));
    inventory.setItem(REDSTONE_SLOT, new ItemStack(Items.REDSTONE, REDSTONE));
    inventory.setItem(DROPPER_SLOT, new ItemStack(Items.DROPPER));
    return player;
  }
}
