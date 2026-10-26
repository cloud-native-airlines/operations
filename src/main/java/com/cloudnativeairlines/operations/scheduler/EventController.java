package com.cloudnativeairlines.operations.scheduler;

import com.cloudnativeairlines.operations.scheduler.EventService.EventInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runs/{runId}/events")
public class EventController {

    private final EventService events;

    public EventController(EventService events) {
        this.events = events;
    }

    public record EventRequest(
            @NotBlank String eventId,
            @NotBlank String flightInstanceId,
            @NotBlank String aircraftId,
            @NotBlank String type,
            @NotNull Instant occurredAt) {
    }

    public record EventResponse(FlightInstanceDto flight) {
    }

    @PostMapping
    public EventResponse record(@PathVariable String runId,
            @Valid @RequestBody EventRequest request) {
        FlightInstance updated = events.ingest(runId, new EventInput(
                request.eventId(), request.flightInstanceId(), request.aircraftId(),
                request.type(), request.occurredAt()));
        return new EventResponse(FlightInstanceDto.from(updated));
    }
}
