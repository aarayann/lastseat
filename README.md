# Booking Eligibility & Policy Rule Engine

MoveInSync Backend Case Study 1. A Spring Boot service that decides whether an office-cab booking is
**ALLOW**, **DENY** or **NEEDS_APPROVAL** by running a configurable stack of policy rules, and explains
exactly why.

- Five policy rules, each its own class behind one `Rule` interface
- A precedence model where a hard deny always wins, whatever the rule order
- Explain mode: which rules ran, in what order, and why each passed, failed or was skipped
- Rules can be enabled, disabled, reordered or re-parameterised at runtime, with no restart
- The last seat in a cost-centre goes to exactly one person, even under concurrent requests
- Cancelling or rejecting a booking frees its seat immediately

---

## How to run

**Needs:** Java 21. Nothing else: the database (H2) runs in memory inside the app.

```bash
# Start the app on http://localhost:8080
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

# Run all 39 tests
./mvnw test                     # Windows: .\mvnw.cmd test
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html (click **Authorize** and log in with a user below)
- **Health:** http://localhost:8080/actuator/health
- **Postman collection:** `postman/booking-engine.postman_collection.json`

All data is seeded on startup and reset on every restart.

### Demo users

| Username | Password | Role | Employee id | Cost-centre | Gender | Shift |
|---|---|---|---|---|---|---|
| `aarav` | `password` | EMPLOYEE | 1 | Engineering (20 seats) | Male | 09:00–18:00 |
| `priya` | `password` | EMPLOYEE | 2 | Engineering | Female | 09:00–18:00 |
| `meera` | `password` | EMPLOYEE | 3 | Engineering | Female | 14:00–23:00 |
| `rohan` | `password` | EMPLOYEE | 4 | Operations (2 seats) | Male | 22:00–06:00 |
| `kavya` | `password` | EMPLOYEE | 5 | Operations | Female | 09:00–18:00 |
| `ishaan` | `password` | EMPLOYEE | 6 | Operations | Male | 09:00–18:00 |
| `approver` | `approver123` | APPROVER | — | — | — | — |
| `admin` | `admin123` | ADMIN | — | — | — | — |

Seat limits are per cost-centre, per day, per direction. Operations has only 2 seats so the quota
and cancel behaviour is easy to demo.

---

## API

| Method | Path | Who | What it does |
|---|---|---|---|
| POST | `/api/bookings/evaluate` | Employee, Admin | Dry run with the full trace. Writes nothing, takes no lock. Always 200 |
| POST | `/api/bookings` | Employee, Admin | Real booking. **201** ALLOW, **202** NEEDS_APPROVAL, **422** DENY |
| GET | `/api/bookings/{id}` | Owner, Admin | One booking |
| POST | `/api/bookings/{id}/cancel` | Owner, Admin | Cancel and free the seat. Safe to call twice |
| GET | `/api/approvals/pending` | Approver | Bookings waiting for approval |
| POST | `/api/approvals/{id}/approve` | Approver | PENDING_APPROVAL → CONFIRMED |
| POST | `/api/approvals/{id}/reject` | Approver | PENDING_APPROVAL → REJECTED (seat freed) |
| GET | `/api/admin/rules?clientId=acme` | Admin | All rule rows |
| PUT | `/api/admin/rules/{id}` | Admin | Change one rule: `enabled`, `priority`, `onFail`, `params` |
| GET | `/actuator/health` | Anyone | Health check |
| GET | `/actuator/metrics/{name}` | Admin | Counters and timers |

Employees can only see, book and cancel their own bookings (`403 FORBIDDEN` otherwise).
Add `?explain=true` to `POST /api/bookings` to get the trace on a real booking too.

### Example 1: the case study's "good" booking → ALLOW

```bash
curl -u aarav:password -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"date":"2026-10-15","direction":"LOGIN","requestedTime":"09:00",
       "pickup":"Home","drop":"Office Tower B"}'
```

```json
{ "decision": "ALLOW", "bookingId": 1, "status": "CONFIRMED", "reasons": [],
  "configVersion": 1, "evaluatedAt": "2026-10-11T14:30:00" }
