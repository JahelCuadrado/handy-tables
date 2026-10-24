package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.client.ClientStationLock;
import com.einsteinmods.handytables.client.config.HandyTablesSettingsScreen;
import com.einsteinmods.handytables.gametest.mixin.ContainerScreenAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * The real client: middle-clicking a crafting table in a placed chest's screen and in the inventory
 * screen opens the crafting screen with the cursor where it was (vanilla re-centres it when the
 * server closes the chest first), the table's slot shows the padlock, number-key, offhand and drop
 * keys on it change nothing on either side, and middle-clicking it again goes back to that chest or
 * to the inventory, the cursor still in place.
 */
public final class StationScreenClientGameTest implements FabricClientGameTest {
  private static final int STATION_SLOT = 0;
  private static final int SECOND_HOTBAR_KEY = 1;
  private static final int SLOT_CENTRE = 8;
  private static final double AWAY = 1.0;
  private static final int CHEST_ROWS = 3;

  /**
   * A body pixel (the grey lower part) of the 7x7 padlock sprite (row 4, column 3), drawn in the
   * slot's top right corner (9 pixels in).
   */
  private static final int PADLOCK_BODY_X = 12;

  private static final int PADLOCK_BODY_Y = 4;
  private static final int PADLOCK_BODY = ARGB.color(184, 184, 190);
  private static final String SCREENSHOT = "handy-tables-padlock-v3";
  private static final String SETTINGS_SCREENSHOT = "handy-tables-settings";

