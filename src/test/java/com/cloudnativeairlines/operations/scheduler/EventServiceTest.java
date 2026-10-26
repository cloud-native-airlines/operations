package com.cloudnativeairlines.operations.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudnativeairlines.operations.api.ApiException;
import com.cloudnativeairlines.operations.scheduler.EventService.EventInput;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    FlightInstanceRepository flightInstances;
    @Mock
    OperationalEventRepository events;

    EventService service;

    private static final String RUN = "run-1";

    @BeforeEach
    void setUp() {
        service = new EventService(flightInstances, events);
        lenient().when(flightInstances.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private FlightInstance instance() {
        return new FlightInstance("i1", RUN, "CNA1000", "RST", "MSP", "N100CA",
                Instant.parse("2026-01-01T12:00:00Z"), Instant.parse("2026-01-01T12:40:00Z"));
    }

    private EventInput departure() {
        return new EventInput("evt-dep", "i1", "N100CA", "departure",
                Instant.parse("2026-01-01T12:02:00Z"));
    }

    @Test
    void departureSetsActualAndStatus() {
        when(flightInstances.findById("i1")).thenReturn(Optional.of(instance()));
        when(events.findById(any())).thenReturn(Optional.empty());

        FlightInstance result = service.ingest(RUN, departure());

        assertThat(result.getActualDeparture()).isEqualTo(Instant.parse("2026-01-01T12:02:00Z"));
        assertThat(result.getStatus()).isEqualTo("departed");
        verify(events).save(any());
    }

    @Test
    void identicalRetryIsIdempotent() {
        FlightInstance inst = instance();
        when(flightInstances.findById("i1")).thenReturn(Optional.of(inst));
        when(events.findById(any())).thenReturn(Optional.of(new OperationalEvent(
                RUN, "evt-dep", "i1", "N100CA", "departure",
                Instant.parse("2026-01-01T12:02:00Z"), Instant.parse("2026-01-01T12:02:05Z"))));

        FlightInstance result = service.ingest(RUN, departure());

        assertThat(result).isSameAs(inst);
        verify(events, never()).save(any());
    }

    @Test
    void reusedEventIdWithDifferentContentConflicts() {
        when(flightInstances.findById("i1")).thenReturn(Optional.of(instance()));
        when(events.findById(any())).thenReturn(Optional.of(new OperationalEvent(
                RUN, "evt-dep", "i1", "N100CA", "arrival", // different type
                Instant.parse("2026-01-01T12:02:00Z"), Instant.parse("2026-01-01T12:02:05Z"))));

        assertThatThrownBy(() -> service.ingest(RUN, departure()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("conflict"));
    }

    @Test
    void outOfOrderDepartureDoesNotDowngradeArrived() {
        FlightInstance inst = instance();
        inst.setActualArrival(Instant.parse("2026-01-01T12:40:00Z"));
        inst.setStatus("arrived");
        when(flightInstances.findById("i1")).thenReturn(Optional.of(inst));
        when(events.findById(any())).thenReturn(Optional.empty());

        FlightInstance result = service.ingest(RUN, departure());

        assertThat(result.getActualDeparture()).isEqualTo(Instant.parse("2026-01-01T12:02:00Z"));
        assertThat(result.getStatus()).isEqualTo("arrived");
    }

    @Test
    void aircraftMismatchIsInvalidRequest() {
        when(flightInstances.findById("i1")).thenReturn(Optional.of(instance()));
        EventInput wrongAircraft = new EventInput("evt-dep", "i1", "N999ZZ", "departure",
                Instant.parse("2026-01-01T12:02:00Z"));

        assertThatThrownBy(() -> service.ingest(RUN, wrongAircraft))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("invalid_request"));
    }

    @Test
    void unknownTypeIsInvalidRequest() {
        EventInput badType = new EventInput("evt-x", "i1", "N100CA", "boarding",
                Instant.parse("2026-01-01T12:02:00Z"));

        assertThatThrownBy(() -> service.ingest(RUN, badType))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("invalid_request"));
    }
}
