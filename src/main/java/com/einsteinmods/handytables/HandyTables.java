package com.einsteinmods.handytables;

import com.einsteinmods.handytables.config.JsonConfigFile;
import com.einsteinmods.handytables.config.ServerPolicyFile;
import com.einsteinmods.handytables.core.StationPolicy;
import com.einsteinmods.handytables.network.HandyTablesNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/** Common (client + server) entrypoint. */
public final class HandyTables implements ModInitializer {
  public static final String MOD_ID = "handytables";

  @Override
  public void onInitialize() {
    HandyTablesNetworking.register();
    ServerLifecycleEvents.SERVER_STARTING.register(
        server -> HandyTablesNetworking.setFilePolicy(loadFilePolicy()));
    ServerLifecycleEvents.SERVER_STOPPED.register(
        server -> HandyTablesNetworking.setFilePolicy(StationPolicy.UNRESTRICTED));
  }

  public static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(MOD_ID, path);
  }

  /**
   * Every server (dedicated or integrated) reads its policy file; the singleplayer/LAN host is
   * exempted per player in {@link HandyTablesNetworking#policyFor}.
   */
  private static StationPolicy loadFilePolicy() {
    return new JsonConfigFile<>(
            FabricLoader.getInstance().getConfigDir().resolve(ServerPolicyFile.FILE_NAME),
            ServerPolicyFile.class,
            ServerPolicyFile.DEFAULTS)
        .load()
        .toPolicy();
  }
}
