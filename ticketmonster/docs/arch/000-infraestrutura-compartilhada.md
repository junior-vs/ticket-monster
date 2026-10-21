# Feature Specification: Infraestrutura Compartilhada da Plataforma (Keycloak, PostgreSQL, Redis, Kafka, Observabilidade)

**Feature Branch**: `000-infraestrutura-compartilhada`
**Created**: 2026-07-26
**Status**: Draft
**Input**: User description: "baseado no doc arquitetura-solucao.md, gerar especificação incluindo tasks necessárias para configurações de infra como keycloak, redis, postgresql (comum aos módulos), observabilidade (grafana, graylog, prometheus)"

**Origem**: `arquitetura-solucao.md` — seções 6.1 (Camadas), 7 (Stack Quarkus), 9 (Banco de Dados), 10 (Redis), 11 (Kafka/Outbox), 14 (Observabilidade), 15 (Segurança/Keycloak), 16 (Cloud First/K8s), 22 (Roadmap — Quick Wins).

## Divergência identificada e correção aplicada

O input desta spec solicita **Graylog** como componente de observabilidade. `arquitetura-solucao.md` seção 14 define explicitamente a stack **LGTM (Grafana, Loki, Tempo, Mimir)** integrada a **OpenTelemetry**, sem qualquer menção a Graylog. Não há ADR que substitua LGTM por Graylog. Esta spec segue a stack documentada (Grafana + Prometheus + Loki + Tempo) como linha de base, e trata Graylog como pedido não coberto pela arquitetura de referência — se for uma necessidade real (ex.: Graylog para logs estruturados em vez de Loki), recomenda-se abrir um ADR complementar antes de incluir na infraestrutura, em vez de decidir isso implicitamente nesta spec.

## Escopo

Infraestrutura transversal, não pertencente a um único microsserviço, consumida por todos os quatro serviços (`catalog`, `inventory`, `booking`, `telemetry`): identidade (Keycloak), bancos de dados por serviço (PostgreSQL, *database per service*), cache/lock distribuído (Redis), mensageria (Kafka — incluída por ser pré-requisito de Outbox/Saga usados por `booking`/`inventory`/`telemetry`), e observabilidade (OpenTelemetry Collector, Prometheus, Grafana, Loki, Tempo).

Fora de escopo: lógica de negócio de cada microsserviço (coberta pelas specs US-CAT-*, US-INV-*, US-BOOK-*, US-TEL-*); provisionamento de Kubernetes/OpenShift em si (seção 16 trata como decisão de plataforma, não detalhada em manifests nesta spec).

## Dependência com as demais specs

Todas as specs de US já produzidas (US-CAT-01 a US-CAT-14, e futuras de `inventory`/`booking`/`telemetry`) assumem, sem detalhar, a existência de: Postgres acessível por schema próprio, Redis para cache-aside/locks, Keycloak emitindo JWT validável via JWKS, e OpenTelemetry coletando traces/métricas. Esta spec é pré-requisito de infraestrutura (Fase -1) para todas as fases funcionais já mapeadas na análise de dependências do `microservice-catalog`.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Provisionar PostgreSQL por Serviço (Database per Service) (Priority: P1)

Como Engenheiro de Plataforma, quero provisionar uma instância/schema PostgreSQL isolado por microsserviço, para que cada serviço tenha autoridade exclusiva sobre seu próprio schema, sem acoplamento físico entre bancos (ADR 03).

**Why this priority**: Nenhuma feature funcional de nenhum microsserviço pode ser implementada ou testada via Testcontainers sem essa base — é o pré-requisito mais básico de todas as specs já produzidas.

**Independent Test**: Subir os 4 databases (`catalog_db`, `inventory_db`, `booking_db`, `telemetry_db`) via Docker Compose local ou Testcontainers e confirmar que cada schema aplica sua própria migração Liquibase sem erro e sem visibilidade cruzada entre bancos.

**Acceptance Scenarios**:

1. **Given** o ambiente de desenvolvimento local, **When** o Docker Compose é executado, **Then** quatro instâncias/databases PostgreSQL distintos (`catalog_db`, `inventory_db`, `booking_db`, `telemetry_db`) sobem isolados, cada um com seu próprio usuário/credencial.
2. **Given** as migrações Liquibase de cada serviço, **When** aplicadas na inicialização, **Then** cada schema é criado (`catalog`, `inventory`, `booking`, `telemetry`) com as extensões necessárias (`pgcrypto` para UUID via `gen_random_uuid()`).
3. **Given** um serviço tentando referenciar uma tabela de outro banco via FK, **When** a migração é aplicada, **Then** a tentativa MUST falhar — reforça a ausência intencional de FK cross-serviço (ADR 03, seção 9).