  @Override
  public void runTest(ClientGameTestContext context) {
    try (TestSingleplayerContext world = context.worldBuilder().create()) {
      world.getConnection().waitForChunksRender();
      TestServerContext server = world.getServer();
      server.runOnServer(
          minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().setItem(STATION_SLOT, new ItemStack(Items.CRAFTING_TABLE));
            player.getInventory().setItem(SECOND_HOTBAR_KEY, new ItemStack(Items.STICK));
          });
      context.waitTick();

      // From a chest: the server closes it first, which re-centres the cursor in vanilla.
      server.runOnServer(
          minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            BlockPos chest = player.blockPosition().east();
            ServerLevel level = player.level();
            level.setBlockAndUpdate(chest.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            player.openMenu(level.getBlockState(chest).getMenuProvider(level, chest));
          });
      context.waitForScreen(ContainerScreen.class);
      switchKeepingTheCursor(context, CraftingScreen.class);
      expectLockHolds(context, server);
      // Off the slot: vanilla's hover highlight would tint the padlock.
      context.getInput().setCursorPos(AWAY, AWAY);
      context.waitTick();
      expectPadlock(context, true);
      // The open gesture on the locked table goes back to the same chest.
      switchKeepingTheCursor(context, ContainerScreen.class);
      expectChestBlockMenu(context);

      // From the inventory screen: no close is sent, nothing to restore; and back to it.
      context.runOnClient(mc -> mc.player.closeContainer());
      context.waitForScreen(null);
      context.getInput().pressKey(options -> options.keyInventory);
      context.waitForScreen(InventoryScreen.class);
      switchKeepingTheCursor(context, CraftingScreen.class);
      expectPadlock(context, false);
      switchKeepingTheCursor(context, InventoryScreen.class);
      context.runOnClient(mc -> mc.player.closeContainer());
      context.waitForScreen(null);

      // The settings screen (Mod Menu's "Configure") opens and closes.
      context.runOnClient(mc -> mc.gui.setScreen(new HandyTablesSettingsScreen(null)));
      context.waitForScreen(HandyTablesSettingsScreen.class);
      context.takeScreenshot(SETTINGS_SCREENSHOT);
      context.runOnClient(mc -> mc.gui.setScreen(null));
      context.waitForScreen(null);
    }
  }

  /** Hovers the table, middle-clicks it and checks the next screen keeps the cursor. */
  private static void switchKeepingTheCursor(
      ClientGameTestContext context, Class<? extends Screen> next) {
    double[] target = context.computeOnClient(StationScreenClientGameTest::stationSlotCentre);
    context.getInput().setCursorPos(target[0], target[1]);
    context.waitTick();
    context.getInput().pressKey(options -> options.keyPickItem);
    context.waitForScreen(next);
    context.waitTick();
    double[] after =
        context.computeOnClient(
            mc -> new double[] {mc.mouseHandler.xpos(), mc.mouseHandler.ypos()});
    if (after[0] != target[0] || after[1] != target[1]) {
      throw new AssertionError(
          "cursor moved from ("
              + target[0]
              + ", "
              + target[1]
              + ") to ("
              + after[0]
              + ", "
              + after[1]
              + ")");
    }
  }

  /** The chest screen is the placed chest's own menu again (its block entity's container). */
  private static void expectChestBlockMenu(ClientGameTestContext context) {
    boolean chest =
        context.computeOnClient(
            mc ->
                mc.gui.screen() instanceof ContainerScreen screen
                    && screen.getMenu().getRowCount() == CHEST_ROWS
                    && mc.player.containerMenu == screen.getMenu());
    if (!chest) {
      throw new AssertionError("the chest did not open again");
    }
  }

  /** Window coordinates of the centre of the slot showing the station's inventory slot. */
  private static double[] stationSlotCentre(Minecraft minecraft) {
    if (!(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
      throw new AssertionError("no container screen");
    }
    Slot slot = stationSlot(minecraft, screen);
    ContainerScreenAccessor geometry = (ContainerScreenAccessor) screen;
    double scale =
        (double) minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth();
    return new double[] {
      (geometry.handytablesTest$leftPos() + slot.x + SLOT_CENTRE) * scale,
      (geometry.handytablesTest$topPos() + slot.y + SLOT_CENTRE) * scale
    };
  }

  private static Slot stationSlot(Minecraft minecraft, AbstractContainerScreen<?> screen) {
    for (Slot slot : screen.getMenu().slots) {
      if (slot.container == minecraft.player.getInventory()
          && slot.getContainerSlot() == STATION_SLOT) {
        return slot;
      }
    }
    throw new AssertionError("the station's slot is not in the menu");
  }

  /** The lock targets the table's slot and its padlock is in the rendered frame. */
  private static void expectPadlock(ClientGameTestContext context, boolean screenshot) {
    @Nullable String problem =
        context.computeOnClient(
            mc -> {
              Slot locked = ClientStationLock.lockedSlot();
              if (locked == null || locked.getContainerSlot() != STATION_SLOT) {
                return "the table's slot is not locked: " + locked;
              }
              return null;
            });
    if (problem != null) {
      throw new AssertionError(problem);
    }
    if (!screenshot) {
      return;
    }
    int[] pixel =
        context.computeOnClient(
            mc -> {
              AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
              ContainerScreenAccessor geometry = (ContainerScreenAccessor) screen;
              Slot slot = ClientStationLock.lockedSlot();
              int scale = mc.getWindow().getGuiScale();
              return new int[] {
                (geometry.handytablesTest$leftPos() + slot.x + PADLOCK_BODY_X) * scale,
                (geometry.handytablesTest$topPos() + slot.y + PADLOCK_BODY_Y) * scale
              };
            });
    Path path = context.takeScreenshot(SCREENSHOT);
    int color = readPixel(path, pixel[0], pixel[1]);
    if (ARGB.opaque(color) != PADLOCK_BODY) {
      throw new AssertionError(
          "no padlock at (" + pixel[0] + ", " + pixel[1] + "): " + Integer.toHexString(color));
    }
  }

  /** Number-key, offhand-swap and drop keys over the locked table change nothing anywhere. */
  private static void expectLockHolds(ClientGameTestContext context, TestServerContext server) {
    context.getInput().pressKey(options -> options.keyHotbarSlots[SECOND_HOTBAR_KEY]);
    context.getInput().pressKey(options -> options.keySwapOffhand);
    context.getInput().pressKey(options -> options.keyDrop);
    context.waitTicks(2);
    boolean clientKept =
        context.computeOnClient(
            mc -> mc.player.getInventory().getItem(STATION_SLOT).is(Items.CRAFTING_TABLE));
    boolean serverKept =
        server.computeOnServer(
            minecraftServer ->
                minecraftServer
                    .getPlayerList()
                    .getPlayers()
                    .getFirst()
                    .getInventory()
                    .getItem(STATION_SLOT)
                    .is(Items.CRAFTING_TABLE));
    boolean stillOpen = context.computeOnClient(mc -> mc.gui.screen() instanceof CraftingScreen);
    if (!clientKept || !serverKept || !stillOpen) {
      throw new AssertionError(
          "lock broken: client " + clientKept + ", server " + serverKept + ", open " + stillOpen);
    }
  }

  private static int readPixel(Path path, int x, int y) {
    try (InputStream input = Files.newInputStream(path);
        NativeImage image = NativeImage.read(input)) {
      return image.getPixel(x, y);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
