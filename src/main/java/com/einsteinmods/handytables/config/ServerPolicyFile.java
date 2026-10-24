package com.einsteinmods.handytables.config;

import com.einsteinmods.handytables.core.Station;
import com.einsteinmods.handytables.core.StationPolicy;
import org.jspecify.annotations.Nullable;

/**
 * On-disk shape of {@code config/handytables-server.json} (every server; never applied to the
 * singleplayer/LAN host). A missing flag (Gson reads it as null) means allowed, so a file written
 * by an older version keeps new stations on.
 */
public record ServerPolicyFile(
    int schemaVersion,
    @Nullable Boolean allowCraftingTable,
    @Nullable Boolean allowStonecutter,
    @Nullable Boolean allowLoom,
    @Nullable Boolean allowCartographyTable,
    @Nullable Boolean allowSmithingTable,
    @Nullable Boolean allowGrindstone,
    @Nullable Boolean allowAnvil) {
  public static final int CURRENT_SCHEMA_VERSION = 1;
  public static final String FILE_NAME = "handytables-server.json";

  public static final ServerPolicyFile DEFAULTS = from(StationPolicy.UNRESTRICTED);

  public static ServerPolicyFile from(StationPolicy policy) {
    return new ServerPolicyFile(
        CURRENT_SCHEMA_VERSION,
        policy.allows(Station.CRAFTING_TABLE),
        policy.allows(Station.STONECUTTER),
        policy.allows(Station.LOOM),
        policy.allows(Station.CARTOGRAPHY_TABLE),
        policy.allows(Station.SMITHING_TABLE),
        policy.allows(Station.GRINDSTONE),
        policy.allows(Station.ANVIL));
  }

  public StationPolicy toPolicy() {
    return StationPolicy.NONE
        .with(Station.CRAFTING_TABLE, allowedUnlessFalse(allowCraftingTable))
        .with(Station.STONECUTTER, allowedUnlessFalse(allowStonecutter))
        .with(Station.LOOM, allowedUnlessFalse(allowLoom))
        .with(Station.CARTOGRAPHY_TABLE, allowedUnlessFalse(allowCartographyTable))
        .with(Station.SMITHING_TABLE, allowedUnlessFalse(allowSmithingTable))
        .with(Station.GRINDSTONE, allowedUnlessFalse(allowGrindstone))
        .with(Station.ANVIL, allowedUnlessFalse(allowAnvil));
  }

  private static boolean allowedUnlessFalse(@Nullable Boolean flag) {
    return flag == null || flag;
  }
}
