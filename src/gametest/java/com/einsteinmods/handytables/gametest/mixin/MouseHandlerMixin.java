package com.einsteinmods.handytables.gametest.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test-only: vanilla {@code Gui#setScreen} ends with {@code MouseHandler#resyncMousePosition},
 * which reads the desktop's real cursor ({@code SDL_GetGlobalMouseState}). Fabric's client
 * GameTests cancel the cursor warps ({@code InputConstants#grabMouse} / {@code #releaseMouse}) but
 * not this read, so the position would be wherever the developer's mouse is. Without it the
 * position is exactly what the game code sets (simulated moves, vanilla's re-centring, Handy
 * Tables' restore).
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
  @Inject(method = "resyncMousePosition", at = @At("HEAD"), cancellable = true)
  private void handytablesTest$keepSimulatedCursor(CallbackInfo ci) {
    ci.cancel();
  }
}
