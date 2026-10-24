# AGENTS.md — TicketMonster (Quarkus/Java Modernization)

> Operational guide for any agent (human or AI) generating code in this repository.
> Architectural source of truth: `docs/arch/arquitetura-solucao.md`.
> Legacy source of truth: `docs/legacy/projeto-legado.md`.
> Per-service specs: `docs/spec/microservice-*.spec.md`.

This file does not replace the documents above — it summarizes the **mandatory and verifiable**
rules for generating code consistent with decisions already made (ADRs 01–09).

---

## 1. Context

Modernization of the Java EE 6 monolith (`docs/legacy/projeto-legado.md`) into reactive Quarkus
microservices. 4 target services, each with its own database (Database per Service):

| Service | Directory | Database | Spec |
|---|---|---|---|
| `microservice-catalog` | `microservice-catalog/` | `catalog_db` | `docs/spec/microservice-catalog.spec.md`* |
| `microservice-inventory` | `microservice-inventory/` | `inventory_db` + Redis | `docs/spec/microservice-inventory.spec.md` |
| `microservice-booking` | `microservice-booking/` | `booking_db` | `docs/spec/microservice-bookin.spec.md` |
| `microservice-telemetry` | `microservice-telemetry/` | `telemetry_db` (Timescale) + Redis | `docs/spec/microservice-telemetry.spec.md` |

`*` The catalog spec currently lives consolidated in `arquitetura-solucao.md` section 6 + embedded
DDL; confirm whether it will be extracted into its own file under `docs/spec/`.

**Decisions already closed (do not reopen without a new ADR):**
- **ADR 05:** **Choreography** Saga via Kafka (not Orchestration).
- **ADR 06:** **Token Relay** (not Token Exchange) for identity propagation.
- **ADR 07:** Allocation result event = `SeatsReservedEvent` / `SeatsReservationFailedEvent`
  (not `SeatsAllocatedEvent`).
- **ADR 08:** **Jakarta Bean Validation (Hibernate Validator)** as primary input validation layer,
  with programmatic/domain validation (`Objects`, VOs) used complementarily.
- **ADR 09:** **MapStruct** is the **single, mandatory** mechanism for object-to-object mapping
  (DTO/Response ↔ domain, domain ↔ JPA entity, event ↔ internal model). Manual mapping and
  hand-written `*Mapper` classes are forbidden (see section 2 and the patterns table).

Any code or spec that contradicts these decisions must be flagged, not silently "corrected".

**Note on Java version:** the codebase already runs on **Java 25** (see section 2), diverging from
the Java 21 recorded in the ADR 01 text. Until the ADR is updated, the standard to follow is the
one in the code: Java 25.

---

## 2. Mandatory tech stack

- **Java 25**, as configured in `microservice-catalog/pom.xml` (`maven.compiler.release=25`) — use
  this value in all new modules. *(Diverges from `arquitetura-solucao.md` ADR 01 text, which cites
  Java 21; report to the team to update the ADR and reflect the actual decision in the code.)*
- **Quarkus 3.27+** (current pom uses `3.37.4` — compatible, keep pinned via BOM).
- **Hibernate Reactive with Panache** — never blocking/synchronous JPA (`quarkus-hibernate-reactive-panache`,
  not `quarkus-hibernate-orm-panache`).
- **Mutiny** (`Uni<T>`, `Multi<T>`) for every I/O operation. `.await().indefinitely()` is forbidden
  outside tests.
- **Reactive Kafka** via `quarkus-smallrye-reactive-messaging-kafka` (current pom: `quarkus-messaging-kafka`
  — confirm the correct artifact for the BOM version in use).
- **Redis** via `quarkus-redis-client` (locks, cache, ephemeral state — never the source of truth
  for permanent data).
- **OIDC, Quarkus Security**, **quarkus-oidc** + Keycloak — never implement manual JWT validation.
- **quarkus-smallrye-fault-tolerance** for `@CircuitBreaker`/`@Retry`/`@Timeout`/`@Bulkhead` on
  synchronous calls between services.
- **Jakarta Bean Validation / Hibernate Validator** (`quarkus-hibernate-validator`, ADR 08) as the
  primary input validation layer on REST adapters/DTOs (producing RFC 7807 responses);
  programmatic defensive checks (`java.util.Objects.requireNonNull`, etc.) and domain Value
  Objects/Aggregates used complementarily for business invariants.
- **MapStruct** (`org.mapstruct:mapstruct`, version pinned via `org.mapstruct.version`, with
  `mapstruct-processor` in `annotationProcessorPaths` of the `maven-compiler-plugin`) — the single,
  mandatory object-to-object mapping mechanism (ADR 09). Every `@Mapper` is declared with
  `componentModel = "cdi"` (or `MappingConstants.ComponentModel.JAKARTA_CDI`) so the generated
  implementation is an injectable CDI bean. **Never** write manual conversions (`new X(...)` plus
  getters) nor hand-written `*Mapper` classes for the same boundary.
