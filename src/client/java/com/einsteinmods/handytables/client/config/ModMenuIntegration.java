package com.einsteinmods.handytables.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu "Configure" button; only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return HandyTablesSettingsScreen::new;
  }
}
