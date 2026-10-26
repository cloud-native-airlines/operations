package com.cloudnativeairlines.operations.fleet;

/**
 * Where an aircraft is right now within a run: parked at an airport, flying a leg,
 * or unknown (no schedule for the run). {@code source} is {@code "event"} when
 * derived from reported events, {@code "schedule"} when from the planned timeline.
 */
public record AircraftLocation(String state, String airport, Flight flight, String source) {

    /** The leg an aircraft is flying, when {@code state == "flying"}. */
    public record Flight(String flightInstanceId, String flightNumber, String origin,
            String destination) {
    }

    public static AircraftLocation unknown() {
        return new AircraftLocation("unknown", null, null, null);
    }

    public static AircraftLocation atAirport(String airport, String source) {
        return new AircraftLocation("at_airport", airport, null, source);
    }

    public static AircraftLocation flying(Flight flight, String source) {
        return new AircraftLocation("flying", null, flight, source);
    }
}
