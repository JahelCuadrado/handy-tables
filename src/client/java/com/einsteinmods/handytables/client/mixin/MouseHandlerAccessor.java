package com.einsteinmods.handytables.client.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Writes the cursor position vanilla keeps ({@code MouseHandler#xpos}, {@code #ypos}). */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
  @Accessor("xpos")
  void handytables$setXpos(double xpos);

  @Accessor("ypos")
  void handytables$setYpos(double ypos);
}
