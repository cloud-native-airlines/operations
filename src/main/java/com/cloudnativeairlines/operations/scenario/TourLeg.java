package com.cloudnativeairlines.operations.scenario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/** One ordered leg of a repeating tour template, keyed by {@code (tour_id, seq)}. */
@Entity
@Table(name = "tour_leg")
@IdClass(TourLeg.Key.class)
public class TourLeg {

    @Id
    @Column(name = "tour_id")
    private String tourId;

    @Id
    private int seq;

    @Column(name = "flight_number", nullable = false)
    private String flightNumber;

    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Column(name = "turnaround_minutes", nullable = false)
    private int turnaroundMinutes;

    protected TourLeg() {
    }

    public String getTourId() {
        return tourId;
    }

    public int getSeq() {
        return seq;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getRouteId() {
        return routeId;
    }

    public int getTurnaroundMinutes() {
        return turnaroundMinutes;
    }

    /** Composite primary key. */
    public static class Key implements Serializable {
        private String tourId;
        private int seq;

        public Key() {
        }

        public Key(String tourId, int seq) {
            this.tourId = tourId;
            this.seq = seq;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return seq == key.seq && Objects.equals(tourId, key.tourId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tourId, seq);
        }
    }
}
