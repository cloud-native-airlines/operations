# Cloud Native Airlines Operations Project Goals

## Purpose

Operations is the airline's operational source of truth in Cloud Native Airlines. It owns the fleet, airports, routes, schedules, aircraft assignments, and the recorded operational status of each flight.

Cloud Native Airlines is an observability demo built around a simulated airline. Aircraft run independently, Simulator advances time, and ADS-B stores position reports. Operations gives that activity a business context: which aircraft should operate a flight, where it should depart, when it is scheduled to arrive, and what actually happened.

The goal is a small operational backend that lets aircraft obtain assignments and lets airport displays and the flight tracker explain the fleet's activity. It should create meaningful observability stories through schedule conflicts, late departures, aircraft availability, and operational event delivery.

## Initial scope

Start with a small configured fleet and a reproducible schedule, including one aircraft flying Minneapolis (MSP) to Chicago O'Hare (ORD) and a return flight after turnaround. Provide APIs to inspect airports, aircraft, flights, and assignments, and to record departures and arrivals.

The service should run locally and accept sample operational events without requiring Kubernetes, Simulator, or a live aircraft. The proposed implementation is Java with Spring Boot and PostgreSQL, using versioned database migrations. Operations owns its database independently of ADS-B.

Ticketing, passengers, pricing, upgrades, demand forecasting, and automated capacity planning are outside the first milestone. A separate operations UI is also deferred; documented API commands are sufficient to demonstrate the backend.

## Domain model

| Record | Responsibility |
| --- | --- |
| Airport | Stable identifier, airport codes, name, coordinates, and timezone |
| Aircraft | Stable identity, basic equipment type, and operational availability |
| Route | Origin and destination airport pair |
| Scheduled flight | A specific dated flight with scheduled departure and arrival times |
| Aircraft assignment | Aircraft selected to operate a scheduled flight |
| Operational event | Reported departure or arrival, with identity and simulated occurrence time |
| Flight status | Current operational state derived from accepted events and explicit operational changes |

Distinguish a flight number from a flight instance. The same flight number may recur on different dates; assignments and events must reference a unique instance. Store simulation timestamps in UTC and retain airport timezone information for local display by FIDS.

Separate reusable scenario configuration from state belonging to a particular simulation run. Actual departure and arrival times, event history, and run-specific assignments must not leak between resets.

## Scheduling and assignments

Operations stores the plan and validates assignments. Aircraft executes that plan. A schedule should include the unique flight instance, origin, destination, scheduled times, assigned aircraft, and required turnaround interval.

Validate that departure precedes arrival, referenced records exist, and an aircraft's assignments do not overlap. Consecutive flights must connect geographically and allow the configured turnaround time. Checks and writes should be transactional so concurrent changes cannot assign one aircraft to incompatible flights.

Scheduled availability and actual availability are distinct. A valid published schedule can become infeasible after a late arrival. Record actual events without changing the original scheduled times, allowing clients to compare the plan with what happened.

In a later stage, Operations can expose that an aircraft is not ready for its next departure and supply revised assignments or estimates. Automatic delay propagation and rescheduling are deferred until the basic schedule and event model work. Define the departure authorization contract before implementing aircraft behavior that depends on actual availability.

## Operational events and status

Aircraft eventually sends departure and arrival events directly to Operations. Each event carries a stable event ID, run ID, flight instance ID, aircraft ID, event type, and simulated occurrence time. Operations adds a real receipt timestamp.

Treat identical retries as successful without recording duplicate events. Reusing an event ID with different content is a conflict. Validate the aircraft against the assignment and prevent incompatible events from silently changing the flight's history.

For the initial lifecycle, a flight moves from scheduled to departed to arrived. A delayed delivery or repeated event must not move an arrived flight back to departed. Define how out-of-order events are handled, preserving the event record and exposing incomplete or inconsistent state where appropriate. Actual flight times come from simulated event occurrence times, not HTTP receipt times.

Position reports are a separate data stream. An ADS-B outage does not establish that a flight was canceled or stopped operating. Operations must not infer departure or arrival solely from a map marker or missing position report.

## API consumers

| Consumer | Information or action |
| --- | --- |
| Aircraft | Fetch its assignments and flight plans; submit operational events |
| FIDS | Query departures and arrivals for an airport and time window |
| Flight Tracker | Enrich aircraft reports with flight number, route, and schedule information |
| Demo tooling | Load a scenario, initialize a run, inspect state, and demonstrate assignment validation |

Provide bounded, filtered queries with explicit run selection. Airport flight queries should include scheduled times, actual times when known, assigned aircraft, and status. Estimated times should remain absent until an estimation policy exists.

Document API contracts and sample payloads. Keep exact endpoint paths consistent with implemented consumers. An aircraft should not need to reconstruct its flight plan by joining multiple unrelated API responses; provide an assignment view containing the required airport coordinates and timing information.

## Component boundaries

| Component | Responsibility |
| --- | --- |
| Operations | Airline records, schedules, assignments, and operational flight history |
| Simulator | Simulation clock, speed, tick interval, and run progression |
| Aircraft | Flight calculations, assignment execution, and event reporting |
| ADS-B | Position ingestion, latest positions, and tracks |
| Flight Tracker | Map and flight details |
| FIDS | Airport departures and arrivals display |

Operations does not advance simulation time, calculate coordinates, provision aircraft pods, or store transponder history. The main repository owns installation and integration configuration. When operational decisions later depend on current time, they must use Simulator's authoritative simulated time rather than the host's real clock.

## Observability goals

The service should help operators answer whether aircraft have valid assignments, whether events are arriving, and why recorded flight activity differs from the schedule.

Measure API latency, database operation duration, assignment conflicts, event validation failures, duplicates, and departure or arrival delay. Request latency uses real time; flight delay compares actual and scheduled timestamps within simulated time.

Structured logs record run, aircraft, flight, and event identifiers. OpenTelemetry traces connect assignment queries and event delivery to database operations. Use bounded metric labels and keep individual identifiers in logs and traces. Telemetry export should be configurable so local use does not require a collector.

Useful demo scenarios include conflicting assignments, a late arrival affecting the next scheduled flight, duplicate arrival delivery, delayed event delivery, and a slow database. These create operational questions that viewers can investigate through telemetry.

## First milestone success criteria

- Operations runs locally with PostgreSQL and documented migrations.
- A sample scenario creates two airports, one aircraft, and an outbound and return flight with turnaround time.
- An aircraft assignment query returns enough information to execute the configured flight plan.
- Invalid timing, overlapping assignments, and geographically incompatible consecutive assignments are rejected.
- Departure and arrival events record simulated actual times and advance flight status.
- Duplicate, conflicting, and out-of-order events have documented, tested behavior.
- Airport departure and arrival queries provide the fields needed by FIDS.
- State is isolated by simulation run and initialization is reproducible.
- A container build and API examples demonstrate the flow without external application services.
- Meaningful tests exercise database constraints, concurrent assignment changes, event handling, and run isolation.

## Future development

Expand to larger fleets, multiple equipment types, aircraft substitutions, maintenance restrictions, cancellations, and reassignments. Add explicit departure readiness, revised estimates, and delay propagation as operational workflows mature.

Simulator may later supply weather, demand, and economic conditions. Operations remains responsible for the airline decisions that respond to them. Each extension should produce a runnable scenario with a visible effect on flights or airport displays and an understandable telemetry trail.
