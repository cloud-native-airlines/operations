package com.cloudnativeairlines.operations.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloudnativeairlines.operations.scheduler.TourScheduleGenerator.LegTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TourScheduleGeneratorTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private static final List<LegTemplate> TOUR = List.of(
            new LegTemplate("CNA1000", "RST", "MSP", 40, 60),
            new LegTemplate("CNA0101", "MSP", "SFO", 200, 60),
            new LegTemplate("CNA0102", "SFO", "ATL", 250, 60),
            new LegTemplate("CNA1001", "ATL", "RST", 140, 60));

    @Test
    void chainsLegsWithBlockAndTurnaround() {
        // Horizon just past the first leg's departure so we get exactly one leg.
        List<FlightInstance> instances = TourScheduleGenerator.generate(
                "run-1", START, START.plus(Duration.ofMinutes(1)), "N100CA", TOUR);

        assertThat(instances).hasSize(1);
        FlightInstance first = instances.get(0);
        assertThat(first.getFlightNumber()).isEqualTo("CNA1000");
        assertThat(first.getOriginCode()).isEqualTo("RST");
        assertThat(first.getDestinationCode()).isEqualTo("MSP");
        assertThat(first.getAircraftId()).isEqualTo("N100CA");
        assertThat(first.getScheduledDeparture()).isEqualTo(START);
        assertThat(first.getScheduledArrival()).isEqualTo(START.plus(Duration.ofMinutes(40)));
        assertThat(first.getStatus()).isEqualTo("scheduled");
    }

    @Test
    void nextDepartureIsArrivalPlusTurnaround() {
        List<FlightInstance> instances = TourScheduleGenerator.generate(
                "run-1", START, START.plus(Duration.ofHours(6)), "N100CA", TOUR);

        FlightInstance leg1 = instances.get(0);
        FlightInstance leg2 = instances.get(1);
        // Leg 1 arrives +40m; +60m turnaround => leg 2 departs at +100m.
        assertThat(leg2.getScheduledDeparture())
                .isEqualTo(leg1.getScheduledArrival().plus(Duration.ofMinutes(60)));
        assertThat(leg2.getFlightNumber()).isEqualTo("CNA0101");
    }

    @Test
    void loopsTheTourAndRecyclesFlightNumbers() {
        // A week is long enough to complete several full 4-leg cycles.
        List<FlightInstance> instances = TourScheduleGenerator.generate(
                "run-1", START, START.plus(Duration.ofDays(7)), "N100CA", TOUR);

        assertThat(instances.size()).isGreaterThan(TOUR.size());
        // The fifth instance starts a new cycle with the first flight number again.
        assertThat(instances.get(4).getFlightNumber()).isEqualTo("CNA1000");
        // Instance ids are unique.
        assertThat(instances.stream().map(FlightInstance::getId).distinct().count())
                .isEqualTo(instances.size());
    }

    @Test
    void respectsHorizonAndNeverSchedulesAtOrAfterIt() {
        Instant horizonEnd = START.plus(Duration.ofDays(7));
        List<FlightInstance> instances = TourScheduleGenerator.generate(
                "run-1", START, horizonEnd, "N100CA", TOUR);

        assertThat(instances).allSatisfy(i ->
                assertThat(i.getScheduledDeparture()).isBefore(horizonEnd));
    }

    @Test
    void emptyTemplateYieldsNoInstances() {
        List<FlightInstance> instances = TourScheduleGenerator.generate(
                "run-1", START, START.plus(Duration.ofDays(7)), "N100CA", List.of());
        assertThat(instances).isEmpty();
    }
}
