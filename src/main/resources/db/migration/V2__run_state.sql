-- Per-run operational state, keyed by the Simulator's run_id and reset per run.

CREATE TABLE flight_instance (
    id                  TEXT PRIMARY KEY,
    run_id              TEXT NOT NULL,
    flight_number       TEXT NOT NULL,
    origin_code         TEXT NOT NULL REFERENCES airport (code),
    destination_code    TEXT NOT NULL REFERENCES airport (code),
    aircraft_id         TEXT NOT NULL REFERENCES aircraft (id),
    scheduled_departure TIMESTAMPTZ NOT NULL,
    scheduled_arrival   TIMESTAMPTZ NOT NULL,
    actual_departure    TIMESTAMPTZ,
    actual_arrival      TIMESTAMPTZ,
    status              TEXT NOT NULL DEFAULT 'scheduled'
        CHECK (status IN ('scheduled', 'departed', 'arrived')),
    CHECK (scheduled_departure < scheduled_arrival)
);

CREATE INDEX idx_flight_instance_aircraft
    ON flight_instance (run_id, aircraft_id, scheduled_departure);
CREATE INDEX idx_flight_instance_origin
    ON flight_instance (run_id, origin_code, scheduled_departure);
CREATE INDEX idx_flight_instance_destination
    ON flight_instance (run_id, destination_code, scheduled_arrival);

CREATE TABLE operational_event (
    run_id              TEXT NOT NULL,
    event_id            TEXT NOT NULL,
    flight_instance_id  TEXT NOT NULL REFERENCES flight_instance (id),
    aircraft_id         TEXT NOT NULL REFERENCES aircraft (id),
    type                TEXT NOT NULL CHECK (type IN ('departure', 'arrival')),
    occurred_at         TIMESTAMPTZ NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (run_id, event_id)
);

CREATE INDEX idx_operational_event_flight
    ON operational_event (run_id, flight_instance_id);
