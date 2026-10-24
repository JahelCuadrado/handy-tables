package com.einsteinmods.handytables.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;

/**
 * Shows the inventory screen after the server closed a station back to it ({@code
 * ShowInventoryPayload}), switching straight from the station's screen so the cursor does not move.
 * The client's open menu becomes the inventory menu again, as the server's did ({@code
 * ServerPlayer#doCloseContainer}); otherwise vanilla {@code
 * MultiPlayerGameMode#handleContainerInput} would ignore every click in it. The screen opens as
 * vanilla's inventory key does ({@code Minecraft#handleKeybinds}): the server's own inventory when
 * it controls it (riding), else {@code InventoryScreen}, which switches itself to the creative
 * inventory in creative.
 */
final class ShowInventory {
  private ShowInventory() {}

  static void show(Minecraft minecraft) {
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.gameMode == null) {
      return;
    }
    player.containerMenu = player.inventoryMenu;
    if (minecraft.gameMode.isServerControlledInventory()) {
      player.sendOpenInventory();
    } else {
      minecraft.gui.setScreen(new InventoryScreen(player));
    }
  }
}
