package com.einsteinmods.handytables.core;

/**
 * Stations a server lets one player open from their item, as a bit mask of {@link Station#mask()}.
 * Bits of unknown stations are dropped, so a newer peer never enables something this version lacks.
 *
 * @param allowedMask one bit per allowed station
 */
public record StationPolicy(int allowedMask) {
  private static final int KNOWN_MASK = knownMask();

  /** Every station allowed: the default policy file and the singleplayer/LAN host. */
  public static final StationPolicy UNRESTRICTED = new StationPolicy(KNOWN_MASK);

  /** No station allowed: the client's state until a server with the mod sends its policy. */
  public static final StationPolicy NONE = new StationPolicy(0);

  public StationPolicy {
    allowedMask &= KNOWN_MASK;
  }

  public boolean allows(Station station) {
    return (allowedMask & station.mask()) != 0;
  }

  /** True when no station is allowed: the client then leaves every click to vanilla. */
  public boolean allowsNone() {
    return allowedMask == 0;
  }

  /** This policy with {@code station} allowed or forbidden. */
  public StationPolicy with(Station station, boolean allowed) {
    return new StationPolicy(
        allowed ? allowedMask | station.mask() : allowedMask & ~station.mask());
  }

  private static int knownMask() {
    int mask = 0;
    for (Station station : Station.values()) {
      mask |= station.mask();
    }
    return mask;
  }
}
