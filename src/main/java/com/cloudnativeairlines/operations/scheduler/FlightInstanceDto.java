package com.cloudnativeairlines.operations.scheduler;

import java.time.Instant;

/** A scheduled leg as returned by the schedule queries: scheduled vs. actual. */
public record FlightInstanceDto(
        String id,
        String runId,
        String flightNumber,
        String originCode,
        String destinationCode,
        String aircraftId,
        Instant scheduledDeparture,
        Instant scheduledArrival,
        Instant actualDeparture,
        Instant actualArrival,
        String status) {

    public static FlightInstanceDto from(FlightInstance i) {
        return new FlightInstanceDto(
                i.getId(),
                i.getRunId(),
                i.getFlightNumber(),
                i.getOriginCode(),
                i.getDestinationCode(),
                i.getAircraftId(),
                i.getScheduledDeparture(),
                i.getScheduledArrival(),
                i.getActualDeparture(),
                i.getActualArrival(),
                i.getStatus());
    }
}
