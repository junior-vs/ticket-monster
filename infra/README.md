# TicketMonster shared infrastructure

This folder contains the shared platform infrastructure used by all TicketMonster microservices.

## Local development

Start the shared stack from the repository root:

```shell
docker compose -f docker-compose.shared.yml up -d
```

Shared services:

- PostgreSQL: `localhost:5432`, with `catalog_db`, `inventory_db`, `booking_db`, and `telemetry_db`
- Redis: `localhost:6379`
- Keycloak: `http://localhost:8081`, realm `ticketmonster`, local admin `admin/admin`
- Kafka: `localhost:9092`, topics `catalog-events`, `booking-events`, `seat-reservation-events`, and DLQs
- Grafana: `http://localhost:3000`, local admin `admin/admin`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100`
- Tempo: `http://localhost:3200`

Local credentials are intentionally simple and must not be reused outside development.

## Production boundary

The architecture requires Kubernetes/OpenShift, Vault-backed secrets, encrypted service-to-service traffic, and GitOps.

- Store database, Redis, Kafka, and OIDC secrets in Vault and expose them to workloads through Kubernetes Secrets or an External Secrets operator.
- Replace local Kafka PLAINTEXT with SASL/SCRAM or mTLS and topic ACLs.
- Replace single-node Redis with Sentinel or Redis Cluster.
- Apply these versioned files through ArgoCD after environment-specific values are moved to sealed/external secrets.

See `kubernetes/secrets-contract.md` and `gitops/argocd-contract.md` for the production handoff contract.
