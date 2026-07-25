## ADDED Requirements

### Requirement: Public catalog returns only active events
The public catalog query SHALL return only events with status `PUBLISHED` and SHALL exclude events in `DRAFT` or `ARCHIVED` status.

#### Scenario: Query returns only published events
- **WHEN** the public catalog endpoint is called and the database contains events with statuses `DRAFT`, `PUBLISHED`, and `ARCHIVED`
- **THEN** the response includes only events with status `PUBLISHED`

#### Scenario: Query has no active events
- **WHEN** the public catalog endpoint is called and there are no events with status `PUBLISHED`
- **THEN** the response returns an empty collection with successful HTTP status

### Requirement: Active catalog response is cacheable
The service SHALL apply cache-aside for the public active-events listing and SHALL reuse cached results while the cache entry is valid.

#### Scenario: Cached response is reused
- **WHEN** two equivalent public catalog requests are executed within the cache TTL window
- **THEN** the second response is served from cache without a new database query

### Requirement: Cache is invalidated on active state transitions
The service SHALL invalidate the active-events cache whenever an administrative operation changes an event status to or from `PUBLISHED`.

#### Scenario: Publishing invalidates cache
- **WHEN** an administrator changes an event status from `DRAFT` to `PUBLISHED`
- **THEN** the active-events cache entry is invalidated before the next public query

#### Scenario: Archiving invalidates cache
- **WHEN** an administrator changes an event status from `PUBLISHED` to `ARCHIVED`
- **THEN** the active-events cache entry is invalidated before the next public query
