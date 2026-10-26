# Cloud Native Airlines Operations — Design & Plan

Companion to [OPERATIONS_PROJECT_GOALS.md](OPERATIONS_PROJECT_GOALS.md). Records
the design decisions for the first version and the plan to build it.

## Decisions at a glance

| Topic | Decision | Rationale |
| --- | --- | --- |
| Stack | **Java 21 + Spring Boot + PostgreSQL + Flyway** | Per the goals; strong transactional/validation ergonomics for scheduling constraints; adds a JVM service to the polyglot demo. |
| Database | **Operations owns its own PostgreSQL** (separate from ADS-B) | Operations is the business source of truth; ADS-B owns only position reports. |
| Data split | **Scenario config vs. run state** | Reusable scenario (airports, aircraft, routes, tour template) persists; per-run flight instances/assignments/events reset per run. |
| Tour execution | **One flight pod per leg, started by a launcher** | Keeps the Flight = one leg + exit-on-landing design; the launcher runs each leg in sequence, honoring turnaround. |
| Telemetry | **None yet** | Consistent with the rest of the stack — a future auto-instrumentation target, not hand-wired now. |

## Demo intent: durable airline, ephemeral flights

A **Flight runtime is not the aircraft** — it represents a *flight in process* and
is deliberately **ephemeral**: it starts, flies one leg, and exits. The durable
things (the aircraft `N100CA`, airports, routes, the schedule and what actually
happened) live here in **Operations**. Flight pods come and go against that
stable record.

That split is the point of the demo. The launcher producing a **pod per leg** —
pods starting, running, and stopping as the tour progresses — models a system
driven by **lots of short-lived jobs** (think AI-training runs or bursty
workloads), which is a rich observability story: pod churn, scheduling, start/stop
latency, and failures against a durable control plane. The one-aircraft tour is
the smallest version (four sequential pods); scaling to more aircraft later turns
it into many concurrent ephemeral pods.

## The MVP: a one-aircraft tour

One aircraft (`N100CA`) flies a four-leg loop, waiting one hour at each airport:

| Flight | Leg | Route |
| --- | --- | --- |
| CNA1000 | RST → MSP | regional hop |
| CNA0101 | MSP → SFO | transcon |
| CNA0102 | SFO → ATL | transcon |
| CNA1001 | ATL → RST | medium haul |

Airports (coords match the ADS-B seed): RST (43.9083, −92.5000), MSP (44.8848,
−93.2223), SFO (37.6213, −122.3790), ATL (33.6407, −84.4277).

A proper published schedule and demand-driven scheduling come later; for now the
tour is a fixed template that Operations materializes into a run.

## Data storage

Operations' own PostgreSQL, with versioned Flyway migrations. Two tiers:

**Scenario config** — reusable, seeded by migration, not run-specific:

- `airport` — code (PK), name, latitude, longitude, timezone.
- `aircraft` — id/registration (PK), equipment type.
- `route` — origin + destination airport, and a scheduled **block minutes**
  (nominal flight time) used to compute arrival times.
- `tour` / `tour_leg` — the ordered template: leg sequence, flight number, route,
  turnaround minutes. (The MVP seeds exactly the four legs above.)

