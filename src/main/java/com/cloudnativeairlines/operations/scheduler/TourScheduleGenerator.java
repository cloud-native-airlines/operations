package com.cloudnativeairlines.operations.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Materializes a run's flight instances by walking a repeating tour template from
 * {@code start} until {@code horizonEnd}, chaining legs so each arrival plus the
 * leg's turnaround becomes the next departure. The four flight numbers recur every
 * cycle; each instance gets a unique id. Pure and DB-free so it can be unit-tested.
 */
public final class TourScheduleGenerator {

    private TourScheduleGenerator() {
    }

    /** A resolved tour leg: flight number, geography, and timing in minutes. */
    public record LegTemplate(
            String flightNumber,
            String originCode,
            String destinationCode,
            int blockMinutes,
            int turnaroundMinutes) {
    }

    public static List<FlightInstance> generate(String runId, Instant start, Instant horizonEnd,
            String aircraftId, List<LegTemplate> legs) {
        List<FlightInstance> instances = new ArrayList<>();
        if (legs.isEmpty()) {
            return instances;
        }

        Instant cursor = start;
        int counter = 0;
        while (true) {
            for (LegTemplate leg : legs) {
                Instant departure = cursor;
                if (!departure.isBefore(horizonEnd)) {
                    return instances;
                }
                Instant arrival = departure.plus(Duration.ofMinutes(leg.blockMinutes()));
                String id = runId + "-" + String.format("%04d", counter++);
                instances.add(new FlightInstance(id, runId, leg.flightNumber(),
                        leg.originCode(), leg.destinationCode(), aircraftId, departure, arrival));
                cursor = arrival.plus(Duration.ofMinutes(leg.turnaroundMinutes()));
            }
        }
    }
}
