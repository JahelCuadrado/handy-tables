package com.einsteinmods.handytables.client.mixin;

import com.einsteinmods.handytables.client.ClientStationLock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every container click goes through vanilla {@code MultiPlayerGameMode#handleContainerInput},
 * which predicts it ({@code AbstractContainerMenu#clicked}) and sends it. A click the locked
 * station slot refuses is dropped here, so it is neither predicted nor sent (no flicker); the
 * server refuses it anyway. The prediction itself runs as the server's click does, with the
 * reserved station item set aside when the slot has spares; vanilla compares the slots only after
 * it, so the slots sent are the real ones. Cost: one null check per click while no station is open.
 */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
  @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
  private void handytables$refuseLockedSlot(
      int containerId,
      int slotNum,
      int buttonNum,
      ContainerInput containerInput,
      Player player,
      CallbackInfo ci) {
    if (ClientStationLock.refuses(containerId, slotNum, buttonNum, containerInput, player)) {
      ci.cancel();
    }
  }

  @WrapOperation(
      method = "handleContainerInput",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"))
  private void handytables$predictWithoutReserved(
      AbstractContainerMenu menu,
      int slotNum,
      int buttonNum,
      ContainerInput containerInput,
      Player player,
      Operation<Void> clicked) {
    if (ClientStationLock.guards(menu)) {
      ClientStationLock.clickWithoutReserved(
          menu,
          player,
          containerInput,
          () -> clicked.call(menu, slotNum, buttonNum, containerInput, player));
    } else {
      clicked.call(menu, slotNum, buttonNum, containerInput, player);
    }
  }
}
