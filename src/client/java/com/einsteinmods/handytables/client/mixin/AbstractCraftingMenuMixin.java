package com.einsteinmods.handytables.client.mixin;

import com.einsteinmods.handytables.client.ClientStationLock;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The client's recipe book count ({@code RecipeBookComponent#updateStackedContents}: {@code
 * Inventory#fillStackedContents}, then this method) leaves out the station item in use, as the
 * server's placement does ({@code StationLock#placeWithoutReserved}). Runs only when a recipe book
 * recounts. In singleplayer the server's menus share this class; only the client's locked menu
 * matches.
 */
@Mixin(AbstractCraftingMenu.class)
abstract class AbstractCraftingMenuMixin {
  @Inject(method = "fillCraftSlotsStackedContents", at = @At("TAIL"))
  private void handytables$reserveStation(StackedItemContents contents, CallbackInfo ci) {
    ClientStationLock.reserveFromCount((AbstractCraftingMenu) (Object) this, contents);
  }
}
