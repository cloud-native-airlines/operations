-- Per-run metadata recorded when a run's schedule is materialized.

CREATE TABLE run (
    run_id       TEXT PRIMARY KEY,
    start_at     TIMESTAMPTZ NOT NULL,
    horizon_days INTEGER NOT NULL CHECK (horizon_days > 0),
    created_at   TIMESTAMPTZ NOT NULL
);
