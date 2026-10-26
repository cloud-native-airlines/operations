package com.cloudnativeairlines.operations.scheduler;

import com.cloudnativeairlines.operations.api.ApiException;
import com.cloudnativeairlines.operations.scenario.Aircraft;
import com.cloudnativeairlines.operations.scenario.AircraftRepository;
import com.cloudnativeairlines.operations.scenario.Route;
import com.cloudnativeairlines.operations.scenario.RouteRepository;
import com.cloudnativeairlines.operations.scenario.TourLeg;
import com.cloudnativeairlines.operations.scenario.TourLegRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds, summarizes, and resets a run's materialized schedule. */
@Service
public class ScheduleService {

    private static final String TOUR_ID = "grand-tour";

    private final TourLegRepository tourLegs;
    private final RouteRepository routes;
    private final AircraftRepository aircraft;
    private final RunRepository runs;
    private final FlightInstanceRepository flightInstances;
    private final OperationalEventRepository events;
    private final int defaultHorizonDays;

    public ScheduleService(TourLegRepository tourLegs, RouteRepository routes,
            AircraftRepository aircraft, RunRepository runs,
            FlightInstanceRepository flightInstances, OperationalEventRepository events,
            @Value("${operations.schedule.horizon-days:7}") int defaultHorizonDays) {
        this.tourLegs = tourLegs;
        this.routes = routes;
        this.aircraft = aircraft;
        this.runs = runs;
        this.flightInstances = flightInstances;
        this.events = events;
        this.defaultHorizonDays = defaultHorizonDays;
    }

    /** Summary of a run's schedule. */
    public record RunSummary(String runId, Instant start, int horizonDays, long flightCount) {
    }

    @Transactional
    public RunSummary buildRun(String runId, Instant start, Integer horizonDays) {
        if (runs.existsById(runId) || flightInstances.existsByRunId(runId)) {
            throw ApiException.conflict(
                    "Run " + runId + " is already initialized; DELETE it first to rebuild");
        }
        int horizon = horizonDays != null ? horizonDays : defaultHorizonDays;
        if (horizon <= 0) {
            throw ApiException.invalidRequest("horizon_days must be positive");
        }

        List<TourLeg> legs = tourLegs.findByTourIdOrderBySeqAsc(TOUR_ID);
        if (legs.isEmpty()) {
            throw ApiException.invalidRequest("No tour template '" + TOUR_ID + "' is configured");
        }

        Map<String, Route> routesById = routes.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Route::getId, Function.identity()));

        List<TourScheduleGenerator.LegTemplate> templates = legs.stream()
                .map(leg -> {
                    Route route = routesById.get(leg.getRouteId());
                    if (route == null) {
                        throw ApiException.invalidRequest(
                                "Tour leg references unknown route " + leg.getRouteId());
                    }
                    return new TourScheduleGenerator.LegTemplate(
                            leg.getFlightNumber(),
                            route.getOriginCode(),
                            route.getDestinationCode(),
                            route.getBlockMinutes(),
                            leg.getTurnaroundMinutes());
                })
                .toList();

        String aircraftId = pickAircraft(templates.get(0).originCode());
        Instant horizonEnd = start.plus(Duration.ofDays(horizon));

        List<FlightInstance> instances = TourScheduleGenerator.generate(
                runId, start, horizonEnd, aircraftId, templates);

        runs.save(new Run(runId, start, horizon, Instant.now()));
        flightInstances.saveAll(instances);

        return new RunSummary(runId, start, horizon, instances.size());
    }

    @Transactional(readOnly = true)
    public RunSummary getRun(String runId) {
        Run run = runs.findById(runId)
                .orElseThrow(() -> ApiException.notFound("No run with id " + runId));
        return new RunSummary(run.getRunId(), run.getStartAt(), run.getHorizonDays(),
                flightInstances.countByRunId(runId));
    }

    @Transactional
    public void resetRun(String runId) {
        events.deleteByRunId(runId);
        flightInstances.deleteByRunId(runId);
        runs.deleteByRunId(runId);
    }

    private String pickAircraft(String originCode) {
        List<Aircraft> fleet = aircraft.findAll();
        if (fleet.isEmpty()) {
            throw ApiException.invalidRequest("No aircraft are configured to assign");
        }
        return fleet.stream()
                .filter(a -> originCode.equals(a.getBaseAirportCode()))
                .map(Aircraft::getId)
                .findFirst()
                .orElseGet(() -> fleet.stream()
                        .map(Aircraft::getId)
                        .min(Comparator.naturalOrder())
                        .orElseThrow());
    }
}
