package com.cloudnativeairlines.operations.fleet;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloudnativeairlines.operations.scheduler.FlightInstance;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocationResolverTest {

    private static final String BASE = "RST";

    private static FlightInstance leg(String id, String flight, String origin, String dest,
            String dep, String arr) {
        return new FlightInstance(id, "run-1", flight, origin, dest, "N100CA",
                Instant.parse(dep), Instant.parse(arr));
    }

    // RST->MSP dep 12:00 arr 12:40, then MSP->SFO dep 13:40 arr 17:00.
    private List<FlightInstance> twoLegs() {
        return List.of(
                leg("i1", "CNA1000", "RST", "MSP", "2026-01-01T12:00:00Z", "2026-01-01T12:40:00Z"),
                leg("i2", "CNA0101", "MSP", "SFO", "2026-01-01T13:40:00Z", "2026-01-01T17:00:00Z"));
    }

    @Test
    void unknownWhenNoSchedule() {
        AircraftLocation loc = LocationResolver.resolve(BASE, List.of(),
                Instant.parse("2026-01-01T12:00:00Z"));
        assertThat(loc.state()).isEqualTo("unknown");
        assertThat(loc.source()).isNull();
    }

    @Test
    void atBaseBeforeFirstDeparture() {
        AircraftLocation loc = LocationResolver.resolve(BASE, twoLegs(),
                Instant.parse("2026-01-01T11:00:00Z"));
        assertThat(loc.state()).isEqualTo("at_airport");
        assertThat(loc.airport()).isEqualTo("RST");
        assertThat(loc.source()).isEqualTo("schedule");
    }

    @Test
    void flyingDuringScheduledLeg() {
        AircraftLocation loc = LocationResolver.resolve(BASE, twoLegs(),
                Instant.parse("2026-01-01T12:20:00Z"));
        assertThat(loc.state()).isEqualTo("flying");
        assertThat(loc.flight().flightNumber()).isEqualTo("CNA1000");
        assertThat(loc.source()).isEqualTo("schedule");
    }

    @Test
    void atPreviousDestinationBetweenLegs() {
        AircraftLocation loc = LocationResolver.resolve(BASE, twoLegs(),
                Instant.parse("2026-01-01T13:00:00Z"));
        assertThat(loc.state()).isEqualTo("at_airport");
        assertThat(loc.airport()).isEqualTo("MSP");
        assertThat(loc.source()).isEqualTo("schedule");
    }

    @Test
    void atFinalDestinationAfterLastLeg() {
        AircraftLocation loc = LocationResolver.resolve(BASE, twoLegs(),
                Instant.parse("2026-01-01T18:00:00Z"));
        assertThat(loc.state()).isEqualTo("at_airport");
        assertThat(loc.airport()).isEqualTo("SFO");
    }

    @Test
    void eventDepartedNotArrivedIsFlying() {
        List<FlightInstance> legs = twoLegs();
        legs.get(0).setActualDeparture(Instant.parse("2026-01-01T12:05:00Z"));
        // Even if "now" looks like a schedule gap, the open event wins.
        AircraftLocation loc = LocationResolver.resolve(BASE, legs,
                Instant.parse("2026-01-01T13:00:00Z"));
        assertThat(loc.state()).isEqualTo("flying");
        assertThat(loc.flight().flightNumber()).isEqualTo("CNA1000");
        assertThat(loc.source()).isEqualTo("event");
    }

    @Test
    void eventArrivedIsAtDestination() {
        List<FlightInstance> legs = twoLegs();
        legs.get(0).setActualDeparture(Instant.parse("2026-01-01T12:05:00Z"));
        legs.get(0).setActualArrival(Instant.parse("2026-01-01T12:38:00Z"));
        AircraftLocation loc = LocationResolver.resolve(BASE, legs,
                Instant.parse("2026-01-01T13:00:00Z"));
        assertThat(loc.state()).isEqualTo("at_airport");
        assertThat(loc.airport()).isEqualTo("MSP");
        assertThat(loc.source()).isEqualTo("event");
    }
}
