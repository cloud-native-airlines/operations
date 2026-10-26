package com.cloudnativeairlines.operations.scheduler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A recorded departure/arrival event, keyed by {@code (run_id, event_id)}. Feeds
 * actual times and status. Ingestion lands in a later slice; the mapping exists
 * now so run resets can clear it.
 */
@Entity
@Table(name = "operational_event")
@IdClass(OperationalEvent.Key.class)
public class OperationalEvent {

    @Id
    @Column(name = "run_id")
    private String runId;

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "flight_instance_id", nullable = false)
    private String flightInstanceId;

    @Column(name = "aircraft_id", nullable = false)
    private String aircraftId;

    @Column(nullable = false)
    private String type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected OperationalEvent() {
    }

    public OperationalEvent(String runId, String eventId, String flightInstanceId,
            String aircraftId, String type, Instant occurredAt, Instant receivedAt) {
        this.runId = runId;
        this.eventId = eventId;
        this.flightInstanceId = flightInstanceId;
        this.aircraftId = aircraftId;
        this.type = type;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
    }

    public String getRunId() {
        return runId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getFlightInstanceId() {
        return flightInstanceId;
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public String getType() {
        return type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    /** Composite primary key. */
    public static class Key implements Serializable {
        private String runId;
        private String eventId;

        public Key() {
        }

        public Key(String runId, String eventId) {
            this.runId = runId;
            this.eventId = eventId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(runId, key.runId) && Objects.equals(eventId, key.eventId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(runId, eventId);
        }
    }
}
