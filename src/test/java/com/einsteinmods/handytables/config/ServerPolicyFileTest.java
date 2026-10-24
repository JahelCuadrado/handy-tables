package com.einsteinmods.handytables.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.core.StationPolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerPolicyFileTest {
  private static final String LOOM_OFF_JSON =
      """
      {
        "schemaVersion": 1,
        "allowLoom": false
      }
      """;

  /** A file from before the grindstone and anvil existed: their flags are missing. */
  private static final String WITHOUT_GRINDSTONE_FLAG_JSON =
      """
      {
        "schemaVersion": 1,
        "allowCraftingTable": true,
        "allowStonecutter": true,
        "allowLoom": true,
        "allowCartographyTable": true,
        "allowSmithingTable": false
      }
      """;

  private static final String GRINDSTONE_OFF_JSON =
      """
      {
        "schemaVersion": 1,
        "allowGrindstone": false
      }
      """;

  private static final String ANVIL_OFF_JSON =
      """
      {
        "schemaVersion": 1,
        "allowAnvil": false
      }
      """;

  private static final String BROKEN_JSON = "{ \"allowLoom\": ";

  private static JsonConfigFile<ServerPolicyFile> file(Path directory) {
    return new JsonConfigFile<>(
        directory.resolve(ServerPolicyFile.FILE_NAME),
        ServerPolicyFile.class,
        ServerPolicyFile.DEFAULTS);
  }

  @Test
  void missingFileIsWrittenWithEverythingAllowed(@TempDir Path directory) {
    assertEquals(StationPolicy.UNRESTRICTED, file(directory).load().toPolicy());
    assertEquals(StationPolicy.UNRESTRICTED, file(directory).load().toPolicy());
  }

  @Test
  void missingFlagsAreAllowedAndFalseFlagsForbid(@TempDir Path directory) throws IOException {
    Files.writeString(
        directory.resolve(ServerPolicyFile.FILE_NAME), LOOM_OFF_JSON, StandardCharsets.UTF_8);

    assertEquals(
        StationPolicy.UNRESTRICTED.with(Station.LOOM, false), file(directory).load().toPolicy());
  }

  @Test
  void anOlderFileKeepsNewerStationsAllowed(@TempDir Path directory) throws IOException {
    Files.writeString(
        directory.resolve(ServerPolicyFile.FILE_NAME),
        WITHOUT_GRINDSTONE_FLAG_JSON,
        StandardCharsets.UTF_8);

    StationPolicy policy = file(directory).load().toPolicy();

    assertTrue(policy.allows(Station.GRINDSTONE));
    assertTrue(policy.allows(Station.ANVIL));
    assertFalse(policy.allows(Station.SMITHING_TABLE));
  }

  @Test
  void grindstoneFlagForbidsOnlyTheGrindstone(@TempDir Path directory) throws IOException {
    Files.writeString(
        directory.resolve(ServerPolicyFile.FILE_NAME), GRINDSTONE_OFF_JSON, StandardCharsets.UTF_8);

    assertEquals(
        StationPolicy.UNRESTRICTED.with(Station.GRINDSTONE, false),
        file(directory).load().toPolicy());
  }

  @Test
  void anvilFlagForbidsOnlyTheAnvil(@TempDir Path directory) throws IOException {
    Files.writeString(
        directory.resolve(ServerPolicyFile.FILE_NAME), ANVIL_OFF_JSON, StandardCharsets.UTF_8);

    assertEquals(
        StationPolicy.UNRESTRICTED.with(Station.ANVIL, false), file(directory).load().toPolicy());
  }

  @Test
  void brokenFileFallsBackToEverythingAllowed(@TempDir Path directory) throws IOException {
    Files.writeString(
        directory.resolve(ServerPolicyFile.FILE_NAME), BROKEN_JSON, StandardCharsets.UTF_8);

    assertEquals(StationPolicy.UNRESTRICTED, file(directory).load().toPolicy());
  }

  @Test
  void savedPolicyLoadsBack(@TempDir Path directory) {
    StationPolicy policy =
        StationPolicy.NONE.with(Station.STONECUTTER, true).with(Station.SMITHING_TABLE, true);
    file(directory).save(ServerPolicyFile.from(policy));

    assertEquals(policy, file(directory).load().toPolicy());
  }
}
