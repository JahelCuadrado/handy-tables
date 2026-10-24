package com.einsteinmods.handytables.config;

import org.jspecify.annotations.Nullable;

/**
 * On-disk shape of {@code config/handytables.json}, the player's own settings (client only). A
 * missing field (Gson reads it as null) takes its default, so a file written by an older version
 * keeps working.
 *
 * @param openSound play the station's placing sound when it opens from its item; default on
 */
public record ClientSettingsFile(int schemaVersion, @Nullable Boolean openSound) {
  public static final int CURRENT_SCHEMA_VERSION = 1;
  public static final String FILE_NAME = "handytables.json";
  public static final boolean DEFAULT_OPEN_SOUND = true;

  public static final ClientSettingsFile DEFAULTS = of(DEFAULT_OPEN_SOUND);

  public static ClientSettingsFile of(boolean openSound) {
    return new ClientSettingsFile(CURRENT_SCHEMA_VERSION, openSound);
  }

  public boolean playsOpenSound() {
    return openSound == null ? DEFAULT_OPEN_SOUND : openSound;
  }
}
