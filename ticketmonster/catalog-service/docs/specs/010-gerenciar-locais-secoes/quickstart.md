# Quickstart: US-CAT-10 - Venue e Section

## Prerequisites

- Docker running.
- Java runtime compatible with `microservice-catalog/pom.xml`.
- Maven wrapper or local Maven available for the module.
- Optional admin JWT with `ROLE_ADMIN` for manual API checks.

## Local Setup

From the repository root:

```powershell
docker compose -f docker-compose.shared.yml up -d
```

From `microservice-catalog`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd quarkus:dev
```

The service uses:

- PostgreSQL `catalog_db`
- Redis DB `0`
- Kafka topic `catalog-events`
- Health endpoints under `/q/health/*`
- Metrics under `/q/metrics`

## Contract Validation

The REST contract is `microservice-catalog/specs/010-gerenciar-locais-secoes/contracts/openapi.yaml`.

Expected checks:

- `SectionWrite` does not include `capacity`.
- `Section.capacity` is read-only in responses.
- Admin write routes require bearer auth.
- Public read routes do not require bearer auth.
- Errors use `application/problem+json`.

## Scenario 1: Create Venue

```powershell
$token = '<admin-jwt>'
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/v1/venues' `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Body '{"name":"Teatro Central","description":"Venue principal","addressLine":"Rua A, 100","city":"Sao Paulo","state":"SP","postalCode":"01000-000","country":"BR"}'
```

Expected:

- HTTP 201.
- Response includes UUID `id`.
- Response does not require sections.
- Repeating the same `name` returns HTTP 409 Problem Details.

## Scenario 2: Create Section and Verify Generated Capacity

```powershell
$venueId = '<created-venue-id>'
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/venues/$venueId/sections" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Body '{"name":"Pista Premium","numberOfRows":10,"rowCapacity":50,"capacity":9999}'
```

Expected:

- HTTP 201.
- Response includes `capacity: 500`.
- The submitted `capacity: 9999` is ignored and never persisted.
- Repeating `name` for the same `venueId` returns HTTP 409.

## Scenario 3: Update Section Geometry

```powershell
$sectionId = '<created-section-id>'
Invoke-RestMethod `
  -Method Put `
  -Uri "http://localhost:8080/api/v1/sections/$sectionId" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Body '{"name":"Pista Premium","numberOfRows":12,"rowCapacity":50}'
```

Expected:

- HTTP 200.
- Response includes `capacity: 600`.
- Redis keys `catalog:venue:{venueId}` and `catalog:venue:{venueId}:sections` are invalidated.

## Scenario 4: Delete Venue Without Shows

```powershell
Invoke-WebRequest `
  -Method Delete `
  -Uri "http://localhost:8080/api/v1/venues/$venueId" `
  -Headers @{ Authorization = "Bearer $token" }
```

Expected:

- HTTP 204.
- Child sections are removed by database cascade.
- If any `catalog.show` references the venue, response is HTTP 409 Problem Details.

## Automated Test Expectations

Run from `microservice-catalog`:

```powershell
.\mvnw.cmd test
```

Required coverage:

- Unit tests for blank names, positive section geometry and command models excluding `capacity`.
- REST contract tests for 201, 200, 204, 400, 401/403, 404 and 409.
- PostgreSQL integration test proving generated `capacity` equals `number_of_rows * row_capacity`.
- Redis integration test proving cache invalidation on Venue/Section write and delete.
- E2E P1 flow: create Venue with `ROLE_ADMIN`, read it publicly, reject duplicate name.

## Known Release Risk

Inventory propagation events for `SectionDeleted`, `SectionUpdated` capacity changes and `VenueDeleted` are not specified in this feature. Do not claim cross-service inventory reconciliation complete until an ADR and event contract are approved.
