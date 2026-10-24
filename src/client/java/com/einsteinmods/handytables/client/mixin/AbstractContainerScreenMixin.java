package com.einsteinmods.handytables.client.mixin;

import com.einsteinmods.handytables.client.ClientStationLock;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The padlock over a locked station slot, drawn right after vanilla {@code
 * AbstractContainerScreen#extractSlots} (inside {@code extractContents}' offset to the screen
 * corner, above the items, below the carried item and tooltip; skipped with the slots when the
 * recipe book covers them). Cost: one null check per container screen frame while no station is
 * open.
 */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
  @Shadow @Final protected AbstractContainerMenu menu;

  @Inject(method = "extractSlots", at = @At("TAIL"))
  private void handytables$extractPadlock(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
    ClientStationLock.extractPadlock(menu, graphics);
  }
}
