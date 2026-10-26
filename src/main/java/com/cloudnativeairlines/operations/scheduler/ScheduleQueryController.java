package com.cloudnativeairlines.operations.scheduler;

import com.cloudnativeairlines.operations.api.ApiException;
import com.cloudnativeairlines.operations.api.Pagination;
import com.cloudnativeairlines.operations.scheduler.ScheduleQueryService.Direction;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScheduleQueryController {

    private final ScheduleQueryService queries;

    public ScheduleQueryController(ScheduleQueryService queries) {
        this.queries = queries;
    }

    public record FlightListResponse(List<FlightInstanceDto> flights, Pagination pagination) {
    }

    @GetMapping("/api/v1/aircraft/{id}/flights")
    public FlightListResponse aircraftFlights(
            @PathVariable String id,
            @RequestParam(name = "run_id", required = false) String runId,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "within", required = false) Integer within,
            @RequestParam(name = "at", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        return envelope(queries.forAircraft(id, runId, from, to, within, at));
    }

    @GetMapping("/api/v1/airports/{code}/flights")
    public FlightListResponse airportFlights(
            @PathVariable String code,
            @RequestParam(name = "direction", required = false, defaultValue = "both") String direction,
            @RequestParam(name = "run_id", required = false) String runId,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "within", required = false) Integer within,
            @RequestParam(name = "at", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        return envelope(queries.forAirport(code, parseDirection(direction), runId, from, to, within, at));
    }

    @GetMapping("/api/v1/flights")
    public FlightListResponse flights(
            @RequestParam(name = "run_id", required = false) String runId,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "within", required = false) Integer within,
            @RequestParam(name = "at", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        return envelope(queries.upcoming(runId, from, to, within, at));
    }

    private FlightListResponse envelope(List<FlightInstanceDto> flights) {
        return new FlightListResponse(flights, Pagination.of(flights.size()));
    }

    private Direction parseDirection(String raw) {
        try {
            return Direction.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.invalidRequest(
                    "direction must be one of departures, arrivals, both");
        }
    }
}
