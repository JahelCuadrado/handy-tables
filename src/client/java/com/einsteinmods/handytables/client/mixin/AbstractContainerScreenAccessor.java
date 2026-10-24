package com.einsteinmods.handytables.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The slot under the mouse, as vanilla updates it every frame in {@code extractContents}. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
  @Accessor("hoveredSlot")
  @Nullable Slot handytables$hoveredSlot();
}
