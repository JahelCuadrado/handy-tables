package com.einsteinmods.handytables.core;

/** Picks the policy that applies to one player. Pure and side-agnostic. */
public final class PlayerPolicyResolver {
  private PlayerPolicyResolver() {}

  /**
   * The singleplayer/LAN host is never limited; everyone else (LAN guests, dedicated server
   * players) follows the server policy file.
   */
  public static StationPolicy forPlayer(boolean singleplayerOwner, StationPolicy filePolicy) {
    return singleplayerOwner ? StationPolicy.UNRESTRICTED : filePolicy;
  }
}
