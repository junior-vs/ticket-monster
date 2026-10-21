# GitOps contract

The shared infrastructure in this repository is currently expressed as Docker Compose for local development. Production GitOps must apply equivalent Kubernetes/OpenShift manifests in this order:

1. Namespace and network policies.
2. External Secrets or Vault SecretStore definitions.
3. PostgreSQL operator resources for `catalog_db`, `inventory_db`, `booking_db`, and `telemetry_db`.
4. Redis Sentinel or Redis Cluster resources.
5. Keycloak realm import for `ticketmonster`.
6. Kafka broker, Schema Registry, topics, ACLs, and DLQs.
7. OpenTelemetry Collector, Prometheus, Loki, Tempo, and Grafana provisioning.
8. Microservice workloads, starting with `microservice-catalog`.

ArgoCD applications should use sync waves matching that order so services are deployed only after their infrastructure and secrets are available.
