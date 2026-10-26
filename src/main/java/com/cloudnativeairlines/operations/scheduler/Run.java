package com.cloudnativeairlines.operations.scheduler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Metadata for a materialized run: its start time and schedule horizon. */
@Entity
@Table(name = "run")
public class Run {

    @Id
    @Column(name = "run_id")
    private String runId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "horizon_days", nullable = false)
    private int horizonDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Run() {
    }

    public Run(String runId, Instant startAt, int horizonDays, Instant createdAt) {
        this.runId = runId;
        this.startAt = startAt;
        this.horizonDays = horizonDays;
        this.createdAt = createdAt;
    }

    public String getRunId() {
        return runId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public int getHorizonDays() {
        return horizonDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