---

### User Story 2 - Provisionar Redis Compartilhado com Segregação Lógica por Uso (Priority: P1)

Como Engenheiro de Plataforma, quero provisionar Redis com pools/databases lógicos segregados por finalidade (cache-aside de leitura vs. locks de assento vs. estado do Bot), para evitar que um pico de contenção de lock esgote conexões usadas por cache de leitura (Bulkhead, seção 20.6).

**Why this priority**: Redis é dependência transversal de `catalog` (cache-aside), `inventory` (locks de assento, RN22 to-be) e `telemetry` (estado do Bot, buffer de log) — sem ele, nenhuma das specs de leitura pública (US-CAT-01, 04, 05) atinge as metas de latência propostas.

**Independent Test**: Subir Redis via Docker Compose/Testcontainers e validar, por serviço, que a chave de cache correspondente (`catalog:event:{id}`, `lock:seat:{perfId}:{secId}:{row}:{num}`, `bot:status`) é gravável e lida dentro do TTL esperado.

**Acceptance Scenarios**:

1. **Given** o Redis provisionado, **When** o `microservice-catalog` grava uma chave de cache-aside, **Then** a chave expira dentro do TTL configurado (ex.: 1 hora para catálogo, conforme proposto nas specs US-CAT-04/05).
2. **Given** o Redis provisionado, **When** o `microservice-inventory` executa `SET NX PX 60000` para um lock de assento, **Then** o lock expira automaticamente em exatos 60.000 ms sem intervenção manual (RN23, CA-INV-01-LOK).
3. **Given** picos de contenção em locks de assento, **When** o pool de conexões é avaliado, **Then** o pool dedicado a locks de assento é isolado do pool dedicado a cache de catálogo/leitura (Bulkhead — seção 20.6), evitando que um exaustão de conexões de lock afete o catálogo público.
4. **Given** falha temporária do Redis, **When** qualquer serviço consumidor tenta uma leitura de cache, **Then** o serviço realiza fallback transparente para o PostgreSQL sem erro 500 (padrão já assumido nas specs US-CAT-01/04/05).

---

### User Story 3 - Provisionar Keycloak como Autoridade de Identidade (Priority: P1)

Como Engenheiro de Plataforma, quero provisionar o Keycloak com o realm `ticketmonster`, seus clients e roles conforme especificado (seção 15.1), para que todos os microsserviços possam validar JWT localmente via JWKS, sem introspecção síncrona a cada requisição.

**Why this priority**: Toda spec administrativa já produzida (US-CAT-09, US-CAT-10, US-CAT-12, US-CAT-14) depende de `ROLE_ADMIN` validável — sem Keycloak configurado, nenhum endpoint de escrita pode ser testado ponta a ponta.

**Independent Test**: Subir Keycloak via container, importar a configuração de realm (clients + roles) via script/realm-export, obter um token via Client Credentials para um client de serviço (ex.: `catalog-service`) e validar a assinatura via JWKS a partir de um serviço Quarkus de teste.

**Acceptance Scenarios**:

1. **Given** o Keycloak provisionado, **When** o realm `ticketmonster` é importado, **Then** os clients `ticketmonster-web`, `ticketmonster-admin`, `ticketmonster-gateway` (público/confidencial conforme seção 15.1) e os clients de serviço (`catalog-service`, `inventory-service`, `booking-service`, `telemetry-service`, Client Credentials) existem e estão habilitados.
2. **Given** o realm importado, **When** as realm roles são inspecionadas, **Then** `ROLE_CUSTOMER`, `ROLE_ADMIN` e `ROLE_SERVICE` existem conforme seção 15.1.
3. **Given** um client de serviço autenticado via Client Credentials, **When** um microsserviço Quarkus valida o token recebido, **Then** a validação ocorre localmente via JWKS em cache (`quarkus-oidc`, `bearer-only`), sem chamada de introspecção síncrona ao Keycloak por requisição (ADR 06 — Token Relay).
4. **Given** um administrador autenticando via `ticketmonster-admin`, **When** o fluxo Authorization Code + PKCE é executado, **Then** o token retornado contém `realm_access.roles` incluindo `ROLE_ADMIN`.

