package com.cloudnativeairlines.operations.fleet;

import com.cloudnativeairlines.operations.scenario.Aircraft;

/** An aircraft plus its current location for the resolved run and time. */
public record FleetAircraftDto(
        String id,
        String type,
        String baseAirportCode,
        AircraftLocation location) {

    public static FleetAircraftDto of(Aircraft aircraft, AircraftLocation location) {
        return new FleetAircraftDto(
                aircraft.getId(),
                aircraft.getType(),
                aircraft.getBaseAirportCode(),
                location);
    }
}
