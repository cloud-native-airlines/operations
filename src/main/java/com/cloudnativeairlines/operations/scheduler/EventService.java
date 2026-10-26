package com.cloudnativeairlines.operations.scheduler;

import com.cloudnativeairlines.operations.api.ApiException;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests departure/arrival events, updating a flight instance's actual times and
 * status. Identical retries are idempotent; a reused event id with different
 * content is a conflict; out-of-order events preserve a terminal {@code arrived}
 * status. Actual times come from the event's simulated {@code occurredAt}.
 */
@Service
public class EventService {

    private static final Set<String> TYPES = Set.of("departure", "arrival");

    private final FlightInstanceRepository flightInstances;
    private final OperationalEventRepository events;

    public EventService(FlightInstanceRepository flightInstances,
            OperationalEventRepository events) {
        this.flightInstances = flightInstances;
        this.events = events;
    }

    /** A departure/arrival to record. {@code occurredAt} is simulated time. */
    public record EventInput(String eventId, String flightInstanceId, String aircraftId,
            String type, Instant occurredAt) {
    }

    @Transactional
    public FlightInstance ingest(String runId, EventInput input) {
        if (!TYPES.contains(input.type())) {
            throw ApiException.invalidRequest("type must be one of departure, arrival");
        }

        FlightInstance instance = flightInstances.findById(input.flightInstanceId())
                .orElseThrow(() -> ApiException.notFound(
                        "No flight instance " + input.flightInstanceId()));
        if (!instance.getRunId().equals(runId)) {
            throw ApiException.invalidRequest(
                    "Flight instance " + input.flightInstanceId() + " is not part of run " + runId);
        }
        if (!instance.getAircraftId().equals(input.aircraftId())) {
            throw ApiException.invalidRequest(
                    "Event aircraft " + input.aircraftId() + " does not match the flight's aircraft "
                            + instance.getAircraftId());
        }

        var existing = events.findById(new OperationalEvent.Key(runId, input.eventId()));
        if (existing.isPresent()) {
            if (sameContent(existing.get(), runId, input)) {
                return instance; // idempotent retry
            }
            throw ApiException.conflict(
                    "Event " + input.eventId() + " already recorded with different content");
        }

        events.save(new OperationalEvent(runId, input.eventId(), input.flightInstanceId(),
                input.aircraftId(), input.type(), input.occurredAt(), Instant.now()));

        apply(instance, input);
        return flightInstances.save(instance);
    }

    private void apply(FlightInstance instance, EventInput input) {
        if ("departure".equals(input.type())) {
            if (instance.getActualDeparture() == null) {
                instance.setActualDeparture(input.occurredAt());
            }
            // Never downgrade a terminal 'arrived' back to 'departed'.
            if ("scheduled".equals(instance.getStatus())) {
                instance.setStatus("departed");
            }
        } else { // arrival
            if (instance.getActualArrival() == null) {
                instance.setActualArrival(input.occurredAt());
            }
            instance.setStatus("arrived");
        }
    }

    private boolean sameContent(OperationalEvent e, String runId, EventInput input) {
        return e.getRunId().equals(runId)
                && e.getFlightInstanceId().equals(input.flightInstanceId())
                && e.getAircraftId().equals(input.aircraftId())
                && e.getType().equals(input.type())
                && Objects.equals(e.getOccurredAt(), input.occurredAt());
    }
}
