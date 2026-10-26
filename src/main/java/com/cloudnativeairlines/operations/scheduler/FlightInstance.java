package com.cloudnativeairlines.operations.scheduler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One scheduled leg within a run, with scheduled vs. actual times and status. */
@Entity
@Table(name = "flight_instance")
public class FlightInstance {

    @Id
    private String id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "flight_number", nullable = false)
    private String flightNumber;

    @Column(name = "origin_code", nullable = false)
    private String originCode;

    @Column(name = "destination_code", nullable = false)
    private String destinationCode;

    @Column(name = "aircraft_id", nullable = false)
    private String aircraftId;

    @Column(name = "scheduled_departure", nullable = false)
    private Instant scheduledDeparture;

    @Column(name = "scheduled_arrival", nullable = false)
    private Instant scheduledArrival;

    @Column(name = "actual_departure")
    private Instant actualDeparture;

    @Column(name = "actual_arrival")
    private Instant actualArrival;

    @Column(nullable = false)
    private String status = "scheduled";

    protected FlightInstance() {
    }

    public FlightInstance(String id, String runId, String flightNumber, String originCode,
            String destinationCode, String aircraftId, Instant scheduledDeparture,
            Instant scheduledArrival) {
        this.id = id;
        this.runId = runId;
        this.flightNumber = flightNumber;
        this.originCode = originCode;
        this.destinationCode = destinationCode;
        this.aircraftId = aircraftId;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.status = "scheduled";
    }

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOriginCode() {
        return originCode;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public Instant getScheduledDeparture() {
        return scheduledDeparture;
    }

    public Instant getScheduledArrival() {
        return scheduledArrival;
    }

    public Instant getActualDeparture() {
        return actualDeparture;
    }

    public void setActualDeparture(Instant actualDeparture) {
        this.actualDeparture = actualDeparture;
    }

    public Instant getActualArrival() {
        return actualArrival;
    }

    public void setActualArrival(Instant actualArrival) {
        this.actualArrival = actualArrival;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
