package com.einsteinmods.handytables.client.config;

import com.einsteinmods.handytables.config.ClientSettingsFile;
import com.einsteinmods.handytables.config.JsonConfigFile;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * The player's settings ({@code config/handytables.json}), read on first use (the first station
 * opened or the settings screen), so the game start reads nothing. Render thread only.
 */
public final class ClientSettings {
  private static @Nullable ClientSettingsFile current;

  private ClientSettings() {}

  public static ClientSettingsFile get() {
    ClientSettingsFile settings = current;
    if (settings == null) {
      settings = file().load();
      current = settings;
    }
    return settings;
  }

  public static void save(ClientSettingsFile settings) {
    file().save(settings);
    current = settings;
  }

  private static JsonConfigFile<ClientSettingsFile> file() {
    return new JsonConfigFile<>(
        FabricLoader.getInstance().getConfigDir().resolve(ClientSettingsFile.FILE_NAME),
        ClientSettingsFile.class,
        ClientSettingsFile.DEFAULTS);
  }
}