---

### User Story 4 - Provisionar Kafka para Suporte a Outbox e Saga Coreografada (Priority: P2)

Como Engenheiro de Plataforma, quero provisionar Kafka com os tópicos de domínio necessários para a Saga coreografada de checkout e para a trilha de auditoria, para que `booking`, `inventory` e `telemetry` possam publicar/consumir eventos de forma assíncrona (ADR 04, ADR 05).

**Why this priority**: Sem Kafka, nenhuma feature de `microservice-booking`/`microservice-inventory` além de operações CRUD locais pode ser implementada — é pré-requisito da Saga de checkout, ainda não especificada em spec de US própria.

**Independent Test**: Subir Kafka via Testcontainers, publicar um evento de teste em um tópico de domínio (ex.: `booking-events`) e confirmar consumo por um consumer de teste com o schema esperado.

**Acceptance Scenarios**:

1. **Given** o Kafka provisionado, **When** os tópicos de domínio são criados (`booking-events` — `BookingInitiatedEvent`/`BookingConfirmedEvent`/`BookingCancelledEvent`/`BookingFailedEvent`; tópico de resultado de alocação conforme nomenclatura fixada em ADR 07 — `SeatsReservedEvent`/`SeatsReservationFailedEvent`), **Then** cada tópico existe com a configuração de partições/replicação definida para o ambiente.
2. **Given** o mecanismo de segurança de mensageria (seção 15.4.3), **When** um serviço não autorizado tenta publicar em `booking-events`, **Then** a ACL do tópico impede a publicação — apenas `booking` publica; apenas `inventory`/`telemetry` consomem.
3. **Given** o Outbox Pattern (`booking.outbox_event`), **When** o relay do Outbox executa, **Then** eventos com `published_at IS NULL` são publicados no Kafka e marcados como publicados após ACK do broker.

---

### User Story 5 - Provisionar Stack de Observabilidade (OpenTelemetry, Prometheus, Grafana, Loki, Tempo) (Priority: P2)

Como Engenheiro de Plataforma, quero provisionar a stack de observabilidade definida na arquitetura de referência (OpenTelemetry Collector + Prometheus + Grafana + Loki + Tempo), para correlacionar traces, métricas e logs de uma compra ponta a ponta entre os 4 microsserviços.

**Why this priority**: Todas as metas de performance propostas nas specs já produzidas (p95 de latência, taxa de erro 5xx) dependem de instrumentação real para serem validadas — sem isso, as metas de PR-00x permanecem não verificáveis.

**Independent Test**: Subir a stack via Docker Compose, instrumentar um serviço de teste com `quarkus-opentelemetry` + `quarkus-micrometer-registry-prometheus`, disparar uma requisição HTTP e confirmar que o trace aparece correlacionado no Grafana/Tempo e a métrica correspondente no Prometheus.

**Acceptance Scenarios**:

1. **Given** os quatro microsserviços instrumentados, **When** uma requisição HTTP atravessa o API Gateway e ao menos dois serviços, **Then** o `traceparent` (W3C) é propagado em toda a cadeia HTTP e nos metadados do Kafka, permitindo reconstrução do trace completo no Tempo/Grafana.
2. **Given** o Prometheus configurado, **When** os health checks (`/q/health/live`, `/q/health/ready`) de cada serviço são consultados, **Then** as métricas de ingressos reservados/segundo, taxa de falha de alocação, latência de transações reativas e tamanho de filas Kafka estão disponíveis para scraping.
3. **Given** o Loki configurado como agregador de logs, **When** um serviço emite log estruturado, **Then** o log é correlacionável ao `trace_id` da requisição correspondente.
4. **Given** dashboards no Grafana, **When** um operador consulta o painel de saúde da plataforma, **Then** métricas de negócio (seção 14) e infraestrutura (CPU/memória/conexões) estão disponíveis em um único painel correlacionado.

> Nota: se Graylog for de fato requerido (ver "Divergência identificada"), este US5 precisa ser revisto após decisão formal — Graylog e Loki não são adotados simultaneamente sem justificativa, para evitar duplicidade de pipeline de logs.

---

