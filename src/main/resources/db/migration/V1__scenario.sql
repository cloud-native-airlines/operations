-- Scenario configuration: reusable airline reference data, independent of any
-- simulation run.

CREATE TABLE airport (
    code      TEXT PRIMARY KEY,
    name      TEXT NOT NULL,
    latitude  DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    timezone  TEXT NOT NULL
);

CREATE TABLE aircraft (
    id                TEXT PRIMARY KEY,
    type              TEXT NOT NULL,
    base_airport_code TEXT NOT NULL REFERENCES airport (code)
);

CREATE TABLE route (
    id               TEXT PRIMARY KEY,
    origin_code      TEXT NOT NULL REFERENCES airport (code),
    destination_code TEXT NOT NULL REFERENCES airport (code),
    block_minutes    INTEGER NOT NULL CHECK (block_minutes > 0),
    UNIQUE (origin_code, destination_code)
);

-- A tour is a repeating template of legs the airline flies in a loop.
CREATE TABLE tour (
    id   TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE tour_leg (
    tour_id            TEXT NOT NULL REFERENCES tour (id),
    seq                INTEGER NOT NULL,
    flight_number      TEXT NOT NULL,
    route_id           TEXT NOT NULL REFERENCES route (id),
    turnaround_minutes INTEGER NOT NULL CHECK (turnaround_minutes >= 0),
    PRIMARY KEY (tour_id, seq)
);
