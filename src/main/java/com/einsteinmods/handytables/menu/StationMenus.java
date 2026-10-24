package com.einsteinmods.handytables.menu;

import com.einsteinmods.handytables.core.Station;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** Builds the menu a station item opens, as the vanilla block's {@code getMenuProvider}. */
public final class StationMenus {
  private StationMenus() {}

  /**
   * What {@code ServerPlayer#openMenu} needs to open {@code station} from that inventory slot;
   * {@code returnTarget} is where closing it with its own gesture goes back to.
   */
  public static MenuProvider provider(
      Station station, int inventorySlot, ReturnTarget returnTarget) {
    return new SimpleMenuProvider(
        (containerId, inventory, player) ->
            create(station, containerId, inventory, player, inventorySlot, returnTarget),
        StationItems.title(station));
  }

  /**
   * The vanilla menu with a real level access at the player's feet: with {@code
   * ContainerLevelAccess.NULL} vanilla never computes results ({@code CraftingMenu#slotsChanged},
   * {@code CartographyTableMenu#setupResultSlot}) nor gives the inputs back on close ({@code
   * removed} calls {@code clearContainer} through the access). Sounds play at that position.
   */
  public static AbstractContainerMenu create(
      Station station,
      int containerId,
      Inventory inventory,
      Player player,
      int inventorySlot,
      ReturnTarget returnTarget) {
    ContainerLevelAccess access =
        ContainerLevelAccess.create(player.level(), player.blockPosition());
    StationItemCheck check =
        new StationItemCheck(player.level(), station, inventorySlot, returnTarget);
    return switch (station) {
      case CRAFTING_TABLE -> new PortableCraftingMenu(containerId, inventory, access, check);
      case STONECUTTER -> new PortableStonecutterMenu(containerId, inventory, access, check);
      case LOOM -> new PortableLoomMenu(containerId, inventory, access, check);
      case CARTOGRAPHY_TABLE ->
          new PortableCartographyTableMenu(containerId, inventory, access, check);
      case SMITHING_TABLE -> new PortableSmithingMenu(containerId, inventory, access, check);
      case GRINDSTONE -> new PortableGrindstoneMenu(containerId, inventory, access, check);
      case ANVIL -> new PortableAnvilMenu(containerId, inventory, access, check);
    };
  }
}
