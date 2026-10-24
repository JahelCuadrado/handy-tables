package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.gametest.mixin.ContainerScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * The real click path (mouse events through the crafting screen, the client's prediction and the
 * server) on a locked stack of crafting tables: the spares taken off it go back onto it with a left
 * click, one at a time with a right click, and by dragging; the last table never leaves.
 */
public final class StackMergeClientGameTest implements FabricClientGameTest {
  private static final int STATION_SLOT = 0;
  private static final int EMPTY_SLOT = 20;
  private static final int TABLES = 3;
  private static final int SLOT_CENTRE = 8;
  private static final int SETTLE_TICKS = 2;
  private static final double IN_SLOT_NUDGE = 1.0;

  /** Longer than vanilla's double-click window ({@code MouseHandler#DOUBLE_CLICK_THRESHOLD_MS}). */
  private static final int SEPARATE_CLICK_TICKS = 8;

  @Override
  public void runTest(ClientGameTestContext context) {
    try (TestSingleplayerContext world = context.worldBuilder().create()) {
      world.getConnection().waitForChunksRender();
      TestServerContext server = world.getServer();
      server.runOnServer(
          minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player
                .getInventory()
                .setItem(STATION_SLOT, new ItemStack(Items.CRAFTING_TABLE, TABLES));
          });
      context.waitTick();
      context.getInput().pressKey(options -> options.keyInventory);
      context.waitForScreen(InventoryScreen.class);
      hover(context, STATION_SLOT);
      context.getInput().pressKey(options -> options.keyPickItem);
      context.waitForScreen(CraftingScreen.class);

      hover(context, STATION_SLOT);
      click(context, InputConstants.MOUSE_BUTTON_LEFT);
      expect(context, server, "left click takes the spares", 1, TABLES - 1, 0);
      click(context, InputConstants.MOUSE_BUTTON_LEFT);
      expect(context, server, "left click puts them back", TABLES, 0, 0);

      click(context, InputConstants.MOUSE_BUTTON_LEFT);
      click(context, InputConstants.MOUSE_BUTTON_RIGHT);
      expect(context, server, "right click puts one back", 2, 1, 0);
      click(context, InputConstants.MOUSE_BUTTON_RIGHT);
      expect(context, server, "and the last one", TABLES, 0, 0);

      click(context, InputConstants.MOUSE_BUTTON_LEFT);
      drag(context, InputConstants.MOUSE_BUTTON_LEFT);
      expect(context, server, "a left drag spreads them", 2, 0, 1);
      hover(context, EMPTY_SLOT);
      click(context, InputConstants.MOUSE_BUTTON_LEFT);
      hover(context, STATION_SLOT);
      drag(context, InputConstants.MOUSE_BUTTON_RIGHT);
      expect(context, server, "a right drag puts one each", TABLES, 0, 0);

      // A quick second click is vanilla's double-click: gather the same item into the cursor, the
      // reserved table excepted.
      server.runOnServer(
          minecraftServer ->
              minecraftServer
                  .getPlayerList()
                  .getPlayers()
                  .getFirst()
                  .getInventory()
                  .setItem(EMPTY_SLOT, new ItemStack(Items.CRAFTING_TABLE)));
      context.waitTicks(SETTLE_TICKS);
      hover(context, STATION_SLOT);
      click(context, InputConstants.MOUSE_BUTTON_LEFT);
      expect(context, server, "take the spares again", 1, TABLES - 1, 1);
      context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
      context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
      context.waitTicks(SETTLE_TICKS);
      expect(context, server, "a double-click gathers all but the reserved one", 1, TABLES, 0);

      context.runOnClient(mc -> mc.player.closeContainer());
      context.waitForScreen(null);
    }
  }

  /** A deliberate click: the next one comes later than a double-click would. */
  private static void click(ClientGameTestContext context, int button) {
    context.getInput().pressMouse(button);
    context.waitTicks(SEPARATE_CLICK_TICKS);
  }

  /** Holds {@code button} on the station slot, moves to the empty slot and lets go. */
  private static void drag(ClientGameTestContext context, int button) {
    hover(context, STATION_SLOT);
    context.getInput().holdMouse(button);
    // A real drag moves over the first slot too: vanilla adds a slot on a drag event over it.
    context.getInput().moveCursor(IN_SLOT_NUDGE, IN_SLOT_NUDGE);
    context.waitTick();
    hover(context, EMPTY_SLOT);
    context.getInput().releaseMouse(button);
    context.waitTicks(SETTLE_TICKS);
  }

  private static void hover(ClientGameTestContext context, int inventorySlot) {
    double[] centre = context.computeOnClient(mc -> slotCentre(mc, inventorySlot));
    context.getInput().setCursorPos(centre[0], centre[1]);
    context.waitTick();
  }

  private static double[] slotCentre(Minecraft minecraft, int inventorySlot) {
    AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) minecraft.gui.screen();
    ContainerScreenAccessor geometry = (ContainerScreenAccessor) screen;
    double scale =
        (double) minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth();
    for (Slot slot : screen.getMenu().slots) {
      if (slot.container == minecraft.player.getInventory()
          && slot.getContainerSlot() == inventorySlot) {
        return new double[] {
          (geometry.handytablesTest$leftPos() + slot.x + SLOT_CENTRE) * scale,
          (geometry.handytablesTest$topPos() + slot.y + SLOT_CENTRE) * scale
        };
      }
    }
    throw new AssertionError("no menu slot for inventory slot " + inventorySlot);
  }

  /** The station slot, the cursor and the empty slot hold these many tables, on both sides. */
  private static void expect(
      ClientGameTestContext context,
      TestServerContext server,
      String step,
      int station,
      int carried,
      int other) {
    String client =
        context.computeOnClient(
            mc ->
                describe(
                    mc.player.getInventory(),
                    mc.player.containerMenu.getCarried(),
                    station,
                    carried,
                    other));
    String serverSide =
        server.computeOnServer(
            minecraftServer -> {
              ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
              return describe(
                  player.getInventory(),
                  player.containerMenu.getCarried(),
                  station,
                  carried,
                  other);
            });
    if (!client.isEmpty() || !serverSide.isEmpty()) {
      throw new AssertionError(step + ": client [" + client + "] server [" + serverSide + "]");
    }
  }

  private static String describe(
      Inventory inventory, ItemStack cursor, int station, int carried, int other) {
    int stationCount = tables(inventory.getItem(STATION_SLOT));
    int cursorCount = tables(cursor);
    int otherCount = tables(inventory.getItem(EMPTY_SLOT));
    if (stationCount == station && cursorCount == carried && otherCount == other) {
      return "";
    }
    return "slot " + stationCount + ", cursor " + cursorCount + ", other " + otherCount;
  }

  private static int tables(ItemStack stack) {
    return stack.is(Items.CRAFTING_TABLE) ? stack.getCount() : 0;
  }
}