```

Use a date at least a day ahead, so the cut-off rule passes. Sending the same request again returns
`422` with reason `DUPLICATE`.

### Example 2: the case study's "bad" booking, with explain mode → DENY / NIGHT_SAFETY

```bash
curl -u meera:password -X POST http://localhost:8080/api/bookings/evaluate \
  -H "Content-Type: application/json" \
  -d '{"employeeId":3,"date":"2026-10-15","direction":"LOGOUT","requestedTime":"23:30",
       "pickup":"Office Tower B","drop":"Sector 62, Noida","escort":false}'
```

```json
{
  "decision": "DENY",
  "reasons": [
    { "code": "NIGHT_SAFETY",
      "message": "female employee, 23:30 is inside night hours 20:00 to 06:00, no escort on the booking" }
  ],
  "configVersion": 1,
  "evaluatedAt": "2026-10-11T14:30:00",
  "trace": [
    { "order": 1, "rule": "SHIFT_WINDOW", "onFail": "DENY", "outcome": "PASS", "reasonCode": null,
      "detail": "23:30 is inside shift 14:00 to 23:00 with 30 min grace" },
    { "order": 2, "rule": "CUTOFF", "onFail": "DENY", "outcome": "PASS", "reasonCode": null,
      "detail": "departure in 6300 min, cut-off is 60 min" },
    { "order": 3, "rule": "NIGHT_SAFETY", "onFail": "DENY", "outcome": "FAIL", "reasonCode": "NIGHT_SAFETY",
      "detail": "female employee, 23:30 is inside night hours 20:00 to 06:00, no escort on the booking" },
    { "order": 4, "rule": "DUPLICATE", "onFail": "DENY", "outcome": "SKIPPED", "reasonCode": null,
      "detail": "skipped after hard deny by NIGHT_SAFETY" },
    { "order": 5, "rule": "COST_CENTRE_QUOTA", "onFail": "DENY", "outcome": "SKIPPED", "reasonCode": null,
      "detail": "skipped after hard deny by NIGHT_SAFETY" }
  ]
}
```

The same request with `"escort": true` is allowed.

### Example 3: change a rule without a restart

```bash
curl -u admin:admin123 -X PUT http://localhost:8080/api/admin/rules/2 \
  -H "Content-Type: application/json" \
  -d '{"params":{"minutesBefore":30}}'
