package com.cloudnativeairlines.operations.fleet;

import com.cloudnativeairlines.operations.api.ApiException;
import com.cloudnativeairlines.operations.scenario.Aircraft;
import com.cloudnativeairlines.operations.scenario.AircraftRepository;
import com.cloudnativeairlines.operations.scheduler.FlightInstance;
import com.cloudnativeairlines.operations.scheduler.FlightInstanceRepository;
import com.cloudnativeairlines.operations.sim.SimClient;
import com.cloudnativeairlines.operations.sim.SimClock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fleet roster with current-location derivation for a resolved run and time. */
@Service
public class FleetService {

    private final AircraftRepository aircraft;
    private final FlightInstanceRepository flightInstances;
    private final SimClient sim;

    public FleetService(AircraftRepository aircraft, FlightInstanceRepository flightInstances,
            SimClient sim) {
        this.aircraft = aircraft;
        this.flightInstances = flightInstances;
        this.sim = sim;
    }

    /** The run and simulated time a request resolves against. */
    public record Context(String runId, Instant now) {
    }

    /**
     * Resolve the run and "now": explicit {@code runId}/{@code at} override the
     * Simulator clock, which supplies whichever is missing.
     */
    public Context resolveContext(String runId, Instant at) {
        if (runId != null && at != null) {
            return new Context(runId, at);
        }
        SimClock clock = sim.clock();
        return new Context(
                runId != null ? runId : clock.runId(),
                at != null ? at : clock.simulatedAt());
    }

    @Transactional(readOnly = true)
    public List<FleetAircraftDto> list(String runId, Instant at) {
        Context ctx = resolveContext(runId, at);
        return aircraft.findAll(Sort.by("id")).stream()
                .map(a -> FleetAircraftDto.of(a, locationFor(a, ctx)))
                .toList();
    }

    @Transactional(readOnly = true)
    public FleetAircraftDto get(String id, String runId, Instant at) {
        Aircraft a = aircraft.findById(id)
                .orElseThrow(() -> ApiException.notFound("No aircraft with id " + id));
        Context ctx = resolveContext(runId, at);
        return FleetAircraftDto.of(a, locationFor(a, ctx));
    }

    private AircraftLocation locationFor(Aircraft a, Context ctx) {
        List<FlightInstance> instances = flightInstances
                .findByRunIdAndAircraftIdOrderByScheduledDepartureAsc(ctx.runId(), a.getId());
        return LocationResolver.resolve(a.getBaseAirportCode(), instances, ctx.now());
    }
}
