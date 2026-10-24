package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.menu.PortableCartographyTableMenu;
import com.einsteinmods.handytables.menu.PortableCraftingMenu;
import com.einsteinmods.handytables.menu.PortableGrindstoneMenu;
import com.einsteinmods.handytables.menu.PortableLoomMenu;
import com.einsteinmods.handytables.menu.PortableSmithingMenu;
import com.einsteinmods.handytables.menu.PortableStonecutterMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.AABB;

/**
 * Each station opened from its item computes its result like the placed block: the menus get a real
 * level access, so vanilla's server-side result code runs.
 */
public final class StationMenusGameTest {
  private static final int STATION_SLOT = 0;
  private static final int CRAFTING_RESULT = 0;
  private static final int[] CRAFTING_GRID_2X2 = {1, 2, 4, 5};
  private static final int STONECUTTER_INPUT = 0;
  private static final int STONECUTTER_RESULT = 1;
  private static final int LOOM_BANNER = 0;
  private static final int LOOM_DYE = 1;
  private static final int LOOM_RESULT = 3;
  private static final int CARTOGRAPHY_MAP = 0;
  private static final int CARTOGRAPHY_ADDITION = 1;
  private static final int CARTOGRAPHY_RESULT = 2;
  private static final int SMITHING_TEMPLATE = 0;
  private static final int SMITHING_BASE = 1;
  private static final int SMITHING_ADDITION = 2;
  private static final int SMITHING_RESULT = 3;
  private static final int GRINDSTONE_INPUT = 0;
  private static final int GRINDSTONE_RESULT = 2;
  private static final int SHARPNESS_LEVEL = 3;
  private static final double ORB_SEARCH_RADIUS = 1.0;
  private static final int FIRST_RECIPE = 0;
  private static final int FIRST_PATTERN = 0;
  private static final int MAP_COPIES = 2;
  private static final byte MAP_SCALE = 0;

  @GameTest
  public void craftingTableCraftsFromPlanks(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, Items.CRAFTING_TABLE);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.CRAFTING_TABLE, STATION_SLOT);

    for (int gridSlot : CRAFTING_GRID_2X2) {
      menu.getSlot(gridSlot).set(new ItemStack(Items.OAK_PLANKS));
    }

    helper.assertTrue(menu instanceof PortableCraftingMenu, "a crafting menu opens");
    helper.assertTrue(
        menu.getSlot(CRAFTING_RESULT).getItem().is(Items.CRAFTING_TABLE),
        "four planks give a crafting table");
    helper.succeed();
  }

  @GameTest
  public void stonecutterCutsStone(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, Items.STONECUTTER);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.STONECUTTER, STATION_SLOT);

    menu.getSlot(STONECUTTER_INPUT).set(new ItemStack(Items.STONE));
    PortableStonecutterMenu stonecutter = (PortableStonecutterMenu) menu;
    helper.assertTrue(stonecutter.getNumberOfVisibleRecipes() > 0, "stone has stonecutter recipes");
    menu.clickMenuButton(player, FIRST_RECIPE);

    helper.assertFalse(
        menu.getSlot(STONECUTTER_RESULT).getItem().isEmpty(), "the chosen recipe has a result");
    helper.succeed();
  }

  @GameTest
  public void loomAppliesAPattern(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, Items.LOOM);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.LOOM, STATION_SLOT);

    menu.getSlot(LOOM_BANNER).set(new ItemStack(Items.BANNER.pick(DyeColor.WHITE)));
    menu.getSlot(LOOM_DYE).set(new ItemStack(Items.DYE.pick(DyeColor.RED)));
    menu.clickMenuButton(player, FIRST_PATTERN);

    helper.assertTrue(menu instanceof PortableLoomMenu, "a loom menu opens");
    ItemStack result = menu.getSlot(LOOM_RESULT).getItem();
    helper.assertTrue(result.is(Items.BANNER.pick(DyeColor.WHITE)), "the result is the banner");
    helper.assertFalse(
        result
            .getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
            .layers()
            .isEmpty(),
        "with the pattern");
    helper.succeed();
  }

  @GameTest
  public void cartographyTableCopiesAMap(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, Items.CARTOGRAPHY_TABLE);
    AbstractContainerMenu menu =
        HandyFixtures.open(player, Station.CARTOGRAPHY_TABLE, STATION_SLOT);

    menu.getSlot(CARTOGRAPHY_MAP)
        .set(MapItem.create(helper.getLevel(), 0, 0, MAP_SCALE, false, false));
    menu.getSlot(CARTOGRAPHY_ADDITION).set(new ItemStack(Items.MAP));

    helper.assertTrue(menu instanceof PortableCartographyTableMenu, "a cartography menu opens");
    ItemStack result = menu.getSlot(CARTOGRAPHY_RESULT).getItem();
    helper.assertTrue(
        result.is(Items.FILLED_MAP) && result.getCount() == MAP_COPIES, "the map is copied");
    helper.succeed();
  }

  @GameTest
  public void smithingTableUpgradesToNetherite(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, Items.SMITHING_TABLE);
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.SMITHING_TABLE, STATION_SLOT);

    menu.getSlot(SMITHING_TEMPLATE).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
    menu.getSlot(SMITHING_BASE).set(new ItemStack(Items.DIAMOND_SWORD));
    menu.getSlot(SMITHING_ADDITION).set(new ItemStack(Items.NETHERITE_INGOT));

    helper.assertTrue(menu instanceof PortableSmithingMenu, "a smithing menu opens");
    helper.assertTrue(
        menu.getSlot(SMITHING_RESULT).getItem().is(Items.NETHERITE_SWORD), "the sword is upgraded");
    helper.succeed();
  }

  @GameTest
  public void grindstoneRemovesEnchantmentsAndAwardsExperienceAtThePlayer(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, Items.GRINDSTONE);
    BlockPos feet = player.blockPosition();
    AbstractContainerMenu menu = HandyFixtures.open(player, Station.GRINDSTONE, STATION_SLOT);
    ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
    sword.enchant(
        helper
            .getLevel()
            .registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.SHARPNESS),
        SHARPNESS_LEVEL);

    menu.getSlot(GRINDSTONE_INPUT).set(sword);
    helper.assertTrue(menu instanceof PortableGrindstoneMenu, "a grindstone menu opens");
    ItemStack result = menu.getSlot(GRINDSTONE_RESULT).getItem();
    helper.assertTrue(result.is(Items.DIAMOND_SWORD), "the result is the sword");
    helper.assertFalse(EnchantmentHelper.hasAnyEnchantments(result), "without its enchantment");

    menu.clicked(GRINDSTONE_RESULT, 0, ContainerInput.PICKUP, player);

    helper.assertTrue(menu.getCarried().is(Items.DIAMOND_SWORD), "the sword is taken");
    helper.assertTrue(menu.getSlot(GRINDSTONE_INPUT).getItem().isEmpty(), "the input is used up");
    helper.assertFalse(
        helper
            .getLevel()
            .getEntitiesOfClass(ExperienceOrb.class, new AABB(feet).inflate(ORB_SEARCH_RADIUS))
            .isEmpty(),
        "experience drops at the player's feet");
    menu.setCarried(ItemStack.EMPTY);
    helper.succeed();
  }

  private static ServerPlayer playerWith(GameTestHelper helper, Item station) {
    ServerPlayer player = HandyFixtures.player(helper, GameType.SURVIVAL);
    player.getInventory().setItem(STATION_SLOT, new ItemStack(station));
    return player;
  }
}