```

A booking 40 minutes before departure is denied with `PAST_CUTOFF` before this call and allowed after
it. The `configVersion` in every decision goes from 1 to 2. Invalid values such as
`{"minutesBefore": -5}` are refused with `400 INVALID_RULE_PARAMS` and never saved.

---

## The rules

| Seeded id | Rule | Reason code | Params (default) | Fails when |
|---|---|---|---|---|
| 1 | `SHIFT_WINDOW` | `OUTSIDE_SHIFT_WINDOW` | `graceMinutes: 30` | The requested time is outside the shift widened by the grace on both sides. Works for shifts that cross midnight (22:00–06:00) |
| 2 | `CUTOFF` | `PAST_CUTOFF` | `minutesBefore: 60` | Fewer than N minutes are left before departure. Exactly N still passes |
| 3 | `NIGHT_SAFETY` | `NIGHT_SAFETY` | `nightStart: "20:00"`, `nightEnd: "06:00"` | The employee is female, the time is inside night hours, and there is no escort on the booking |
| 4 | `DUPLICATE` | `DUPLICATE` | — | An active booking (CONFIRMED or PENDING_APPROVAL) exists for the same employee, date and direction |
| 5 | `COST_CENTRE_QUOTA` | `QUOTA_EXCEEDED` | `seatsPerDay: 20` | All seats for that cost-centre, date and direction are taken |
| 6 | `COST_CENTRE_QUOTA` | `QUOTA_EXCEEDED` | `seatsPerDay: 2` | Override for the Operations cost-centre only |

All rules ship with `onFail: DENY`. An admin can switch any of them to `NEEDS_APPROVAL`. For example,
night safety as a soft rule sends the booking to an approver, who arranges the escort.

A rule row with a `costCentreId` overrides the client-wide row of the same type (rule 6 overrides
rule 5 for Operations). Rules live in the `rule_config` table, with their params stored as JSON.

---

## Precedence model

Every rule has an `onFail` effect: **DENY** (hard) or **NEEDS_APPROVAL** (soft).

1. **The verdict is the most severe failure.** DENY beats NEEDS_APPROVAL beats ALLOW. If nothing
   fails, the answer is ALLOW. Because the verdict is a maximum and not "first match wins", changing
   the rule order can never change the verdict. `RuleEngineTest.verdictNeverDependsOnRuleOrder`
   shuffles the rules 100 times to prove it.
2. **Fixed order.** Rules run hard rules first, then by `priority` (lower first), then by id.
   The same request with the same config always runs the same way.
3. **Short-circuit.** After the first hard failure the verdict can't change any more. Rules that query
   the database (`DUPLICATE`, `COST_CENTRE_QUOTA`) are then skipped and shown as `SKIPPED`. The
   in-memory rules still run, so a request that breaks two rules reports both reasons.
4. **Fail closed.** If a rule throws an exception, it counts as failed with reason `RULE_ERROR`.
   A broken rule can never let a booking through.

**Deterministic decisions.** Rules never read the clock themselves. "Now" comes from an injected
`Clock` (Asia/Kolkata), cut to whole minutes, and is part of the evaluation input. Every decision
also returns the `configVersion` it was made with.

---

## Concurrency: the last seat

`BookingService.book()` runs in one transaction:

1. Lock the employee's cost-centre row (`SELECT ... FOR UPDATE` through `@Lock(PESSIMISTIC_WRITE)`).
2. Run the rules. The quota rule counts active bookings for that cost-centre, date and direction.
3. If the decision is not DENY, save the booking. Commit, which releases the lock.

A second booking for the same cost-centre waits at step 1 until the first commits, so the count it
sees already includes the first booking. Exactly one request gets the last seat.
`QuotaConcurrencyTest` sends 2 and 20 simultaneous bookings for a single seat and checks that exactly
one is allowed.

**Cancelling frees the seat** because seats are counted from active bookings, not stored in a counter.
A cancelled or rejected booking simply stops counting. Cancelling twice is safe: the second call
changes nothing.

Bookings for different cost-centres never wait for each other.

---

## Caching, and what can be stale

| Data | Cached? | Details |
|---|---|---|
| Active, sorted rules per cost-centre | Yes, Caffeine | Max 1,000 entries, expires 10 minutes after writing. The whole cache is cleared on every admin rule change, because one client-wide rule row affects every cost-centre |
| Seat counts | No | Must be exact, so they are always counted live under the lock |
| Existing bookings (duplicate check) | No | Must see a booking made a moment ago |

**What can be stale:** nothing on a single instance, because every rule change clears the cache. With
several app instances, the other instances could use old rules for up to 10 minutes. The fix would be
to broadcast cache clears (for example over Redis pub/sub).

---

## Errors and failure handling

Every error from the application uses one JSON shape, with no stack traces or SQL:

```json
{ "timestamp": "...", "status": 400, "errorCode": "VALIDATION_FAILED",
  "message": "direction must not be null", "path": "/api/bookings", "requestId": "7f3a9c21" }