- **TDD** - Always start with a test before implementation.
- **Liquibase** for schema migration (`src/main/resources/db/changelog`).
- **UUID (`gen_random_uuid()`, pgcrypto)** as PK on every entity — never `BIGSERIAL`
  (decision documented in every `*_data_model.md`; reason: IDs are replicated across services via
  events).

---

## 3. Package structure (Clean Architecture — mandatory)

Every microservice follows the 4 layers from `arquitetura-solucao.md` section 4.1, with dependency
pointing inward:

```
br.vsjr.labs.ticketmoster.<service>
├── adapter/
│   ├── in/
│   │   ├── rest/        # JAX-RS Resources
│   │   │   └── mapper/   # MapStruct: DTO/Response <-> domain (ADR 09)
│   │   └── messaging/    # Kafka Consumers
│   │       └── mapper/   # MapStruct: event payload <-> internal model (ADR 09)
│   ├── out/
│   │   ├── persistence/  # Panache Repositories (implement Ports)
│   │   │   ├── entity/   # JPA entities (infrastructure only)
│   │   │   └── mapper/   # MapStruct: domain <-> JPA entity (ADR 09)
│   │   ├── redis/        # Redis Client (implements Ports)
│   │   └── messaging/     # Kafka Publisher (implements Ports)
│   │       └── mapper/   # MapStruct: internal model <-> event payload (ADR 09)
├── application/
│   ├── usecase/          # Use Cases / Application Services (one class per operation)
│   └── port/              # Outbound interfaces (implemented in adapter/out)
└── domain/
    ├── model/             # Aggregates, Entities
    ├── vo/                # Value Objects (Java Records)
    ├── event/             # Domain Events
    └── spec/               # Specifications
```

**Hard rule:** the `domain` layer must not import `io.quarkus.*`, `jakarta.persistence.*` (except
framework-agnostic annotations), `io.smallrye.mutiny.*` inside pure business rules, or any
messaging types. This resolves the legacy's structural problem
(`SectionAllocation.allocateSeats` entangled with JPA) — see `arquitetura-solucao.md` section 3,
item 2 and section 4.1.

If an agent is tempted to put business logic inside a REST `Resource` or a Panache Repository,
that's a sign of the wrong layer — move it to `application/usecase` or `domain`.

**Mapping rule (ADR 09):** `@Mapper` interfaces live in the adapter's `mapper/` package — never in
`domain`. `domain` has no knowledge of DTOs, JPA entities, or MapStruct.

---

## 4. Functional programming integrated with OO (architecture doc section 13)

An explicit project rule — not optional:

1. **Rich domain, not anemic.** Aggregates (`Booking`, `SeatMap`) are objects with behavior, not
   getter/setter DTOs.
2. **Value Objects as Java Records**, immutable: `Seat`, `Money`, `Email`, `CancellationCode`.
   Raw `float`/`String` for domain concepts is forbidden (eliminates *primitive obsession* —
   patterns catalog, section 20.1).
3. **Result/Either instead of exceptions for expected business flow** (not for infrastructure
   errors). The booking creation pipeline is modeled as a composition of pure transformations.
   - If Vavr (`Either<Error, T>`) is adopted, add the dependency explicitly to the module's
     `pom.xml` — it is **not currently present** in `microservice-catalog/pom.xml`.
   - Architecture-accepted alternative: `Uni<T>` operators (`.chain()`, `.onFailure()`) without
     Vavr.
   - **The decision on which of the two to standardize on is not yet formalized — do not mix both
     styles within the same module without aligning first.**
4. **Pure, mock-free functions for calculation rules**, testable without a database, e.g.:
   `calculateContiguousBlock(SeatMap, quantity): Either<AllocationError, List<Seat>>`.
   Do not couple allocation logic to `EntityManager`/Panache — that was exactly the legacy problem
   (`SeatAllocationService`, see `projeto-legado.md` section 9).
5. **Never use nested `try/catch` as a business control-flow structure** — that is the legacy
   pattern (`BookingService.createBooking`, an 80+ line method mixing validation, allocation, and
   persistence) that this modernization exists to eliminate.

---

## 5. Mandatory patterns by scenario (catalog section 20)

