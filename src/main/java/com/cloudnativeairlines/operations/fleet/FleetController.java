package com.cloudnativeairlines.operations.fleet;

import com.cloudnativeairlines.operations.api.Pagination;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fleet tracking: the airline's aircraft and each one's current location. The run
 * and time default to the Simulator clock; {@code run_id} and {@code at} override.
 */
@RestController
@RequestMapping("/api/v1/aircraft")
public class FleetController {

    private final FleetService fleet;

    public FleetController(FleetService fleet) {
        this.fleet = fleet;
    }

    public record FleetListResponse(List<FleetAircraftDto> aircraft, Pagination pagination) {
    }

    public record FleetResponse(FleetAircraftDto aircraft) {
    }

    @GetMapping
    public FleetListResponse list(
            @RequestParam(name = "run_id", required = false) String runId,
            @RequestParam(name = "at", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        List<FleetAircraftDto> items = fleet.list(runId, at);
        return new FleetListResponse(items, Pagination.of(items.size()));
    }

    @GetMapping("/{id}")
    public FleetResponse get(
            @PathVariable String id,
            @RequestParam(name = "run_id", required = false) String runId,
            @RequestParam(name = "at", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        return new FleetResponse(fleet.get(id, runId, at));
    }
}