```

| Situation | Status | `errorCode` |
|---|---|---|
| Booking refused by a rule | 422 | — (the body is the decision, with `reasons`) |
| Missing or blank field, pickup same as drop | 400 | `VALIDATION_FAILED` |
| Invalid JSON, bad date/time, unknown enum value | 400 | `MALFORMED_REQUEST` |
| Bad rule params, or `onFail: ALLOW` | 400 | `INVALID_RULE_PARAMS` / `INVALID_RULE_CONFIG` |
| Unknown employee, booking or rule | 404 | `NOT_FOUND` |
| Acting on someone else's booking | 403 | `FORBIDDEN` |
| Approving a booking that isn't pending | 409 | `INVALID_STATE` |
| Not logged in | 401 | Spring Security's default response |
| Wrong role for the URL | 403 | Spring Security's default response |
| Anything unexpected (lock timeout, database down) | 500 | `INTERNAL_ERROR`, with the full error in the logs only |

- **A rule crashes:** fails closed with `RULE_ERROR`.
- **Crash halfway through a booking:** the lock, rule checks and save are one transaction, so nothing is half-written.
- **Bad config:** rejected before it is saved. The old config keeps running.
- **Restart:** the cache rebuilds from the database. (H2 is in memory, so demo data resets; production would use PostgreSQL with backups.)

---

## Monitoring and logs

Actuator metrics (admin only), at `/actuator/metrics/{name}`:

| Metric | Type | Tag | Shows |
|---|---|---|---|
| `booking.decisions` | counter | `decision` | ALLOW / DENY / NEEDS_APPROVAL counts |
| `rule.violations` | counter | `rule` | Which rule rejects most (`?tag=rule:NIGHT_SAFETY`) |
| `booking.cancellations` | counter | — | Seats returned |
| `approval.outcomes` | counter | `result` | Approved vs rejected |
| `rule.engine.evaluation` | timer | — | Time spent inside the rule engine |

Every request gets an id (`X-Request-Id` header, generated if missing). It appears on every log line,
and in every error body. Each decision logs one summary line:

```text
INFO [requestId=7f3a9c21] ... decision=DENY reasons=NIGHT_SAFETY employeeId=3 configVersion=1
```

Every real booking attempt, including denied ones, is saved in the `decision_log` table with its full
trace, as an audit trail.

---

## Design

```text
HTTP request
  → RequestIdFilter → Spring Security (HTTP Basic, roles)
  → Controller        (validates the request, picks the status code)
  → BookingService    (transaction, cost-centre lock, ownership checks, audit, metrics)
      → RuleConfigService (active rules from the database, cached)
      → RuleEngine        (runs the rules, builds the decision and trace)
          → 5 Rule classes
  → Repositories (Spring Data JPA) → H2
