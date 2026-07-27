# Secrets contract

Production deployments must source these values from Vault and expose them to workloads as Kubernetes Secrets. The local Docker Compose values are development-only defaults.

| Secret key | Consumer | Local dev value |
|---|---|---|
| `CATALOG_DB_USER` | `microservice-catalog` | `catalog_user` |
| `CATALOG_DB_PASSWORD` | `microservice-catalog` | `catalog_password` |
| `INVENTORY_DB_USER` | `microservice-inventory` | `inventory_user` |
| `INVENTORY_DB_PASSWORD` | `microservice-inventory` | `inventory_password` |
| `BOOKING_DB_USER` | `microservice-booking` | `booking_user` |
| `BOOKING_DB_PASSWORD` | `microservice-booking` | `booking_password` |
| `TELEMETRY_DB_USER` | `microservice-telemetry` | `telemetry_user` |
| `TELEMETRY_DB_PASSWORD` | `microservice-telemetry` | `telemetry_password` |
| `OIDC_CLIENT_SECRET` | each service | per-client `*-service-secret` |
| `KAFKA_SASL_USERNAME` | Kafka producers/consumers | not enabled locally |
| `KAFKA_SASL_PASSWORD` | Kafka producers/consumers | not enabled locally |

Environment-specific non-secret endpoints should come from ConfigMaps:

- `CATALOG_JDBC_URL`
- `CATALOG_REACTIVE_URL`
- `CATALOG_REDIS_URL`
- `OIDC_AUTH_SERVER_URL`
- `KAFKA_BOOTSTRAP_SERVERS`
- `OTEL_EXPORTER_OTLP_ENDPOINT`
