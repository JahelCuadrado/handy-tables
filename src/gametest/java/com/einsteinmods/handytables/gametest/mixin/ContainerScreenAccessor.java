package com.einsteinmods.handytables.gametest.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Test-only: where a container screen draws its slots (vanilla {@code leftPos}, {@code topPos}).
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
  @Accessor("leftPos")
  int handytablesTest$leftPos();

  @Accessor("topPos")
  int handytablesTest$topPos();
}