### Edge Cases

- **Falha de conexão do Keycloak durante inicialização de um microsserviço**: o serviço deve falhar o health check `/q/health/ready` (não aceitar tráfego) até que o JWKS esteja acessível, evitando aceitar requisições sem conseguir validar tokens.
- **Rotação de credenciais de banco/Redis/Kafka**: credenciais devem ser injetadas via Kubernetes Secrets integrados ao HashiCorp Vault (seção 15.6), nunca em `application.properties` versionado.
- **Indisponibilidade total do Kafka**: `booking` continua aceitando `POST /bookings` e gravando localmente (`PENDING` + Outbox), mas o relay não publica até o broker voltar — a Saga permanece pendente sem perda de dado (garantia at-least-once do Outbox).
- **Colisão de nome de schema entre serviços**: cada serviço usa schema PostgreSQL próprio (`catalog`, `inventory`, `booking`, `telemetry`) dentro de bancos fisicamente segregados (`*_db`), eliminando colisão por design (ADR 03).

### User Experience Consistency *(mandatory)*

- **Infraestrutura como código**: toda configuração (realm Keycloak, tópicos Kafka, migrações Liquibase, dashboards Grafana) deve ser versionada e aplicável via GitOps/ArgoCD (seção 24 — Recomendações Finais).
- **12-Factor**: configuração via variáveis de ambiente/ConfigMaps, sem estado em disco local (seção 16).
- **TLS obrigatório**: comunicação entre containers via mTLS (Service Mesh) mesmo com JWT (seção 15.6) — o JWT prova identidade do usuário final, não do serviço chamador.

## Requirements *(mandatory)*

### Functional Requirements — PostgreSQL

- **FR-PG-001**: O ambiente MUST provisionar 4 databases PostgreSQL isolados (`catalog_db`, `inventory_db`, `booking_db`, `telemetry_db`), cada um com schema e credencial próprios (ADR 03).
- **FR-PG-002**: Cada database MUST habilitar a extensão `pgcrypto` para suporte a `gen_random_uuid()` como estratégia de chave primária.
- **FR-PG-003**: Migrações de schema MUST ser aplicadas via Liquibase, uma por serviço, sem overlap de responsabilidade entre bancos.
- **FR-PG-004**: Nenhuma migração MUST criar FK apontando para tabela de outro `*_db` — referências cross-serviço são sempre lógicas (UUID sem constraint), conforme já documentado em cada spec de microsserviço.

### Functional Requirements — Redis

- **FR-RD-001**: O ambiente MUST provisionar Redis acessível pelos 4 microsserviços, com segregação lógica de uso: cache-aside de leitura (`catalog`), locks de assento (`inventory`), estado/log do Bot (`telemetry`).
- **FR-RD-002**: A configuração MUST prever pool de conexões dedicado para locks de assento, isolado do pool de cache de leitura (Bulkhead, seção 20.6), para produção — em ambiente local/dev um único Redis é aceitável, com pools lógicos diferenciados na aplicação.
- **FR-RD-003**: Em produção, o Redis MUST operar em modo de alta disponibilidade (Sentinel ou Redis Cluster) com replicação automática (Risco 3, seção 23).
- **FR-RD-004**: Todo consumidor de cache MUST implementar fallback para o banco relacional em caso de indisponibilidade do Redis, sem retornar 500.

### Functional Requirements — Keycloak

- **FR-KC-001**: O ambiente MUST provisionar Keycloak com o realm `ticketmonster`.
- **FR-KC-002**: O realm MUST conter os clients `ticketmonster-web` (público, Authorization Code + PKCE), `ticketmonster-admin` (público, Authorization Code + PKCE), `ticketmonster-gateway` (confidencial, bearer-only), e um client Client Credentials por serviço (`catalog-service`, `inventory-service`, `booking-service`, `telemetry-service`).
- **FR-KC-003**: O realm MUST conter as roles `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_SERVICE`.
- **FR-KC-004**: Cada microsserviço MUST validar JWT localmente via JWKS em cache (`quarkus-oidc`, `bearer-only`), sem introspecção síncrona por requisição (ADR 06).
- **FR-KC-005**: MFA MUST ser obrigatório para o realm/role de administrador (seção 15.6).
- **FR-KC-006**: A configuração de realm (clients, roles) MUST ser versionada como artefato de infraestrutura (realm-export JSON ou Terraform provider Keycloak), não configurada manualmente em produção.

