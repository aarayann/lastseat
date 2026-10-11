# lastseat- Booking Eligibility & Policy Rule Engine
 
MoveInSync Backend Case Study 1. A Spring Boot service that checks an office-cab booking against a set of
company rules and returns **ALLOW**, **DENY** or **NEEDS_APPROVAL**, with the exact reason.
 
- Five rules, each its own class behind one `Rule` interface
- A hard deny always wins, whatever order the rules run in
- Explain mode shows which rules ran, in what order, and why each passed, failed or was skipped
- Rules can be switched on or off, reordered or changed at runtime, with no restart
- Only one person can get the last seat, even when several book at the same moment
- Cancelling a booking frees its seat straight away
<!-- Demo video: add the link here -->
 
## How to run
 
Needs Java 21. The database (H2) runs in memory, so there is nothing else to install.
 
```bash
./mvnw spring-boot:run     # Windows: .\mvnw.cmd spring-boot:run
./mvnw test                # runs all 39 tests
```
 
- Swagger UI: http://localhost:8080/swagger-ui.html (click **Authorize** to log in)
- Health: http://localhost:8080/actuator/health
- Postman collection: `postman/booking-engine.postman_collection.json`
Demo data is loaded on every start and reset on restart.
 
| User | Password | Role | Notes |
|---|---|---|---|
| `aarav` | `password` | Employee | id 1, Engineering, male, 09:00–18:00 |
| `priya` | `password` | Employee | id 2, Engineering, female, 09:00–18:00 |
| `meera` | `password` | Employee | id 3, Engineering, female, 14:00–23:00 |
| `rohan` | `password` | Employee | id 4, Operations, male, 22:00–06:00 |
| `kavya` | `password` | Employee | id 5, Operations, female, 09:00–18:00 |
| `ishaan` | `password` | Employee | id 6, Operations, male, 09:00–18:00 |
| `approver` | `approver123` | Approver | Approves or rejects pending bookings |
| `admin` | `admin123` | Admin | Changes rules, sees metrics |
 
Engineering has 20 seats per day per direction, Operations has 2.
 
## API
 
| Method | Path | Who | What it does |
|---|---|---|---|
| POST | `/api/bookings/evaluate` | Employee, Admin | Dry run with the full trace. Saves nothing |
| POST | `/api/bookings` | Employee, Admin | Real booking: 201 ALLOW, 202 NEEDS_APPROVAL, 422 DENY |
| GET | `/api/bookings/{id}` | Owner, Admin | One booking |
| POST | `/api/bookings/{id}/cancel` | Owner, Admin | Cancel and free the seat |
| GET | `/api/approvals/pending` | Approver | Bookings waiting for approval |
| POST | `/api/approvals/{id}/approve` or `/reject` | Approver | Decide a pending booking |
| GET / PUT | `/api/admin/rules`, `/api/admin/rules/{id}` | Admin | See or change rules |
| GET | `/actuator/metrics/{name}` | Admin | Counters and timers |
 
Employees can only see, book and cancel their own bookings.
 
**Good booking → ALLOW**
 
```bash
curl -u aarav:password -X POST http://localhost:8080/api/bookings -H "Content-Type: application/json" \
  -d '{"employeeId":1,"date":"2026-10-15","direction":"LOGIN","requestedTime":"09:00","pickup":"Home","drop":"Office"}'
```
 
```json
{ "decision": "ALLOW", "bookingId": 1, "status": "CONFIRMED", "reasons": [], "configVersion": 1,
  "evaluatedAt": "2026-10-11T14:30:00" }
```
 
Use a date at least a day ahead. Sending it again gives 422 `DUPLICATE`.
 
**Woman, 11:30 pm, no escort → DENY / NIGHT_SAFETY (explain mode)**
 
```bash
curl -u meera:password -X POST http://localhost:8080/api/bookings/evaluate -H "Content-Type: application/json" \
  -d '{"employeeId":3,"date":"2026-10-15","direction":"LOGOUT","requestedTime":"23:30","pickup":"Office","drop":"Home","escort":false}'
```
 
```json
{
  "decision": "DENY",
  "reasons": [{ "code": "NIGHT_SAFETY",
                "message": "female employee, 23:30 is inside night hours 20:00 to 06:00, no escort on the booking" }],
  "configVersion": 1,
  "trace": [
    { "order": 1, "rule": "SHIFT_WINDOW",      "outcome": "PASS" },
    { "order": 2, "rule": "CUTOFF",            "outcome": "PASS" },
    { "order": 3, "rule": "NIGHT_SAFETY",      "outcome": "FAIL" },
    { "order": 4, "rule": "DUPLICATE",         "outcome": "SKIPPED" },
    { "order": 5, "rule": "COST_CENTRE_QUOTA", "outcome": "SKIPPED" }
  ]
}
```
 
Shortened: the real response also has `evaluatedAt`, and each trace line also has `onFail`, `reasonCode`
and a `detail` sentence saying why. With `"escort": true` the booking is allowed.
 
**Change a rule without restarting**
 
```bash
curl -u admin:admin123 -X PUT http://localhost:8080/api/admin/rules/2 -H "Content-Type: application/json" \
  -d '{"params":{"minutesBefore":30}}'
```
 
The next booking uses the new cut-off, and `configVersion` goes from 1 to 2. Bad values like `-5` are
refused with 400 and never saved.
 
## The rules
 
