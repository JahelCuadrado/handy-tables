package com.einsteinmods.handytables.mixin;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The first half of a double chest's container ({@code CompoundContainer#container1}). */
@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {
  @Accessor("container1")
  Container handytables$first();
}