```

**Object-oriented design**

- **Polymorphism:** `RuleEngine` only knows the `Rule` interface. Five classes implement it.
- **Inheritance / template method:** every rule extends `AbstractRule`, whose final `evaluate()` wraps the rule's `check()` in try/catch so crashes fail closed. The exceptions share one parent, `ApiException`.
- **Encapsulation:** a booking's status only changes through `Booking.cancel()`, `approve()` and `reject()`, which enforce the allowed transitions.
- **Open for extension:** a new rule type is one new class plus a config row. Spring injects every `Rule` bean into the engine, so the engine itself doesn't change.
- **Layers:** controller → service → engine → repository. The engine is plain Java and is unit-tested without a database.

---

## Complexity

R = enabled rules (5 here), N = bookings.

| Step | Time | Space |
|---|---|---|
| Load and sort rules | O(R log R), once per config change, then cached | O(R) per cache entry |
| Evaluate one request | O(R) rule calls | O(R) for the trace |
| Shift, cut-off and night checks | O(1) each | O(1) |
| Duplicate check | O(log N) through index `(employee_id, booking_date, direction)` | O(1) |
| Quota count | O(log N + k) through index `(cost_centre_id, booking_date, direction, status)`, k ≤ seats per day | O(1) |

---

## Assumptions

- Routing isn't modelled, so every night-hours booking for a woman is treated as possibly first pickup or last drop, and needs an escort.
- Shift window = shift start − grace to shift end + grace, for both login and logout.
- Quota = seats per cost-centre, per day, per direction (LOGIN and LOGOUT are separate trips).
- A NEEDS_APPROVAL booking holds a seat until it is approved or rejected.
- All times are in one timezone (Asia/Kolkata).
- Data is in memory (H2) and resets on every restart.

---

## Trade-offs

| Choice | Alternative | Why |
|---|---|---|
| Small custom rule engine | Drools / Easy Rules | Five rules and a strict "explain why" requirement. About 60 lines that are easy to trace. Drools pays off with hundreds of business-written rules |
| Lock the cost-centre row, then count | Seat counter column with a conditional `UPDATE` | Both are correct. Counting rows means cancel and reject free seats with nothing extra to update. The cost: bookings for the same cost-centre are handled one at a time |
| Pessimistic lock | Optimistic locking (`@Version`) | Optimistic locking is also correct, but the losing request gets an exception and needs a retry loop |
| Database lock | Java `synchronized` | `synchronized` only works inside one app instance; two instances would both sell the last seat |
| Skip only database rules after a hard deny | Stop at the first deny / always run everything | Every in-memory failure is still reported, while queries that can't change the verdict are skipped |
| Caffeine in-process cache | Redis | Nothing extra to run. Stale on other instances for up to 10 minutes |
| HTTP Basic | JWT | Quick to build and test. JWT is the production choice |
| H2 in memory | PostgreSQL | Runs anywhere with no setup. Switching needs the PostgreSQL driver in `pom.xml` and new connection settings in `application.yml` |
| Rule params as JSON | One column per parameter | Each rule has different settings; a new setting needs no table change |

---

## Tests

39 tests (`./mvnw test`):

| Test class | What it proves |
|---|---|
| `TimeWindowsTest` | Time windows, including ones that cross midnight |
| `ShiftWindowRuleTest`, `CutoffRuleTest`, `NightSafetyRuleTest`, `CostCentreQuotaRuleTest` | Each rule, including exact boundaries (the grace edge, exactly N minutes, 20:00) |
| `RuleEngineTest` | Hard deny beats soft, order never changes the verdict (100 shuffles), database rules skipped after a deny, all in-memory reasons reported, crashing rule fails closed, sort order |
| `QuotaConcurrencyTest` | 2 and 20 simultaneous bookings for one seat → exactly one allowed |
| `BookingFlowTest` | Full HTTP flows: good and bad examples, duplicate, cancel frees a seat, live rule change and approval, validation, 401/403/404 |

---

## Project structure

```text
src/main/java/com/example/bookingengine/
├── config/          Security, Clock, Swagger, demo data
├── controller/      Booking, Approval, AdminRule (+ Ping)
├── domain/          Entities and enums
├── dto/             Request and response shapes
├── engine/          Rule interface, AbstractRule, RuleEngine, TimeWindows
│   └── rules/       The five rules
├── exception/       ApiException family + GlobalExceptionHandler
├── observability/   RequestIdFilter
├── repository/      Spring Data JPA repositories
└── service/         BookingService, RuleConfigService
```

---

## Requirements checklist

| Case study item | Where |
|---|---|
| Model employees, bookings, configurable rules | `domain/`, `RuleConfig` |
| One decision with exact reasons | `RuleEngine`, `DecisionResponse.reasons` |
| Deterministic decisions | Injected `Clock`, fixed sort order, `configVersion` |
| Precedence and short-circuit | `RuleEngine`, `RuleEngineTest` |
| The five rules | `engine/rules/` |
| Change rules without a redeploy | `PUT /api/admin/rules/{id}` + cache clear |
| Explain mode | `POST /api/bookings/evaluate` |
| Atomic quota | `CostCentreRepository.lockById`, `QuotaConcurrencyTest` |
| Cancellation frees quota | Status-based seat counting, `BookingFlowTest` |
| Authentication and roles | `SecurityConfig`, ownership checks in `BookingService` |
| Complexity | [Complexity](#complexity) |
| Failure handling | [Errors and failure handling](#errors-and-failure-handling) |
| OOP | [Design](#design) |
| Trade-offs | [Trade-offs](#trade-offs) |
| Monitoring | Actuator metrics, request ids, `decision_log` |
| Caching | Caffeine + [what can be stale](#caching-and-what-can-be-stale) |
| Error handling | `GlobalExceptionHandler`, one error shape |

---

## With more time

- Keep a history of rule changes, so the exact rules behind any `configVersion` can be shown
- JWT authentication instead of HTTP Basic
- PostgreSQL with Flyway migrations and backups
- Broadcast cache clears across instances (for example Redis pub/sub)
- Return Spring Security's 401/403 responses in the same JSON shape as other errors

---

## My approach

I started by designing the core domain models and the rule evaluation contract before touching any Spring controllers or database queries. The hardest part was getting the race condition right: ensuring that two concurrent bookings for the last seat don't both succeed, while also ensuring that cancellations cleanly return seats to the quota without drift. Because of the deadline, I chose H2 in memory and HTTP Basic authentication instead of running external PostgreSQL and JWT services, which kept setup zero-friction for evaluators.

## What I learned

I learned how crucial database transaction boundaries are when handling concurrency—when I commented out `costCentres.lockById()` in `BookingService` and ran `QuotaConcurrencyTest`, 10 out of 20 concurrent threads simultaneously grabbed the single remaining seat (`expected: <1> but was: <10>`). Re-enabling the pessimistic lock serialized the seat reservations and restored 100% quota safety. With another week, I would implement an audit history table for rule version snapshots and replace HTTP Basic with JWT authentication.
