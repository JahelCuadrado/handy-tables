package com.einsteinmods.handytables.gametest;

import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.menu.ReturnTarget;
import com.einsteinmods.handytables.menu.StationMenus;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Players and menus shared by the GameTests. */
final class HandyFixtures {
  private static final Vec3 PLAYER_POSITION = new Vec3(0.5, 1.0, 0.5);
  private static final String PLAYER_NAME = "handytables-test";
  private static final int CONTAINER_ID = 1;

  private HandyFixtures() {}

  /**
   * A Fabric fake player (it has a packet listener; {@code GameTestHelper#makeMockServerPlayer} is
   * {@code forRemoval}) with a fresh UUID, in {@code gameType}, with an empty inventory. Its {@code
   * openMenu} does nothing, so tests build menus with {@link #open}.
   */
  static ServerPlayer player(GameTestHelper helper, GameType gameType) {
    ServerPlayer player =
        FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), PLAYER_NAME));
    player.setGameMode(gameType);
    player.setPos(helper.absoluteVec(PLAYER_POSITION));
    player.getInventory().clearContent();
    return player;
  }

  /** The menu {@code provider} builds for {@code player}, made the player's open menu. */
  static AbstractContainerMenu open(ServerPlayer player, MenuProvider provider) {
    AbstractContainerMenu menu = provider.createMenu(CONTAINER_ID, player.getInventory(), player);
    player.containerMenu = menu;
    return menu;
  }

  /** {@code station} opened from {@code inventorySlot}, as the request handler builds it. */
  static AbstractContainerMenu open(ServerPlayer player, Station station, int inventorySlot) {
    return open(player, StationMenus.provider(station, inventorySlot, ReturnTarget.INVENTORY));
  }
}
