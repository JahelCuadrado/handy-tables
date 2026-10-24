package com.einsteinmods.handytables.menu;

/**
 * A vanilla station menu opened from its item ({@code Portable*Menu}): what the server needs to
 * find again while it is open (the open request, the lock sync, closing it back).
 */
public interface PortableStationMenu {
  StationLock stationLock();

  StationItemCheck stationCheck();
}