| Scenario | Pattern | Do not do |
|---|---|---|
| Business rule on an aggregate | Aggregate + Domain Events | Anemic model |
| Use case | 1 Use Case/Interactor per operation | "Generic" service with unrelated methods |
| Communication between services | Ports & Adapters | Domain calling an HTTP/Kafka client directly |
| Seat lock | Redis `SET NX PX` (ADR 02) | JPA/DB pessimistic lock |
| Final Booking persistence | Optimistic lock (`@Version`) | Pessimistic lock |
| Distributed checkout | Choreographed Saga (ADR 05) | Central orchestrator |
| Event publication + local write | Outbox Pattern | Direct publish to Kafka in the same call |
| Kafka consumption | Idempotent Receiver | Assuming exactly-once delivery |
| Failure in a synchronous chain | Circuit Breaker + Retry + Timeout | Unlimited retry / no timeout |
| Read cache | Cache-Aside | Cache with no TTL/invalidation |
| Input validation | Jakarta Bean Validation (`@Valid`, Hibernate Validator) on DTOs (ADR 08) | Manual `if-null` clutter in resources / use cases |
| Object-to-object conversion | MapStruct `@Mapper(componentModel = "cdi")` (ADR 09) | Manual `new X(...)` + getters; hand-written `*Mapper` classes |
| Domain invariants | Value Objects + `java.util.Objects` + Aggregates | Relying exclusively on edge DTO validation for domain rules |

---

## 6. Business rule traceability

Every rule implemented in code must be traceable to an origin. When generating/reviewing code:

- **RNxx** → rule inherited from the legacy system, see `projeto-legado.md` section 11 (RN01–RN42
  table).
- **RN-NOVAxx** → new rule introduced by the modernization, with no legacy equivalent
  (`arquitetura-solucao.md` section 25). Never treat it as a "migration".
- **[ALTERA RNxx]** → an altered rule; keep the as-is RN documented for traceability, do not erase
  it from history.
- When adding a new rule not covered by any of the documents, **explicitly state the gap** in the
  PR/commit, do not invent an origin.

Suggested reference comment in code (Javadoc on the Use Case or domain class):
```java
// RN17 (projeto-legado.md) — ticket price = TicketPrice in effect at creation time
// RN-NOVA-03 — idempotency via Idempotency-Key
```

---

## 7. Domain events and Kafka contracts

- Naming fixed by **ADR 07**: `SeatsReservedEvent` / `SeatsReservationFailedEvent`
  (not `SeatsAllocatedEvent`). The legacy `SectionAllocation`/`SeatAllocationService` used the verb
  "allocate" — this is intentionally **not** carried over into the event name (see ADR 07,
  rationale).
- Event payload **never carries a JWT** — it includes the user's `sub` or `"system:bot"` for audit
  purposes (RN-NOVA-05, section 15.3).
- Payload without raw PII whenever possible — identifiers only (section 15.4.3).
- Schema Registry (Avro/JSON Schema) is an already-made decision (section 19) — do not introduce
  events without a versioned schema.
- `booking` publishes to `booking-events`; only authorized `inventory`/`telemetry` consume it
  (per-topic ACL, section 15.4.3).

---

## 8. Security (Keycloak + RBAC + ownership)

- Never validate a JWT manually — use `quarkus-oidc` + the injected `JsonWebToken`.
- RBAC (`ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_SERVICE`) via `@RolesAllowed`, **but RBAC alone is not
  enough** (section 15.4.1): every operation on a `Booking` requires an ownership check
  (`sub == booking.ownerId` OR a valid `X-Cancellation-Code`) **inside the Use Case**, not just via
  declarative annotation. See the reference example in `arquitetura-solucao.md` section 15.5.
- Administrative write routes (`POST/PUT/DELETE`) require `ROLE_ADMIN` — fixes RN-NOVA-02 (the
  legacy admin panel today has zero authentication).
- `GET /bookings` (full listing) is `ROLE_ADMIN`-only — in the legacy it's public and exposes
  third-party emails (RN-NOVA-01, OWASP Excessive Data Exposure).
- Service-to-service calls (e.g., `booking → catalog`) use a `Client Credentials` token
  (`ROLE_SERVICE`), never relaying the end user's token for that purpose.

---

## 9. Persistence

- UUID as PK, always (`gen_random_uuid()`).
- **No FKs across different services' databases** — references to another service are plain UUID
  columns, validated at runtime (event/call), never a DB `REFERENCES`.
- Where another service needs read data from a neighboring service, use a **snapshot** table
  populated by consuming events (e.g., `inventory.section_snapshot` fed by `catalog` events) —
  never a synchronous call in the critical allocation path.
- Partial unique indexes as defense-in-depth beyond the application-level lock (e.g.,
  `uq_seat_ledger_occupied` in `inventory`, data model section 1).
- Schema migration always via Liquibase (`db/changelog`), never manual DDL in production.

---

