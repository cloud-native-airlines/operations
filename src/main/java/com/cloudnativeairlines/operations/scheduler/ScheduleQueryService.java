package com.cloudnativeairlines.operations.scheduler;

import com.cloudnativeairlines.operations.api.ApiException;
import com.cloudnativeairlines.operations.sim.SimClient;
import com.cloudnativeairlines.operations.sim.SimClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only schedule queries: by aircraft, by airport (FIDS board), and an
 * upcoming window across the fleet. The run and time default to the Simulator
 * clock; {@code run_id} and {@code at} override, and a window is given either as
 * an explicit {@code from}/{@code to} or as {@code within} hours from "now".
 */
@Service
public class ScheduleQueryService {

    private final FlightInstanceRepository flightInstances;
    private final SimClient sim;

    public ScheduleQueryService(FlightInstanceRepository flightInstances, SimClient sim) {
        this.flightInstances = flightInstances;
        this.sim = sim;
    }

    public enum Direction {
        DEPARTURES, ARRIVALS, BOTH
    }

    /** A half-open window [from, to); either bound may be null (unbounded). */
    private record Window(String runId, Instant from, Instant to) {
        boolean contains(Instant t) {
            return (from == null || !t.isBefore(from)) && (to == null || t.isBefore(to));
        }
    }

    @Transactional(readOnly = true)
    public List<FlightInstanceDto> forAircraft(String aircraftId, String runId, Instant from,
            Instant to, Integer within, Instant at) {
        Window w = resolveWindow(runId, from, to, within, at);
        return flightInstances
                .findByRunIdAndAircraftIdOrderByScheduledDepartureAsc(w.runId(), aircraftId).stream()
                .filter(i -> w.contains(i.getScheduledDeparture()))
                .map(FlightInstanceDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlightInstanceDto> forAirport(String code, Direction direction, String runId,
            Instant from, Instant to, Integer within, Instant at) {
        Window w = resolveWindow(runId, from, to, within, at);
        return flightInstances.findByRunIdOrderByScheduledDepartureAsc(w.runId()).stream()
                .filter(i -> matchesAirport(i, code, direction, w))
                .sorted(Comparator.comparing(i -> boardTime(i, code, direction)))
                .map(FlightInstanceDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlightInstanceDto> upcoming(String runId, Instant from, Instant to, Integer within,
            Instant at) {
        Window w = resolveWindow(runId, from, to, within, at);
        return flightInstances.findByRunIdOrderByScheduledDepartureAsc(w.runId()).stream()
                .filter(i -> w.contains(i.getScheduledDeparture()))
                .map(FlightInstanceDto::from)
                .toList();
    }

    private boolean matchesAirport(FlightInstance i, String code, Direction direction, Window w) {
        boolean dep = i.getOriginCode().equals(code) && w.contains(i.getScheduledDeparture());
        boolean arr = i.getDestinationCode().equals(code) && w.contains(i.getScheduledArrival());
        return switch (direction) {
            case DEPARTURES -> dep;
            case ARRIVALS -> arr;
            case BOTH -> dep || arr;
        };
    }

    private Instant boardTime(FlightInstance i, String code, Direction direction) {
        if (direction == Direction.ARRIVALS) {
            return i.getScheduledArrival();
        }
        if (direction == Direction.DEPARTURES) {
            return i.getScheduledDeparture();
        }
        // BOTH: order by whichever time applies to this airport.
        return i.getOriginCode().equals(code) ? i.getScheduledDeparture() : i.getScheduledArrival();
    }

    private Window resolveWindow(String runId, Instant from, Instant to, Integer within,
            Instant at) {
        if (within != null && within <= 0) {
            throw ApiException.invalidRequest("within must be a positive number of hours");
        }
        if (within != null && (from != null || to != null)) {
            throw ApiException.invalidRequest("Provide either from/to or within, not both");
        }

        String effectiveRun = runId;
        Instant effectiveNow = at;
        boolean needNow = within != null && effectiveNow == null;
        if (effectiveRun == null || needNow) {
            SimClock clock = sim.clock();
            if (effectiveRun == null) {
                effectiveRun = clock.runId();
            }
            if (effectiveNow == null) {
                effectiveNow = clock.simulatedAt();
            }
        }

        if (within != null) {
            return new Window(effectiveRun, effectiveNow, effectiveNow.plus(Duration.ofHours(within)));
        }
        return new Window(effectiveRun, from, to);
    }
}
