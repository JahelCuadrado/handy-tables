package com.einsteinmods.handytables.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientSettingsFileTest {
  @Test
  void aMissingFieldTakesItsDefault() {
    assertTrue(
        new ClientSettingsFile(ClientSettingsFile.CURRENT_SCHEMA_VERSION, null).playsOpenSound());
  }

  @Test
  void theOpenSoundCanBeTurnedOff() {
    assertFalse(ClientSettingsFile.of(false).playsOpenSound());
    assertTrue(ClientSettingsFile.DEFAULTS.playsOpenSound());
  }
}
