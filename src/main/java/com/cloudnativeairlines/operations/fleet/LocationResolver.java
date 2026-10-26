package com.cloudnativeairlines.operations.fleet;

import com.cloudnativeairlines.operations.fleet.AircraftLocation.Flight;
import com.cloudnativeairlines.operations.scheduler.FlightInstance;
import java.time.Instant;
import java.util.List;

/**
 * Derives an aircraft's current location from its run schedule plus any reported
 * actual times. Events win when present; otherwise the scheduled timeline at
 * {@code now} is used. Pure and DB-free so it can be unit-tested.
 */
public final class LocationResolver {

    private LocationResolver() {
    }

    /**
     * @param baseAirport the aircraft's base, used before its first scheduled leg
     * @param instances   the aircraft's instances for one run, sorted by scheduled
     *                    departure ascending
     * @param now         the current simulated time
     */
    public static AircraftLocation resolve(String baseAirport, List<FlightInstance> instances,
            Instant now) {
        if (instances.isEmpty()) {
            return AircraftLocation.unknown();
        }

        // Event-driven: mid-leg (departed, not yet arrived) wins.
        FlightInstance airborne = instances.stream()
                .filter(i -> i.getActualDeparture() != null && i.getActualArrival() == null)
                .findFirst()
                .orElse(null);
        if (airborne != null) {
            return AircraftLocation.flying(flightOf(airborne), "event");
        }

        // Event-driven: otherwise the most recently completed leg's destination.
        FlightInstance lastArrived = instances.stream()
                .filter(i -> i.getActualArrival() != null)
                .reduce((a, b) -> a.getActualArrival().isAfter(b.getActualArrival()) ? a : b)
                .orElse(null);
        if (lastArrived != null) {
            return AircraftLocation.atAirport(lastArrived.getDestinationCode(), "event");
        }

        // Schedule fallback.
        FlightInstance first = instances.get(0);
        if (now.isBefore(first.getScheduledDeparture())) {
            return AircraftLocation.atAirport(baseAirport, "schedule");
        }
        for (FlightInstance i : instances) {
            if (!now.isBefore(i.getScheduledDeparture()) && now.isBefore(i.getScheduledArrival())) {
                return AircraftLocation.flying(flightOf(i), "schedule");
            }
        }
        // Between legs or after the last: at the most recent completed leg's destination.
        FlightInstance landed = null;
        for (FlightInstance i : instances) {
            if (!i.getScheduledArrival().isAfter(now)) {
                landed = i;
            }
        }
        String airport = landed != null ? landed.getDestinationCode() : baseAirport;
        return AircraftLocation.atAirport(airport, "schedule");
    }

    private static Flight flightOf(FlightInstance i) {
        return new Flight(i.getId(), i.getFlightNumber(), i.getOriginCode(), i.getDestinationCode());
    }
}