| Id | Rule | Fails with | Settings | Fails when |
|---|---|---|---|---|
| 1 | Shift window | `OUTSIDE_SHIFT_WINDOW` | `graceMinutes: 30` | Time is outside the shift ± grace. Handles shifts past midnight |
| 2 | Cut-off | `PAST_CUTOFF` | `minutesBefore: 60` | Less than N minutes before departure |
| 3 | Night safety | `NIGHT_SAFETY` | `nightStart: 20:00`, `nightEnd: 06:00` | A woman travels at night with no escort |
| 4 | Duplicate | `DUPLICATE` | — | The employee already has an active booking that day in that direction |
| 5 | Seat quota | `QUOTA_EXCEEDED` | `seatsPerDay: 20` | All seats for that cost-centre, day and direction are taken |
| 6 | Seat quota | `QUOTA_EXCEEDED` | `seatsPerDay: 2` | Same rule, overridden for Operations only |
 
All rules start as hard rules (`DENY`). An admin can make any rule soft (`NEEDS_APPROVAL`), so it sends
the booking to an approver instead. If a rule itself crashes, the booking is refused with `RULE_ERROR`.
 
## How it works
 
**Precedence.** The decision is the most severe failure: DENY beats NEEDS_APPROVAL beats ALLOW. Because it
is the most severe and not the first failure, the order the rules run in can never change the decision.
A test shuffles the rules 100 times to check this. Rules run hard ones first, then by priority, then by id,
so the same request always runs the same way.
 
**Short-circuit.** Once a hard rule fails, the decision can't change, so the two rules that query the
database (duplicate and quota) are skipped. The other rules still run, so a booking that breaks two rules
gets both reasons.
 
**The last seat.** A booking locks its cost-centre's row in the database, counts the active bookings, saves,
and then releases the lock. A second booking for the same cost-centre waits for the first one to finish,
so it sees the updated count. A test fires 2 and 20 bookings at a single seat at once: exactly one wins.
Without the lock, 10 out of 20 got the seat.
 
**Cancelling.** Seats are counted from active bookings, not kept in a separate counter. A cancelled or
rejected booking stops counting, so its seat is free immediately. Cancelling twice changes nothing.
 
**Caching.** The active rules are cached in memory (Caffeine, up to 1,000 entries, 10-minute expiry) and the
cache is cleared whenever an admin changes a rule. Seat counts and bookings are never cached. With a single
app instance nothing goes stale. With several instances, the others could use old rules for up to 10 minutes.
 
**Same input, same answer.** Rules never read the clock themselves; the current time is passed in. Every
decision returns the `configVersion` it was made with.
 
**Design.** Controller → service → rule engine → repository. Each rule implements the `Rule` interface and
extends `AbstractRule`, which catches crashes. A booking's status can only change through its own
`cancel()`, `approve()` and `reject()` methods. Adding a new rule means one new class and one config row;
the engine doesn't change.
 
## Errors and monitoring
 
Every error has the same JSON shape (`status`, `errorCode`, `message`, `path`, `requestId`) and never
shows stack traces. Main codes: `VALIDATION_FAILED` and `MALFORMED_REQUEST` (400), `INVALID_RULE_PARAMS`
(400), `FORBIDDEN` (403), `NOT_FOUND` (404), `INVALID_STATE` (409), `INTERNAL_ERROR` (500). Login and role
errors (401, 403) use Spring Security's default response.
 
A booking is saved in one transaction, so a crash halfway leaves nothing half-written. Every request gets an
`X-Request-Id` that appears in its log lines and error body. Every real booking attempt is saved to a
`decision_log` table with its full trace.
 
Metrics at `/actuator/metrics`: `booking.decisions`, `rule.violations`, `booking.cancellations`,
`approval.outcomes` and `rule.engine.evaluation` (time spent in the engine).
 
## Complexity
 
With R rules (5 here): sorting costs O(R log R) once per config change, then it's cached. Each booking
runs R rules in O(R) time and O(R) memory for the trace. The duplicate and quota checks use database
indexes, so they stay fast as bookings grow.
 
## Assumptions
 
- Routing isn't modelled, so every night-time booking for a woman is treated as possibly first pickup or last drop.
- The shift window is shift start minus grace to shift end plus grace, for both login and logout.
- Seats are counted per cost-centre, per day, per direction (login and logout are separate trips).
- A booking waiting for approval holds its seat until it is approved or rejected.
- All times are in one timezone (Asia/Kolkata).
- Data is in memory and resets on restart.
## Trade-offs
 
| Choice | Instead of | Why |
|---|---|---|
| Small custom rule engine | Drools | Five rules and a strict "explain why" need. Easy to trace |
| Lock the cost-centre, then count | A seat counter | Nothing to update on cancel. Bookings for the same cost-centre run one at a time |
| Database lock | Java `synchronized` | `synchronized` only works inside one server |
| In-memory cache | Redis | Nothing extra to run. Can be stale across instances |
| HTTP Basic login | JWT | Quick to build and test. JWT is the production choice |
| H2 in memory | PostgreSQL | Runs anywhere with no setup. Switching needs the driver and new connection settings |
 
## Tests
 
39 tests: each rule including its edge cases, the engine's precedence rules, the last-seat race, and full
HTTP flows (good and bad examples, duplicate, cancel, live rule change, approval, validation, 401/403/404).
 
## Next steps
 
- Keep a history of rule changes, so the rules behind any `configVersion` can be shown
- JWT login, PostgreSQL with migrations, and cache clearing across several instances
- Return login and role errors in the same JSON shape as other errors
 