### Functional Requirements — Kafka

- **FR-KF-001**: O ambiente MUST provisionar Kafka com os tópicos de domínio necessários para a Saga coreografada (`booking-events` e o tópico de resultado de alocação de assentos, nomenclatura `SeatsReservedEvent`/`SeatsReservationFailedEvent` conforme ADR 07).
- **FR-KF-002**: ACLs de tópico MUST restringir publicação a `booking` (eventos de booking) e a `inventory` (eventos de alocação), com consumo restrito aos serviços autorizados (seção 15.4.3).
- **FR-KF-003**: A comunicação com o broker MUST usar SASL/SCRAM ou mTLS (seção 15.4.3).
- **FR-KF-004**: Um Schema Registry (Avro/JSON Schema) SHOULD ser provisionado para evitar quebra de consumidor por mudança de payload de evento (seção 19 — Melhorias de Mercado).
- **FR-KF-005**: O ambiente MUST prever Dead Letter Queue para consumidores Kafka, isolando mensagens que falham repetidamente sem travar partição (seção 20.4).

### Functional Requirements — Observabilidade

- **FR-OB-001**: O ambiente MUST provisionar OpenTelemetry Collector para receber traces/métricas/logs de todos os microsserviços.
- **FR-OB-002**: O ambiente MUST provisionar Prometheus para scraping de métricas via `quarkus-micrometer-registry-prometheus`.
- **FR-OB-003**: O ambiente MUST provisionar Grafana para dashboards correlacionando métricas de negócio (ingressos reservados/segundo, taxa de falha de alocação, latência de transações reativas, tamanho de filas Kafka) e infraestrutura.
- **FR-OB-004**: O ambiente MUST provisionar Tempo (ou Jaeger, conforme diagrama da seção 18.1) para tracing distribuído correlacionado por `traceparent` (W3C).
- **FR-OB-005**: O ambiente MUST provisionar Loki para agregação de logs correlacionáveis por `trace_id` — linha de base documentada (seção 14); ver "Divergência identificada" quanto ao pedido de Graylog.
- **FR-OB-006**: Cada microsserviço MUST expor `/q/health/live` e `/q/health/ready` (SmallRye Health) para uso por Kubernetes e por scraping do Prometheus.

### Key Entities *(include if feature involves data)*

- **RealmConfig (Keycloak)**: definição de realm, clients, roles — artefato de infraestrutura versionado.
- **TopicConfig (Kafka)**: definição de tópicos, partições, ACLs — artefato de infraestrutura versionado.
- **DatabaseInstance (PostgreSQL)**: um por microsserviço, com schema e credencial isolados.
- **CacheNamespace (Redis)**: segregação lógica de uso (cache-aside, locks, estado do Bot) dentro da instância Redis compartilhada.
- **ObservabilityStack**: OpenTelemetry Collector + Prometheus + Grafana + Loki + Tempo, compartilhados por todos os serviços via instrumentação padrão.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos 4 microsserviços conseguem inicializar localmente (Docker Compose) com Postgres, Redis, Keycloak e Kafka provisionados por esta spec, sem configuração manual adicional.
- **SC-002**: 100% dos endpoints administrativos das specs já produzidas (US-CAT-09, 10, 12, 14) conseguem validar `ROLE_ADMIN` via token emitido pelo Keycloak provisionado por esta spec.
- **SC-003**: 100% dos traces de uma requisição que atravessa 2+ microsserviços são reconstituíveis no Grafana/Tempo via `traceparent` comum.
- **SC-004**: Zero FKs cross-database em qualquer migração Liquibase aplicada.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: O tempo de inicialização completo do ambiente local (Docker Compose, todos os componentes desta spec) SHOULD ser <= 3 minutos — meta proposta, não confirmada em `arquitetura-solucao.md`.
- **PR-002**: O Redis em produção MUST se recuperar automaticamente de falha de nó via Sentinel/Cluster sem intervenção manual (Risco 3, seção 23).
- **PR-003**: A indisponibilidade do Kafka MUST NOT impedir a criação local de `Booking` (`PENDING` + Outbox); apenas atrasa a publicação de eventos.

## Riscos

