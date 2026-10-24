package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.menu.PortableAnvilMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * The anvil opened from its item works like the placed one (combine, rename, XP cost) and wears out
 * like it, but the wear lands on the item and never on a block at the player's position.
 */
public final class AnvilGameTest {
  private static final int STATION_SLOT = 0;
  private static final int INPUT = 0;
  private static final int ADDITION = 1;
  private static final int RESULT = 2;
  private static final int PRIMARY_BUTTON = 0;
  private static final int STARTING_LEVELS = 100;
  private static final int SWORD_DAMAGE = 1000;
  private static final int USES = 40;
  private static final int ROLLS = 1000;
  private static final int TWO_ANVILS = 2;
  private static final int FULL_STACK = 64;
  private static final String NAME = "Blade";
  private static final long SEED = 26_3L;
  private static final double MIN_RATE = 0.09;
  private static final double MAX_RATE = 0.15;

  /** The player's feet: {@code HandyFixtures} puts the player at (0.5, 1, 0.5). */
  private static final BlockPos FEET = new BlockPos(0, 1, 0);

  @GameTest
  public void combiningRepairsAndCostsExperience(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, new ItemStack(Items.ANVIL));
    PortableAnvilMenu menu = open(player);
    ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
    sword.setDamageValue(SWORD_DAMAGE);

    menu.getSlot(INPUT).set(sword);
    menu.getSlot(ADDITION).set(new ItemStack(Items.DIAMOND));
    int cost = menu.getCost();
    ItemStack result = menu.getSlot(RESULT).getItem();
    helper.assertTrue(result.is(Items.DIAMOND_SWORD), "the result is the sword");
    helper.assertTrue(result.getDamageValue() < SWORD_DAMAGE, "repaired");
    helper.assertTrue(cost > 0, "at a cost");

    menu.clicked(RESULT, PRIMARY_BUTTON, ContainerInput.PICKUP, player);

