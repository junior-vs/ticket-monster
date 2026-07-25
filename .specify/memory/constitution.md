<!--
Sync Impact Report
- Version change: template (unversioned) -> 1.0.0
- Modified principles:
	- [PRINCIPLE_1_NAME] -> I. Domain-Centric Code Quality
	- [PRINCIPLE_2_NAME] -> II. Contract-First API and Event Discipline
	- [PRINCIPLE_3_NAME] -> III. Test Depth and Quality Gates (NON-NEGOTIABLE)
	- [PRINCIPLE_4_NAME] -> IV. User Experience Consistency Across Channels
	- [PRINCIPLE_5_NAME] -> V. Performance and Resilience Budgets
- Added sections:
	- Architecture and Delivery Constraints
	- Development Workflow and Review Gates
- Removed sections:
	- None
- Templates requiring updates:
	- ✅ updated .specify/templates/plan-template.md
	- ✅ updated .specify/templates/spec-template.md
	- ✅ updated .specify/templates/tasks-template.md
- Follow-up TODOs:
	- None
-->

# TicketMonster Modernization Constitution

## Core Principles

### I. Domain-Centric Code Quality
Every change MUST preserve strict boundaries between adapter-in, application, domain,
and adapter-out layers. Domain code MUST remain framework-agnostic and MUST NOT depend
directly on HTTP, Kafka, ORM, or infrastructure classes. Public APIs and events MUST be
explicitly versioned at contract boundaries before behavior changes are merged.

Rationale: The legacy bottlenecks were caused by business rules mixed with infrastructure.
This principle keeps business invariants testable, replaceable, and resilient.

### II. Contract-First API and Event Discipline
REST and event contracts MUST be defined before implementation changes. HTTP errors MUST
follow RFC 7807 Problem Details. Event names, payload schema, and ownership MUST be
documented and validated for compatibility before producer rollout.

Rationale: The target architecture is event-driven and distributed. Contract drift causes
multi-service regressions that are expensive to detect late.

### III. Test Depth and Quality Gates (NON-NEGOTIABLE)
No production code change is complete without automated tests mapped to requirements.
Each feature MUST include:
- Unit tests for domain rules and calculations.
- Contract tests for REST and Kafka interfaces when contracts are created or changed.
- Integration tests with real dependencies via Testcontainers for Postgres, Redis,
	and Kafka for affected flows.
- One end-to-end scenario for every P1 user story.

Any failing required test blocks merge. Temporary test waivers require documented risk,
owner, expiry date, and explicit approval.

Rationale: Modernization introduces distributed failure modes not observable through unit
tests alone.

### IV. User Experience Consistency Across Channels
User-facing behavior MUST remain consistent across public and administrative channels.
API semantics MUST be uniform for pagination, identifiers, date/time formats, and error
representation. Security-driven UX (ownership checks, forbidden actions, auth flows) MUST
produce deterministic responses and actionable messages.

At minimum:
- Pagination MUST use page and size with zero-based page indexing.
- Booking creation MUST support idempotent retries without duplicate outcomes.
- Authorization failures MUST return explicit 401 or 403 semantics consistently.

Rationale: The legacy system had duplicated and divergent endpoints. Consistent UX reduces
support load and integration defects.

### V. Performance and Resilience Budgets
Each spec and plan MUST define measurable budgets and validation approach before delivery.
Default budgets for modernization streams are:
- Public catalog reads: p95 latency <= 250 ms under agreed load profile.
- Availability checks: p95 latency <= 150 ms with warm cache.
- Booking initiation API: synchronous response in <= 300 ms returning accepted processing state.
- Error rate: < 1% server-side failures in steady-state tests.

Services MUST include health endpoints, tracing propagation, and metrics for latency,
throughput, and failure reasons. If a budget is exceeded, release requires a mitigation
plan and rollback strategy.

Rationale: Ticket sales are bursty and sensitive to lock contention and downstream lag.
Explicit budgets keep capacity and resilience work first-class.

## Architecture and Delivery Constraints

- Java runtime baseline for new services is Java 21 with Quarkus 3.27+ unless an ADR
	records an exception.
- Service communication style MUST align with approved ADRs:
	- Saga style: choreography for checkout flows.
	- Identity propagation baseline: token relay with per-service validation.
	- Seat reservation result events: SeatsReservedEvent and SeatsReservationFailedEvent.
- Database-per-service is mandatory. Shared database writes across service boundaries are
	prohibited.
- Any change that alters security, event semantics, or consistency behavior MUST include
	ADR linkage in spec or plan artifacts.

## Development Workflow and Review Gates

Every feature artifact MUST pass the following gates:
1. Specification gate: includes prioritized user stories, testable acceptance criteria,
	 UX consistency expectations, and measurable performance outcomes.
2. Plan gate: includes constitution check pass, architecture alignment, test strategy,
	 and observability plan.
3. Task gate: includes explicit tasks for test implementation, contract validation,
	 UX consistency checks, and performance verification.
4. Review gate: pull request evidence includes passing automated tests, benchmark or load
	 evidence for impacted flows, and confirmation of unchanged or intentionally updated
	 contracts.

## Governance

This constitution overrides conflicting local development habits for modernization work.
Amendments require:
1. A documented change proposal with rationale and impact.
2. Update of dependent templates and guidance artifacts in the same change.
3. Team approval by architecture and delivery owners.

Versioning policy:
1. MAJOR for principle removal, incompatible governance changes, or redefinition of
	 compliance obligations.
2. MINOR for new principles, new mandatory sections, or materially expanded gates.
3. PATCH for clarifications and non-semantic wording updates.

Compliance review expectations:
1. Every plan MUST include an explicit constitution check result.
2. Every task set MUST trace how quality, tests, UX, and performance are validated.
3. Exceptions MUST be time-bound and tracked until closure.

**Version**: 1.0.0 | **Ratified**: 2026-07-24 | **Last Amended**: 2026-07-24
