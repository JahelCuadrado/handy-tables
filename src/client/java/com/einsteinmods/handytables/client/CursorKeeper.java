package com.einsteinmods.handytables.client;

import com.einsteinmods.handytables.client.mixin.MouseHandlerAccessor;
import com.einsteinmods.handytables.core.Station;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;

/**
 * Keeps the cursor where it was when a station opens from a container screen. The server closes the
 * open container first ({@code ServerPlayer#openMenu} → {@code closeContainer}); the client then
 * goes back to the game ({@code LocalPlayer#clientSideCloseContainer} → {@code Gui#setScreen(null)}
 * → {@code MouseHandler#grabMouse}, cursor to the window centre) and the station's screen releases
 * it there again ({@code Gui#setScreen} → {@code MouseHandler#releaseMouse}). From the inventory
 * screen no close is sent ({@code StationRequests#open} closes the inventory menu server-side
 * only), so nothing moves there.
 *
 * <p>The position is remembered when the request is sent; when the requested station's screen is
 * set up within {@link #TIMEOUT_MILLIS} and vanilla left the cursor exactly at the window centre,
 * it is put back with the vanilla call that places a released cursor ({@code
 * InputConstants#releaseMouse}, an SDL warp) and in {@code MouseHandler}. Anything else (another
 * screen, the cursor already moved, too late) leaves it alone.
 *
 * <p>Cost: two field writes per station opened; per container screen set up, one null check. Render
 * thread only.
 */
public final class CursorKeeper {
  /** How long the station screen may take to arrive after the request (server round trip). */
  static final long TIMEOUT_MILLIS = 1000L;

  private static final double HALF = 2.0;

  private static @Nullable MenuType<?> expectedMenu;
  private static double cursorX;
  private static double cursorY;
  private static long deadline;

  private CursorKeeper() {}

  /** Called right before an open request for {@code station} is sent. */
  static void rememberBeforeOpening(Minecraft minecraft, Station station) {
    cursorX = minecraft.mouseHandler.xpos();
    cursorY = minecraft.mouseHandler.ypos();
    deadline = Util.getMillis() + TIMEOUT_MILLIS;
    expectedMenu = menuType(station);
  }

  /** {@code ScreenEvents.AFTER_INIT}: puts the cursor back on the requested station's screen. */
  static void afterScreenInit(Minecraft minecraft, Screen screen) {
    MenuType<?> expected = expectedMenu;
    if (expected == null) {
      return;
    }
    expectedMenu = null;
    if (Util.getMillis() > deadline
        || !(screen instanceof AbstractContainerScreen<?> containerScreen)
        || containerScreen.getMenu().getType() != expected) {
      return;
    }
    MouseHandler mouse = minecraft.mouseHandler;
    Window window = minecraft.getWindow();
    if (mouse.isMouseGrabbed()
        || mouse.xpos() != window.getScreenWidth() / HALF
        || mouse.ypos() != window.getScreenHeight() / HALF) {
      return;
    }
    InputConstants.releaseMouse(window, cursorX, cursorY);
    MouseHandlerAccessor accessor = (MouseHandlerAccessor) mouse;
    accessor.handytables$setXpos(cursorX);
    accessor.handytables$setYpos(cursorY);
  }

  /** The vanilla menu each station's block opens ({@code getMenuProvider} of each block). */
  private static MenuType<?> menuType(Station station) {
    return switch (station) {
      case CRAFTING_TABLE -> MenuType.CRAFTING;
      case STONECUTTER -> MenuType.STONECUTTER;
      case LOOM -> MenuType.LOOM;
      case CARTOGRAPHY_TABLE -> MenuType.CARTOGRAPHY_TABLE;
      case SMITHING_TABLE -> MenuType.SMITHING;
      case GRINDSTONE -> MenuType.GRINDSTONE;
      case ANVIL -> MenuType.ANVIL;
    };
  }
}