## 10. Testing

- **Unit tests exist to find bugs relative to the specification, not to validate the
  implementation.** A test must fail when behavior deviates from the spec (RN/RN-NOVA/CA), not
  when a refactor changes internal structure while preserving behavior. Do not write tests that
  assert internal implementation details (private state, call order, specific class collaborators)
  unless the spec itself mandates that behavior.
- **The specification is the only source for generating unit tests.** Test cases are derived from
  `docs/spec/microservice-*.spec.md`, the RN/RN-NOVA tables, and the acceptance criteria (CA-*) —
  never from reading the implementation and reproducing what it currently does. If a rule needed
  for a test isn't documented, stop and flag the gap (section 6) instead of inferring it from code.
- **Domain classes must be 90% testable** without infrastructure (no Testcontainers, no mocks of
  Panache/Redis/Kafka). If a domain rule (Value Object, Specification, aggregate invariant,
  allocation calculation) cannot be unit-tested without spinning up infrastructure, the logic is
  in the wrong layer — move it out of `adapter`/`application` and into `domain` (see section 3).
- **Testcontainers** for Postgres/Redis/Kafka in integration tests — mocking these dependencies in
  an integration test is forbidden (section 19, "market improvements").
- **Contract Testing (Pact)** between services that exchange events/APIs — a contract change must
  not silently break a consumer.
- Every Use Case implementing an RN or RN-NOVA must have a test that references the rule in its
  name (`@DisplayName("RN15 - booking requires at least 1 ticket")`).

---

## 11. Local build and execution

```bash
# dev mode for a specific microservice
cd microservice-<name>
./mvnw quarkus:dev

# full local stack (Postgres/Redis/Kafka) — see docker-compose.yml at the repo root
docker-compose up -d

# tests (unit + integration via Testcontainers)
./mvnw verify
```

---

## 12. Checklist before opening a PR / considering a task done

- [ ] Is the code in the correct layer (domain free of framework dependencies)?
- [ ] Is the business rule referenced (RN/RN-NOVA) in a comment or test name?
- [ ] Were unit tests derived from the specification, not from the implementation?
- [ ] Is the touched domain code still testable without infrastructure (≥90% target)?
- [ ] Was any ADR 05/06/07/08/09 decision contradicted without a recorded justification?
- [ ] Are input DTOs validated using Jakarta Bean Validation / Hibernate Validator (ADR 08) as primary defense?
- [ ] Does every object-to-object conversion use a MapStruct `@Mapper` (ADR 09), with no manual `new X(...)` + getters and no hand-written `*Mapper` classes?
- [ ] Does the published event follow ADR 07 naming and avoid carrying a JWT/raw PII?
- [ ] Does a new endpoint check RBAC **and** ownership where applicable (section 8)?
- [ ] Do local write + event publication use the Outbox (not direct publish)?
- [ ] Does the integration test use Testcontainers, not mocked Postgres/Redis/Kafka?
- [ ] Is a new PK a UUID, with no cross-service FK?

---

## 13. Legacy anti-patterns — never reintroduce

See `projeto-legado.md` section 3 (Architectural Diagnosis) for details on each:

1. Whole-section pessimistic lock (`LockModeType.PESSIMISTIC_WRITE`) — replaced by per-seat Redis
   NX locking (ADR 02).
2. Seat matrix serialized as a binary `@Lob` — replaced by the queryable, relational `seat_ledger`.
3. In-process CDI events (`Event<Booking>`) — replaced by Kafka + Outbox.
4. Blocking synchronous HTTP call for external media (`MediaManager`) — I/O is always reactive.
5. Duplicated public vs. Forge endpoints without RBAC — a single `Booking` creation flow, with RBAC
   and ownership checks (`projeto-legado.md` section 14, `arquitetura-solucao.md` section 25.2).
6. Fixed, unvalidated cancellation code `"abc"` (RN29/RN30 as-is) — hash + mandatory validation
   (RN29/RN30 to-be, `microservice-booking.spec.md`).
7. Manual object mapping (`new X(...)` plus getters) and hand-written `*Mapper` classes
   (`@ApplicationScoped`) duplicating a generated MapStruct mapper — replaced by a MapStruct
   `@Mapper(componentModel = "cdi")` per boundary (ADR 09).

---

## 14. References

- `docs/arch/arquitetura-solucao.md` — target architecture, ADRs, roadmap.
- `docs/legacy/projeto-legado.md` — as-is business rules (RN01–RN42), legacy sequence diagrams.
- `docs/spec/microservice-*.spec.md` — spec + data model per service.
- `docs/olds/` — earlier versions consolidated into `arquitetura-solucao.md`; do not use as a
  standalone source of truth, historical reference only.