**Run state** — keyed by `run_id` (the Simulator's run), reset per run:

- `flight_instance` — run_id, flight number, route, scheduled_departure,
  scheduled_arrival, assigned aircraft, status (`scheduled`/`departed`/`arrived`).
- `operational_event` — run_id, event_id, flight_instance, aircraft, type
  (`departure`/`arrival`), `occurred_at` (simulated), `received_at` (real).
- Status is derived from accepted events plus explicit changes; **actual** times
  come from events' simulated `occurred_at`, never HTTP receipt time.

All times are UTC; airports keep timezone for FIDS local display. A reset deletes
a run's `flight_instance`/`operational_event` rows; scenario config is untouched.

### Run initialization

`POST /api/v1/runs/{runId}` with a run **start time** materializes the tour into
`flight_instance` rows for that run, computing the timeline from the start time,
each route's block minutes, and the one-hour turnarounds, and assigning `N100CA`.
Idempotent (re-initializing the same run is a no-op or a documented reset).

Example timeline (block minutes illustrative, tuned later):

```text
CNA1000 RST→MSP   dep T0        arr T0+0:40
  (turnaround 1:00)
CNA0101 MSP→SFO   dep +1:00     arr +3:20
  (turnaround 1:00)
CNA0102 SFO→ATL   dep +1:00     arr +4:10
  (turnaround 1:00)
CNA1001 ATL→RST   dep +1:00     arr +2:20
```

## API surface (Phase 1)

All run-specific reads/writes select a run explicitly.

| Method & path | Purpose |
| --- | --- |
| `GET /api/v1/airports` · `/airports/{code}` | Scenario airports |
| `GET /api/v1/aircraft` · `/aircraft/{id}` | Fleet |
| `GET /api/v1/routes` | Routes |
| `POST /api/v1/runs/{runId}` | Initialize a run from the scenario + start time |
| `GET /api/v1/runs/{runId}/flights` | The run's schedule (instances) |
| `GET /api/v1/runs/{runId}/aircraft/{id}/assignments` | **Assignment view** for an aircraft: ordered legs with airport coordinates, scheduled times, flight numbers, turnaround — everything needed to execute, no client-side joins |
| `POST /api/v1/runs/{runId}/events` | Record a departure/arrival event |
| `GET /api/v1/runs/{runId}/flights/{id}` | One instance: scheduled vs. actual + status |
| `GET /api/v1/runs/{runId}/airports/{code}/board` | FIDS: departures/arrivals for an airport + window |

### Validation (transactional)

- `scheduled_departure < scheduled_arrival`; referenced airports/aircraft/routes exist.
- An aircraft's assignments must not **overlap** in time.
- **Consecutive** legs must connect geographically (leg N+1 origin = leg N
  destination) and respect turnaround (`dep[N+1] ≥ arr[N] + turnaround`).
- Checks + writes in one transaction so concurrent changes can't create an
  incompatible assignment.

### Event lifecycle

`scheduled → departed → arrived`. Identical retries (same event_id + content) are
accepted without duplication; a reused event_id with different content is a
conflict (`409`). Out-of-order/late events preserve the record and must not move
an `arrived` flight back to `departed`. Events are validated against the
assignment (right aircraft/flight). Position reports are a **separate** stream —
Operations never infers departure/arrival from ADS-B.

## Executing the tour: the launcher

Per the chosen model, each leg is flown by its **own Flight process that exits on
landing**, and a **launcher** runs the legs in sequence:

```text
launcher (for run R, aircraft N100CA):
  init run R in Operations (materialize schedule)
  for each leg in GET /runs/R/aircraft/N100CA/assignments:
      start a Flight for this leg with its plan (origin/dest coords,
        scheduled times, flight number, run_id, flight_instance_id,
        aircraft_id, Operations events URL)
      wait until the leg's arrival event is recorded (poll Operations)
      wait the turnaround, then start the next leg
```

- **Flight changes (Phase 2):** the Flight already flies a plan and reports
  positions to ADS-B. It additionally **reports departure/arrival events to
  Operations** (stable event_id = `{run}-{flightInstance}-{type}`, carrying the
  simulated occurrence time). It receives the leg's plan + Operations/event IDs
  from the launcher at start (extends the existing `FLIGHT_PLAN` config).
- **Compose realization:** the launcher runs Flight containers per leg via the
  host Docker (one container per leg, matching "pod per leg"). It tracks the
  simulated clock from the Simulator (NATS ticks or `GET /clock`) to time each
  leg's launch and the turnaround waits.
- **Kubernetes (later):** the launcher becomes a controller that creates a Job
  per leg; the main repo owns that packaging.

## Build plan

### Phase 1 — Operations backend (matches the goals' first milestone)

1. **Skeleton** — Spring Boot app, PostgreSQL, Flyway, config, `/actuator/health`,
   Dockerfile, build (Gradle), CI.
2. **Scenario schema + seed** — airports (RST/MSP/SFO/ATL), aircraft (N100CA),
   routes with block minutes, and the four-leg tour template.
3. **Run init + schedule** — `POST /runs/{runId}` materializes instances with the
   computed timeline + assignment; the assignment-view query.
4. **Assignment validation** — timing, existence, overlap, geographic continuity
   and turnaround, all transactional; tests incl. concurrent assignment attempts.
5. **Events + status** — `POST /runs/{runId}/events`; dedup/conflict/out-of-order
   with tested behavior; status derivation; actual vs scheduled.
6. **Queries** — flights, flight detail, FIDS airport board; run-scoped.
7. **Reset/run isolation** — per-run state deletion; reproducible init; tests.

**Phase 1 done when:** Operations runs locally with Postgres + migrations; a
sample run initializes the tour (4 instances, assigned, with turnaround); the
assignment query returns enough to fly each leg; invalid timing / overlaps /
geographically incompatible legs are rejected; events record simulated actual
times and advance status with documented duplicate/conflict/out-of-order
behavior; FIDS board queries return the needed fields; state is run-isolated;
and API examples demonstrate the flow with no other services running.

### Phase 2 — fly the tour (launcher + flight integration)

1. **Flight → Operations events** — the Flight posts departure/arrival events.
2. **Launcher** — sequences the legs (pod per leg), timing launches/turnarounds
   off simulated time; initializes the run in Operations.
3. **Compose** — add `operations` + its Postgres and the `launcher`; wire the
   end-to-end tour so pressing play flies RST→MSP→SFO→ATL→RST with 1-hour waits.

## Open items / boundary reconciliation

- **Airports now live in two places.** We seeded airports in ADS-B earlier
  (pragmatically, before Operations existed). Operations is the rightful owner
  (fleet/airports/routes). Decide whether to (a) make Operations authoritative and
  have the Flight Tracker fetch airports from Operations (ADS-B keeps none, or a
  position-only copy), or (b) leave ADS-B's copy for now and reconcile later. The
  MVP can seed both; long-term, Operations owns them.
- **Run start time** must align with the Simulator's run and scenario start so
  schedule times, positions, and events correlate. The launcher/demo tooling
  initializes the Operations run with the same `run_id` and start time.
- **Block minutes** per route are configured now; later they can derive from
  great-circle distance at a nominal cruise speed (consistent with the Flight
  model) or come from a real schedule.
- **Departure authorization** (Operations telling an aircraft it's cleared / not
  ready) is deferred; define that contract before aircraft behavior depends on
  actual availability.