- **Graylog não documentado na arquitetura de referência**: ver "Divergência identificada". Adotar Graylog junto com Loki duplicaria pipeline de logs sem decisão registrada — recomenda-se ADR complementar antes de incluir Graylog no escopo de infraestrutura.
- **Ausência de evento de propagação `Section`/`Venue` para `inventory`** (já identificado na spec de US-CAT-10): esta spec de infraestrutura provisiona o Kafka e os tópicos já nomeados nos documentos, mas não resolve a lacuna de definição desses eventos adicionais — permanece como pendência de modelagem, não de infraestrutura.
- **Segredos em ambiente local vs. produção**: esta spec assume Vault + Kubernetes Secrets em produção (seção 15.6); ambiente de desenvolvimento local (Docker Compose) usará variáveis de ambiente simples — divergência aceitável, mas deve ser explicitada em documentação de setup para não ser copiada para produção por engano.

## Assumptions

- Ambiente de desenvolvimento local usa Docker Compose; ambiente de produção usa Kubernetes/OpenShift com GitOps (ArgoCD), conforme seção 16 e Recomendações Finais (seção 24) — provisionamento detalhado de manifests K8s está fora do escopo desta spec.
- A stack de observabilidade segue LGTM (Grafana, Loki, Tempo, Mimir/Prometheus) conforme seção 14; Graylog não é adotado até decisão formal complementar.
- Metas de tempo de inicialização (PR-001) são propostas desta spec, não confirmadas em `arquitetura-solucao.md`.
- Provisionamento de Vault é assumido disponível na plataforma-alvo; não é detalhado nesta spec além da referência da seção 15.6.

## Ordenação de Tasks (Fase -1 — pré-requisito de todas as fases funcionais)

| # | Task | Depende de | Origem |
|---|---|---|---|
| 1 | Provisionar PostgreSQL: 4 databases isolados + extensão `pgcrypto` + Liquibase por serviço | — | FR-PG-001 a 004; ADR 03 |
| 2 | Provisionar Redis: instância única (dev) com pools lógicos segregados; HA via Sentinel/Cluster (prod) | — | FR-RD-001 a 004; seção 10, 20.6, Risco 3 |
| 3 | Provisionar Keycloak: realm, clients, roles, MFA para admin | — | FR-KC-001 a 006; seção 15.1 |
| 4 | Provisionar Kafka: tópicos, ACLs, SASL/mTLS, Schema Registry, DLQ | — | FR-KF-001 a 005; seção 11, 15.4.3, 19, 20.4 |
| 5 | Provisionar Observabilidade: OpenTelemetry Collector, Prometheus, Grafana, Tempo, Loki | Idealmente após 1–4 (para ter o que observar), mas pode ser paralelo | FR-OB-001 a 006; seção 14 |
| 6 | Health checks (`/q/health/live`, `/q/health/ready`) por serviço | Depende do serviço já ter Postgres/Redis/Kafka configurados (checks reais de conectividade) | FR-OB-006; seção 14 |
| 7 | Vault + Kubernetes Secrets para credenciais de todos os componentes acima | Pode ser paralelo a 1–4, mas bloqueia deploy em produção | seção 15.6 |
| 8 | GitOps (ArgoCD) para aplicar 1–7 de forma consistente entre ambientes | Depende de 1–7 estarem versionados como código | seção 24 |

As tasks 1–4 são paralelizáveis entre si (sem dependência cruzada). A task 5 se beneficia de 1–4 existirem para ter sinal real a observar, mas pode ser desenvolvida em paralelo com dashboards vazios. As tasks 6–8 fecham o ciclo de produção-ready, mas não bloqueiam o desenvolvimento funcional local das specs de US já produzidas.

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-PG-001 a 004 | `arquitetura-solucao.md` seção 9, ADR 03 | Database per Service |
| FR-RD-001 a 004 | seção 10, seção 20.6 (Bulkhead), Risco 3 (seção 23) | Redis segregado por uso |
| FR-KC-001 a 006 | seção 15.1, 15.6, ADR 06 | Keycloak/realm/roles/MFA |
| FR-KF-001 a 005 | seção 11, 15.4.3, 19, 20.4, ADR 04/07 | Kafka/Outbox/Saga |
| FR-OB-001 a 006 | seção 14 | Stack LGTM + OpenTelemetry |
| Graylog | Input do usuário — sem lastro em `arquitetura-solucao.md` | Divergência declarada; ADR complementar recomendado |