    helper.assertTrue(menu.getCarried().is(Items.DIAMOND_SWORD), "the sword is taken");
    helper.assertTrue(player.experienceLevel == STARTING_LEVELS - cost, "the cost is paid");
    helper.assertTrue(menu.getSlot(ADDITION).getItem().isEmpty(), "the diamond is used up");
    menu.setCarried(ItemStack.EMPTY);
    helper.succeed();
  }

  @GameTest
  public void renamingWorksThroughTheVanillaPath(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, new ItemStack(Items.ANVIL));
    PortableAnvilMenu menu = open(player);
    menu.getSlot(INPUT).set(new ItemStack(Items.DIAMOND_SWORD));

    // ServerGamePacketListenerImpl#handleRenameItem: containerMenu is an AnvilMenu, stillValid.
    helper.assertTrue(menu.stillValid(player), "the rename packet is accepted");
    menu.setItemName(NAME);
    int cost = menu.getCost();
    menu.clicked(RESULT, PRIMARY_BUTTON, ContainerInput.PICKUP, player);

    helper.assertTrue(
        NAME.equals(menu.getCarried().getHoverName().getString()), "the sword is renamed");
    helper.assertTrue(cost > 0 && player.experienceLevel == STARTING_LEVELS - cost, "for XP");
    menu.setCarried(ItemStack.EMPTY);
    helper.succeed();
  }

  @GameTest
  public void forcedWearTurnsTheItemToChippedDamagedThenGone(GameTestHelper helper) {
    helper.setBlock(FEET, Blocks.ANVIL);
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, new ItemStack(Items.ANVIL));
    PortableAnvilMenu menu = open(player);

    menu.afterUse(player, true);
    helper.assertTrue(stationSlot(player).is(Items.CHIPPED_ANVIL), "chipped");
    menu.afterUse(player, true);
    helper.assertTrue(stationSlot(player).is(Items.DAMAGED_ANVIL), "damaged");
    helper.assertTrue(menu.stillValid(player), "still usable while damaged");
    menu.afterUse(player, true);
    helper.assertTrue(stationSlot(player).isEmpty(), "gone");
    helper.assertBlockPresent(Blocks.ANVIL, FEET);
    helper.succeed();
  }

  @GameTest
  public void aStackOfTwoWearsOnlyOneAndKeepsWearingIt(GameTestHelper helper) {
    ServerPlayer player =
        playerWith(helper, GameType.SURVIVAL, new ItemStack(Items.ANVIL, TWO_ANVILS));
    PortableAnvilMenu menu = open(player);

    menu.afterUse(player, true);
    helper.assertTrue(count(player, Items.ANVIL) == 1, "one anvil stays intact");
    helper.assertTrue(count(player, Items.CHIPPED_ANVIL) == 1, "one is chipped");
    menu.afterUse(player, true);
    helper.assertTrue(count(player, Items.ANVIL) == 1, "the intact one is untouched");
    helper.assertTrue(count(player, Items.CHIPPED_ANVIL) == 0, "the chipped one wears on");
    helper.assertTrue(count(player, Items.DAMAGED_ANVIL) == 1, "into a damaged one");
    helper.succeed();
  }

  @GameTest
  public void anAnvilOnTheCursorWears(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.SURVIVAL, new ItemStack(Items.ANVIL));
    PortableAnvilMenu menu = open(player);
    menu.setCarried(player.getInventory().removeItemNoUpdate(STATION_SLOT));

    menu.afterUse(player, true);

    helper.assertTrue(menu.getCarried().is(Items.CHIPPED_ANVIL), "the carried anvil is chipped");
    menu.setCarried(ItemStack.EMPTY);
    helper.succeed();
  }

  @GameTest
  public void breakingClosesTheMenuAndGivesTheInputsBack(GameTestHelper helper) {
    ServerPlayer player =
        playerWith(helper, GameType.SURVIVAL, new ItemStack(Items.DAMAGED_ANVIL, TWO_ANVILS));
    PortableAnvilMenu menu = open(player);
    menu.getSlot(INPUT).set(new ItemStack(Items.DIAMOND_SWORD));

    menu.afterUse(player, true);

    helper.assertTrue(count(player, Items.DAMAGED_ANVIL) == 1, "one damaged anvil broke");
    helper.assertFalse(menu.stillValid(player), "the broken anvil's menu closes");
    player.closeContainer();
    helper.assertTrue(count(player, Items.DIAMOND_SWORD) == 1, "the input is back");
    helper.succeed();
  }

  @GameTest
  public void survivalUsesNeverTouchTheBlockAtThePlayersFeet(GameTestHelper helper) {
    helper.setBlock(FEET, Blocks.ANVIL);
    ServerPlayer player =
        playerWith(helper, GameType.SURVIVAL, new ItemStack(Items.ANVIL, FULL_STACK));
    PortableAnvilMenu menu = open(player);
    menu.setItemName(NAME);

    int uses = renameRepeatedly(player, menu);

    helper.assertTrue(uses > 0, "the anvil was used");
    helper.assertTrue(player.experienceLevel == STARTING_LEVELS - uses, "one level per rename");
    helper.assertBlockPresent(Blocks.ANVIL, FEET);
    helper.succeed();
  }

  @GameTest
  public void creativeNeverWears(GameTestHelper helper) {
    ServerPlayer player = playerWith(helper, GameType.CREATIVE, new ItemStack(Items.ANVIL));
    PortableAnvilMenu menu = open(player);
    menu.setItemName(NAME);

    for (int roll = 0; roll < ROLLS; roll++) {
      helper.assertFalse(PortableAnvilMenu.rollsWear(player), "creative never rolls a wear");
    }
    helper.assertTrue(renameRepeatedly(player, menu) == USES, "every rename is taken");
    helper.assertTrue(stationSlot(player).is(Items.ANVIL), "the anvil is intact");
    helper.assertTrue(stationSlot(player).getCount() == 1, "and alone");
    helper.succeed();
  }

  /**
   * 1000 uses with a seeded random from a stack of 64: each wear changes exactly one anvil one step
   * (intact → chipped → damaged → gone, the total dropping only when one breaks), and the observed
   * rate is near vanilla's 12% ({@code AnvilMenu#onTake}). After a break the menu is opened again.
   */
  @GameTest
  public void eachWearChangesExactlyOneAnvilAtVanillaOdds(GameTestHelper helper) {
    ServerPlayer player =
        playerWith(helper, GameType.SURVIVAL, new ItemStack(Items.ANVIL, FULL_STACK));
    player.getRandom().setSeed(SEED);
    PortableAnvilMenu menu = open(player);
    int wears = 0;
    for (int use = 0; use < ROLLS; use++) {
      if (!menu.stillValid(player)) {
        menu = open(player);
      }
      int intact = count(player, Items.ANVIL);
      int chipped = count(player, Items.CHIPPED_ANVIL);
      int damaged = count(player, Items.DAMAGED_ANVIL);
      boolean wear = PortableAnvilMenu.rollsWear(player);
      menu.afterUse(player, wear);
      int intactAfter = count(player, Items.ANVIL);
      int chippedAfter = count(player, Items.CHIPPED_ANVIL);
      int damagedAfter = count(player, Items.DAMAGED_ANVIL);
      boolean unchanged =
          intactAfter == intact && chippedAfter == chipped && damagedAfter == damaged;
      boolean oneStep =
          (intactAfter == intact - 1 && chippedAfter == chipped + 1 && damagedAfter == damaged)
              || (intactAfter == intact
                  && chippedAfter == chipped - 1
                  && damagedAfter == damaged + 1)
              || (intactAfter == intact && chippedAfter == chipped && damagedAfter == damaged - 1);
      helper.assertTrue(wear ? oneStep : unchanged, "use " + use + " changed one anvil at most");
      if (wear) {
        wears++;
      }
    }
    double rate = (double) wears / ROLLS;
    helper.assertTrue(rate > MIN_RATE && rate < MAX_RATE, "wear rate " + rate + " near 12%");
    helper.succeed();
  }

  /** Renames a fresh sword up to {@link #USES} times through the result slot; returns the uses. */
  private static int renameRepeatedly(ServerPlayer player, PortableAnvilMenu menu) {
    int uses = 0;
    while (uses < USES && menu.stillValid(player)) {
      menu.getSlot(INPUT).set(new ItemStack(Items.DIAMOND_SWORD));
      menu.clicked(RESULT, PRIMARY_BUTTON, ContainerInput.PICKUP, player);
      if (!menu.getCarried().has(DataComponents.CUSTOM_NAME)) {
        break;
      }
      menu.setCarried(ItemStack.EMPTY);
      uses++;
    }
    return uses;
  }

  private static ServerPlayer playerWith(
      GameTestHelper helper, GameType gameType, ItemStack anvil) {
    ServerPlayer player = HandyFixtures.player(helper, gameType);
    player.getInventory().setItem(STATION_SLOT, anvil);
    player.giveExperienceLevels(STARTING_LEVELS);
    return player;
  }

  private static PortableAnvilMenu open(ServerPlayer player) {
    return (PortableAnvilMenu) HandyFixtures.open(player, Station.ANVIL, STATION_SLOT);
  }

  private static ItemStack stationSlot(ServerPlayer player) {
    return player.getInventory().getItem(STATION_SLOT);
  }

  private static int count(ServerPlayer player, Item item) {
    return player.getInventory().countItem(item);
  }
}
