# Proposta de Arquitetura de Referência — TicketMonster Modernizado

Este documento detalha a proposta de arquitetura de referência para a modernização do **TicketMonster**, atendendo à arquitetura-alvo definida (Microsserviços, Quarkus 3.27+, PostgreSQL, Redis, Cloud First, Observabilidade via OpenTelemetry, Clean Architecture, DDD, APIs REST, Sistema Reativo, Programação Funcional integrada com OO). Complementa `modernization_architecture.md` e `microservices_specification.md`, detalhando o *como* e o *porquê* de cada escolha técnica, além de um catálogo de design patterns recomendados por cenário.

---

## 1. Visão Geral

A arquitetura mantém o particionamento por *bounded context* já estabelecido em `microservices_specification.md` (`catalog`, `inventory`, `booking`, `telemetry`), cada um com Clean Architecture interna, DDD tático, banco PostgreSQL próprio (**Database per Service**), e comunicação híbrida: REST reativo síncrono para consultas e Kafka assíncrono para eventos de domínio e orquestração de Saga.

```mermaid
flowchart TB
    subgraph Clientes
        WEB[SPA Pública]
        ADM[Painel Admin]
        MOB[App Mobile]
    end

    subgraph Edge["Edge / API Gateway"]
        GW[API Gateway / BFF - Quarkus Reativo<br/>OIDC, Rate Limiting, Roteamento]
    end

    WEB --> GW
    ADM --> GW
    MOB --> GW

    subgraph Services["Microsserviços (Quarkus)"]
        CAT[microservice-catalog]
        INV[microservice-inventory]
        BOOK[microservice-booking]
        TEL[microservice-telemetry]
    end

    GW --> CAT
    GW --> INV
    GW --> BOOK
    GW --> TEL

    subgraph Data["Camada de Dados"]
        CATDB[(catalog_db<br/>PostgreSQL)]
        INVDB[(inventory_db<br/>PostgreSQL)]
        BOOKDB[(booking_db<br/>PostgreSQL)]
        REDIS[(Redis<br/>Locks / Cache / Estado Bot)]
    end

    CAT --> CATDB
    INV --> INVDB
    BOOK --> BOOKDB
    INV --> REDIS
    CAT -.cache-aside.-> REDIS
    TEL --> REDIS

    subgraph Backbone["Event Backbone"]
        KAFKA[/Kafka/]
    end

    BOOK <-- Saga / Domain Events --> KAFKA
    INV <-- Domain Events --> KAFKA
    KAFKA --> TEL

    subgraph Obs["Observabilidade"]
        OTEL[OpenTelemetry Collector]
        PROM[Prometheus]
        GRAF[Grafana]
        JAEG[Jaeger / Tempo]
    end

    CAT -.traces/metrics/logs.-> OTEL
    INV -.traces/metrics/logs.-> OTEL
    BOOK -.traces/metrics/logs.-> OTEL
    TEL -.traces/metrics/logs.-> OTEL
    OTEL --> PROM
    OTEL --> JAEG
    PROM --> GRAF
```

---

## 2. Como cada princípio da arquitetura-alvo é atendido

### 2.1 Arquitetura de Microsserviços
Corte por *bounded context* de negócio (catálogo, estoque de assentos, vendas, telemetria) — não por entidade CRUD, evitando repetir o erro do legado de expor CRUDs genéricos duplicados (`rest/bookings` vs `rest/forge/bookings`, ver `projeto.md` seção 14). Cada serviço possui seu próprio schema de banco, eliminando o acoplamento hoje existente implicitamente via `EntityManager` compartilhado no monólito.

### 2.2 Quarkus 3.27+
Escolhido pelo tempo de inicialização (~50ms em *native image*) e footprint de memória reduzido, essenciais para o Horizontal Pod Autoscaler reagir rapidamente a picos de venda — justamente o cenário que mais estressa o legado hoje, dado o lock pessimista por seção inteira.

Extensões-chave:
- `quarkus-rest` + `quarkus-rest-jackson` (RESTEasy Reactive)
- `quarkus-hibernate-reactive-panache` (acesso a dados não bloqueante)
- `quarkus-redis-client`
- `quarkus-smallrye-reactive-messaging-kafka`
- `quarkus-opentelemetry` + `quarkus-micrometer-registry-prometheus`
- `quarkus-oidc` (integração Keycloak)
- `quarkus-smallrye-fault-tolerance` (Circuit Breaker, Retry, Timeout, Bulkhead)
- `quarkus-smallrye-health`

### 2.3 PostgreSQL
Uma instância/schema lógico por serviço. Acesso via **Hibernate Reactive com Panache**, não bloqueante — preserva a produtividade do JPA sem bloquear threads do event loop, resolvendo o problema estrutural do legado, onde toda query JPA bloqueava a thread da requisição.

### 2.4 Redis
Usado para três finalidades distintas, deliberadamente **não** tratadas como um único "cache genérico":
1. **Lock distribuído por assento** (`SET lock:seat:{perfId}:{secId}:{row}:{num} <bookingId> NX PX 60000`) — substitui o `@Lob`/lock pessimista por seção inteira do legado.
2. **Cache-aside** de leitura pesada do catálogo (eventos, shows, disponibilidade agregada).
3. **Estado efêmero de telemetria** (status RUNNING/STOPPED do Bot, buffer circular de log).

### 2.5 Cloud First
- Containers com imagem *native* GraalVM.
- Configuração via variáveis de ambiente / ConfigMaps (12-Factor), sem estado em disco local — elimina o cache em arquivo (`tmpDir`) do `MediaManager` legado, substituído por object storage (S3/MinIO).
- Health checks (`/q/health/live`, `/q/health/ready`) para probes do Kubernetes.
- HPA reagindo a RPS e a *consumer lag* do Kafka (via KEDA).

### 2.6 Observabilidade (OpenTelemetry)
- Instrumentação automática via `quarkus-opentelemetry`, com propagação de contexto W3C Trace Context de ponta a ponta — inclusive através dos headers de mensagem do Kafka — permitindo rastrear uma única compra desde o clique no front até a confirmação assíncrona no `inventory`.
- Logs estruturados em JSON correlacionados por `traceId`/`spanId`, facilitando localizar todos os logs de uma Saga específica.
- Métricas de negócio customizadas via Micrometer (`booking.created.count`, `seat.lock.contention`, `bot.requests.total`), além das técnicas (latência, throughput, taxa de erro).

### 2.7 Clean Architecture + DDD + REST
Cada serviço segue quatro camadas com regra de dependência única direção (para dentro):

```mermaid
flowchart LR
    subgraph adapterIn["adapter-in"]
        REST[REST Resources]
        KCONS[Kafka Consumers]
    end
    subgraph application["application"]
        UC[Use Cases / Application Services]
        PORTS[Portas de saída - interfaces]
    end
    subgraph domain["domain"]
        AGG[Agregados / Entidades]
        VO[Value Objects]
        DEVT[Domain Events]
        SPEC[Specifications]
    end
    subgraph adapterOut["adapter-out"]
        REPO[Repositórios Panache]
        REDISC[Redis Client]
        KPUB[Kafka Publisher]
    end

    REST --> UC
    KCONS --> UC
    UC --> AGG
    UC --> PORTS
    PORTS -.implementada por.-> REPO
    PORTS -.implementada por.-> REDISC
    PORTS -.implementada por.-> KPUB
```

`domain` não conhece Quarkus, JPA ou Kafka. Isso resolve o principal problema estrutural do legado, onde a regra de negócio (`SectionAllocation.allocateSeats`) está emaranhada com anotações JPA na mesma classe.

### 2.8 Sistema Reativo
Todo I/O (banco, Redis, Kafka, chamadas entre serviços) é feito via **Mutiny** (`Uni<T>` / `Multi<T>`), não bloqueante fim a fim. Substitui o modelo síncrono-bloqueante do legado (EJB `@Stateless` + JPA bloqueante), origem do gargalo de concorrência identificado no lock pessimista por seção.

### 2.9 Programação Funcional integrada com Orientação a Objeto
- **Domínio rico e imutável, orientado a objetos:** agregados como `Booking` e `SeatMap` continuam sendo objetos com comportamento (não *anemic model*), mas Value Objects (`Seat`, `Money`, `Email`, `CancellationCode`) são modelados como **Java Records**, imutáveis por padrão.
- **Composição funcional para orquestração:** o pipeline de criação de reserva é modelado como cadeia de transformações puras usando `Either<Error, T>` (Vavr) ou os operadores de `Uni<T>` (`.chain()`, `.onFailure()`), em vez do `try/catch` aninhado do legado (`BookingService.createBooking`, hoje um método de mais de 80 linhas misturando validação, alocação e persistência).
- **Funções puras para regras de cálculo**, testáveis isoladamente sem mocks de banco — por exemplo `calculateContiguousBlock(SeatMap, quantity): Either<AllocationError, List<Seat>>`, algo inviável no legado porque a lógica de alocação está acoplada ao `EntityManager`.

---

## 3. Melhorias de mercado incluídas além do escopo original

| Melhoria | Justificativa |
|---|---|
| **API Gateway / BFF** | Centraliza rate limiting/anti-scalping e evita expor os 4 serviços diretamente aos clientes. |
| **Outbox Pattern** | Garante atomicidade entre a escrita em `booking_db` e a publicação do evento no Kafka — sem isso, uma falha entre commit e publish perde o evento silenciosamente. |
| **Saga orquestrada** (não coreografada) para o checkout | Com 3 serviços envolvidos (`booking` → `inventory` → `catalog` para preço), orquestração centralizada é mais debugável que coreografia pura, e mapeia diretamente o fluxo transacional único hoje existente em `BookingService.createBooking`. |
| **Idempotency-Key** | Cobre RN-NOVA-03 (`microservices_specification.md`, seção 5.3); essencial assim que o fluxo deixa de ser uma transação local ACID. |
| **CQRS leve no `catalog`** | Separa modelo de escrita (admin) do modelo de leitura (público, cacheado em Redis) — o legado já sofre disso implicitamente (`EventService` com predicados vs. Forge CRUD); aqui a separação é formalizada. |
| **Contract Testing (Pact)** | Evita que a mudança de contrato de um serviço quebre outro silenciosamente — risco real numa arquitetura de 4 serviços + gateway. |
| **Testcontainers** | Testes de integração reais contra Postgres/Redis/Kafka em CI, sem mocks frágeis. |
| **Feature Flags** (Unleash / OpenFeature) | Permite rollout gradual de regras críticas novas (ex.: validação de código de cancelamento) sem *big-bang*. |
| **Schema Registry** (Avro/JSON Schema no Kafka) | Evita quebra de consumidor por mudança de payload de evento — problema inexistente no legado (eventos CDI *in-process*, sem serialização). |

---

## 4. Catálogo de Design Patterns Recomendados por Cenário

### 4.1 Domínio e regras de negócio (DDD Tático)

| Pattern | Onde aplicar | Motivo |
|---|---|---|
| **Aggregate** | `Booking` (raiz, contém `Ticket`); `SeatMap`/`SectionAllocation` (raiz, contém `Seat`) | Garante invariantes (ex.: nunca dois tickets no mesmo assento) dentro de um limite transacional claro — hoje o legado só garante isso via lock de banco, não via modelo. |
| **Value Object** | `Money`, `Email`, `SeatCoordinate`, `CancellationCode` | Elimina *primitive obsession* (hoje `price` é `float` cru, `email` é `String` cru sem validação encapsulada). |
| **Domain Events** | `SeatsAllocated`, `BookingConfirmed`, `BookingCancelled` | Já usado no legado via CDI `Event<Booking>`; mantém-se o padrão, trocando apenas o transporte (CDI local → Outbox/Kafka). |
| **Specification** | Regra "assentos contíguos suficientes na seção" | Encapsula a regra de elegibilidade de alocação como objeto testável isoladamente, separando o "o quê" da regra do "como" persistir. |
| **Repository** | Uma interface por agregado (`BookingRepository`), implementação em `adapter-out` | Mantém a camada `domain` livre de Panache/Hibernate. |

### 4.2 Aplicação / Casos de Uso (Clean Architecture)

| Pattern | Onde aplicar | Motivo |
|---|---|---|
| **Use Case / Interactor** | `CreateBookingUseCase`, `CancelBookingUseCase`, `AllocateSeatsUseCase` | Um caso de uso por operação de negócio, substituindo o método monolítico `BookingService.createBooking` de hoje. |
| **Ports & Adapters (Hexagonal)** | Interfaces `SeatAllocationPort`, `PricingPort` no domínio, implementadas por clients HTTP/Kafka reais | Permite trocar o transporte de comunicação com `inventory` sem tocar a regra de negócio. |
| **CQRS** | `catalog` (modelo de leitura cacheado) e consulta/histórico em `booking` | Separa carga de leitura pesada da escrita transacional. |
| **Result / Either (Railway-Oriented Programming)** | Toda a cadeia de validação de `CreateBookingUseCase` | Substitui o `try/catch` genérico do legado por composição explícita de sucesso/falha tipada. |

### 4.3 Concorrência e Alocação de Assentos

| Pattern | Onde aplicar | Motivo |
|---|---|---|
| **Distributed Lock** (Redis `SET NX PX`) | Lock de assento individual | Substitui o `LockModeType.PESSIMISTIC_WRITE` por seção inteira, reduzindo drasticamente a contenção. |
| **Lease Pattern** | Reserva temporária de 60s do assento | O "lock" é, na prática, um aluguel com expiração automática — nomear explicitamente como tal no domínio, não apenas como "lock" genérico. |
| **Optimistic Offline Lock** (`@Version`) | Persistência final do `Booking` em `booking_db` | Mantém proteção contra concorrência na escrita definitiva, sem serializar toda a seção como hoje. |

### 4.4 Integração entre Serviços

| Pattern | Onde aplicar | Motivo |
|---|---|---|
| **Saga (Orquestração)** | Fluxo `booking` → `inventory` → confirmação | Coordena a transação distribuída de checkout, com compensação (`ReleaseSeatsCommand`) em caso de falha — equivalente ao `failedSections` do legado, porém distribuído. |
| **Outbox** | `booking_db` | Publica evento de forma atômica com a escrita local. |
| **Idempotent Receiver** | Consumidores Kafka em `inventory` e `booking` | Protege contra reprocessamento de mensagem (entrega *at-least-once* do Kafka). |
| **Dead Letter Queue** | Todos os consumidores Kafka | Isola mensagens que falham repetidamente sem travar a partição. |
| **Circuit Breaker + Retry + Timeout** (`@CircuitBreaker`, `@Retry`, `@Timeout` — SmallRye Fault Tolerance) | Chamadas síncronas `booking` → `catalog` (busca de preço) | Evita cascata de falha caso `catalog` degrade. |
| **API Gateway / BFF** | Entrada única do front público, admin e mobile | Centraliza autenticação, rate limiting e agregação de respostas. |

### 4.5 Cache e Leitura

| Pattern | Onde aplicar | Motivo |
|---|---|---|
| **Cache-Aside** | Catálogo de eventos/shows no Redis | Reduz carga no Postgres para o tráfego de navegação (majoritariamente leitura). |
| **Read-Through / TTL curto** | Disponibilidade agregada de assentos (contagem, não o mapa individual) | Evita servir dado obsoleto por muito tempo em cenário de alta demanda. |

### 4.6 Observabilidade e Resiliência Operacional

| Pattern | Onde aplicar | Motivo |
|---|---|---|
| **Correlation ID / Distributed Tracing Context Propagation** | Toda a cadeia HTTP + Kafka | Rastreia uma compra ponta a ponta entre os 4 serviços. |
| **Health Check / Readiness Probe** | Todos os serviços | Kubernetes só roteia tráfego para instâncias prontas (conexão com DB/Redis/Kafka estabelecida). |
| **Bulkhead** | Pool de conexão Redis separado para locks de assento vs. cache de catálogo | Um pico de contenção de lock não deve esgotar conexões usadas pelo cache de leitura. |

---

## 5. Segurança e Identidade (Keycloak, JWT, RBAC)

### 5.1 Visão geral da solução

O Keycloak atua como **Identity Provider (IdP)** central via protocolo **OIDC** (camada de identidade sobre OAuth2). O **API Gateway/BFF** valida o **JWT** em toda requisição de entrada; os microsserviços internos revalidam o token localmente (`quarkus-oidc`), sem depender de uma chamada síncrona ao Keycloak a cada request (validação via chave pública do realm, cacheada).

```mermaid
flowchart LR
    subgraph KC["Keycloak (Realm: ticketmonster)"]
        RP[Public Client<br/>ticketmonster-spa - PKCE]
        RC[Confidential Client<br/>ticketmonster-gateway]
        RS[Service Clients<br/>booking-service, inventory-service...]
        ROLES[Client Roles:<br/>booking:read, booking:admin,<br/>catalog:write, pricing:write...]
    end

    SPA[SPA / Mobile] -->|Authorization Code + PKCE| RP
    RP -->|JWT access_token + refresh_token| SPA
    SPA -->|Bearer JWT| GW[API Gateway]
    GW -->|valida assinatura JWT<br/>JWKS do Keycloak| RC
    GW -->|Token Exchange<br/>escopo reduzido| RS
    GW -->|Bearer JWT com claims/roles| SVC[Microsserviços]
    SVC -->|client_credentials<br/>chamada serviço-a-serviço| RS
```

### 5.2 Glossário de termos utilizados

| Termo | Definição | Onde se aplica no TicketMonster |
|---|---|---|
| **IdP (Identity Provider)** | Sistema central responsável por autenticar usuários e emitir tokens. | Keycloak, único IdP do domínio `ticketmonster`. |
| **Realm** | Espaço isolado de configuração no Keycloak (usuários, clients, roles próprios). | Um realm `ticketmonster`, separado de outros sistemas da organização. |
| **Client (Keycloak)** | Aplicação/serviço registrado no realm que pode solicitar tokens. | SPA pública, Gateway, cada microsserviço. |
| **Public Client** | Client que **não** guarda segredo (roda no browser/mobile, código exposto ao usuário). Autentica via **PKCE**, não por `client_secret`. | SPA de compra (`ticketmonster-spa`), app mobile. |
| **Confidential Client** | Client que roda em ambiente controlado (servidor) e pode guardar um `client_secret` com segurança. | API Gateway/BFF, que troca `client_secret` por token nas chamadas administrativas. |
| **PKCE (Proof Key for Code Exchange)** | Extensão do Authorization Code Flow que impede interceptação do código de autorização, sem exigir `client_secret`. Obrigatório para *public clients*. | Login da SPA/mobile. |
| **Authorization Code Flow** | Fluxo OAuth2 padrão para aplicações com interface de usuário: usuário autentica no Keycloak, recebe um *code*, trocado por tokens. | Login do comprador e do administrador. |
| **Client Credentials Flow** | Fluxo OAuth2 sem usuário: o próprio serviço se autentica com `client_id` + `client_secret` para obter um token representando a si mesmo. | Chamadas internas serviço-a-serviço (ex.: `telemetry` consultando `catalog` em um job agendado, sem usuário envolvido). |
| **Token Exchange** | Mecanismo (RFC 8693) que permite trocar um token por outro com escopo/audiência diferente — ex.: reduzir os privilégios do token do usuário antes de repassar a uma chamada interna. | Gateway reduzindo o escopo do JWT do comprador antes de chamar `catalog` para buscar preço. |
| **JWT (JSON Web Token)** | Formato de token assinado (JWS), contendo claims (usuário, roles, expiração) verificáveis sem consulta ao IdP a cada uso. | Formato do `access_token` emitido pelo Keycloak. |
| **Claims** | Pares chave-valor dentro do JWT (ex.: `sub`, `email`, `realm_access.roles`). | `sub` usado para checagem de *ownership* de reserva. |
| **`sub` (Subject)** | Claim padrão do JWT que identifica unicamente o usuário autenticado. | Comparado ao `ownerId` do `Booking` para autorização de posse. |
| **`azp` / `client_id`** | Claim que identifica **qual client** originou o token (a aplicação, não o usuário). | Usado na auditoria para saber se a ação veio da SPA, do admin ou de um job interno. |
| **JWKS (JSON Web Key Set)** | Endpoint público do Keycloak com as chaves públicas usadas para validar a assinatura dos JWTs, sem precisar chamar o Keycloak a cada validação. | Consumido pelo Gateway e por cada serviço via `quarkus-oidc`. |
| **RBAC (Role-Based Access Control)** | Modelo de autorização baseado em papéis atribuídos ao usuário/client. | `ROLE_CUSTOMER`, `ROLE_ADMIN` e roles granulares (ver 5.3). |
| **Realm Roles** | Roles globais do realm, válidas em qualquer client. | Ex.: `platform-admin` (acesso total, uso restrito). |
| **Client Roles** | Roles específicas de um client, mais granulares que Realm Roles. | `booking:admin`, `catalog:write`, `pricing:write` (ver 5.3). |
| **ABAC (Attribute-Based Access Control)** | Modelo de autorização baseado em atributos do recurso/contexto (não só papel do usuário) — usado quando RBAC não é suficiente. | Checagem de *ownership* de reserva (o atributo "dono da reserva == usuário autenticado"). |
| **UMA (User-Managed Access)** | Extensão do OAuth2 suportada pelo Keycloak Authorization Services, permitindo políticas de autorização finas administradas centralmente (não hardcoded no código do serviço). | Alternativa avaliada para autorização por recurso, caso o RBAC simples se torne insuficiente (ver 5.3). |
| **BFF (Backend for Frontend)** | Camada intermediária entre o cliente e os microsserviços, adaptando/agregando respostas e concentrando preocupações transversais. | Papel exercido pelo API Gateway nesta arquitetura. |
| **mTLS (mutual TLS)** | TLS onde **ambos os lados** apresentam certificado — o cliente também prova sua identidade ao servidor, não só o contrário. | Comunicação serviço-a-serviço dentro do cluster (via service mesh). |
| **ACL (Access Control List) de tópico Kafka** | Regra que define quais *principals* (serviços) podem publicar/consumir em um tópico específico. | Só `booking` publica em `booking-events`; só `inventory`/`telemetry` consomem. |
| **SASL/SCRAM** | Mecanismo de autenticação do Kafka baseado em usuário/senha desafio-resposta, alternativa mais simples ao mTLS entre serviço e broker. | Autenticação dos microsserviços junto ao broker Kafka. |
| **Least Privilege** | Princípio de conceder ao usuário/serviço apenas as permissões mínimas necessárias para sua função. | Guia a granularidade das Client Roles (5.3) e do Token Exchange. |
| **BOLA (Broken Object Level Authorization)** | Categoria de vulnerabilidade (OWASP API Top 10) onde o sistema verifica autenticação, mas não verifica se o usuário autenticado tem posse do recurso específico solicitado. | Gap confirmado no legado: `GET/DELETE /rest/bookings/{id}` sem checagem de dono. |
| **Mass Assignment** | Vulnerabilidade onde o backend aceita e persiste qualquer campo enviado no payload, sem *whitelist* explícita. | Gap confirmado: `BookingDTO.fromDTO()` no legado aceita qualquer campo do payload de entrada. |

### 5.3 Autorização fina — RBAC não é suficiente sozinho

RBAC responde "o usuário tem a role X?", mas não responde "esta reserva específica pertence a ele?". É o gap mapeado como **RN-NOVA-01** (`microservices_specification.md`, seção 5.3) — hoje `GET/DELETE /rest/bookings/{id}` é aberto a qualquer requisitante, mesmo autenticado.

* **Ownership check no *use case*, não só no Gateway:** comparar o claim `sub` do JWT com o `ownerId` do agregado `Booking` dentro do `CancelBookingUseCase`/`GetBookingUseCase` — nunca confiar apenas na validação de role feita na borda.
* **Guest checkout (sem login):** manter e-mail + `CancellationCode`, mas armazenar o código **com hash** (nunca texto plano) e comparar por hash — inexistente hoje (o legado grava `"abc"` fixo em texto plano).
* **Client Roles granulares**, evitando um `ROLE_ADMIN` monolítico: `catalog:write`, `booking:admin`, `pricing:write`, `venue:write` — cada um mapeado a um *use case* específico, seguindo *least privilege*. O legado hoje não tem nenhuma segmentação: quem acessa o painel acessa tudo.
* **Keycloak Authorization Services (UMA/policies)** como evolução futura, caso a granularidade cresça além do que Client Roles conseguem expressar de forma simples (ex.: regras que dependem de atributo do recurso, não só do papel do usuário).

### 5.4 OWASP API Security Top 10 — mapeamento a gaps já confirmados no legado

| Risco OWASP | Gap confirmado no legado | Mitigação proposta |
|---|---|---|
| **BOLA** — Broken Object Level Authorization | `GET/DELETE /rest/bookings/{id}` sem checagem de posse | Ownership check (5.3) |
| **Broken Function Level Authorization** | Painel admin acessível sem autenticação alguma | RBAC + Client Roles + enforcement no Gateway e no serviço |
| **Excessive Data Exposure** | `GET /rest/bookings` lista todas as reservas de todos os clientes, incluindo e-mail (PII) de terceiros | Listagem filtrada por `sub`; endpoint administrativo separado, com role própria |
| **Mass Assignment** | `BookingDTO.fromDTO()` aceita e persiste qualquer campo enviado, sem *whitelist* | DTOs de entrada explícitos por *use case*, nunca reaproveitar DTO de leitura para escrita |
| **Lack of Resources & Rate Limiting** | Nenhum limite hoje — o próprio `Bot` do legado evidencia como é fácil gerar carga | Rate limiting no Gateway (RN-NOVA-04) |

### 5.5 Proteção de dados pessoais (PII)

`Booking.contactEmail` é dado pessoal e hoje trafega e é exposto sem controle algum.

* Mascaramento de e-mail em respostas administrativas (`j***@acme.com`) quando o requisitante não for o próprio dono.
* Política de retenção definida para reservas antigas contendo PII.
* Se aplicável (LGPD/GDPR): regra de negócio própria para exclusão/anonimização de reserva antiga — inexistente no legado.

### 5.6 Segurança na camada de mensageria (Kafka)

RBAC via Keycloak cobre REST; os eventos de domínio (`BookingConfirmedEvent`, etc.) também carregam e-mail e trafegam entre serviços sem controle equivalente hoje:

* **SASL/SCRAM ou mTLS** na comunicação com o broker.
* **ACLs por tópico**: apenas `booking` publica em `booking-events`; apenas `inventory`/`telemetry` autorizados consomem.
* Payload de evento sem PII bruta — apenas identificadores; quem precisar do e-mail consulta o serviço dono via API autenticada.

### 5.7 Transporte e operação

* **mTLS entre serviços** via service mesh (Istio/Linkerd) — necessário mesmo com JWT, pois o JWT prova identidade do *usuário final*, não do *serviço chamador*.
* **Secrets management** (Vault/Kubernetes Secrets) para `client_secret` dos Confidential Clients — nunca hardcoded (o legado tem exatamente esse padrão de erro aplicado a segredo de negócio: `cancellationCode = "abc"` fixo no código).
* **CORS estrito** no Gateway — hoje inexistente, pois front e backend do legado compartilham o mesmo WAR.
* **Cabeçalhos de segurança** (CSP, HSTS, X-Content-Type-Options) nas respostas do Gateway/BFF.
* **MFA obrigatório** para o realm/role de administrador — o painel admin manipula preço e inventário, é o alvo de maior impacto.

### 5.8 Auditoria correlacionada à identidade

A trilha de auditoria via eventos já proposta (RN-NOVA-05) passa a correlacionar cada evento de domínio ao `sub` (usuário) e ao `azp`/`client_id` (client que originou a ação), permitindo responder "quem fez o quê e quando" — inviável no legado, que não possui nenhum conceito de identidade.

---

## 6. Fluxo de Referência: Saga de Criação de Reserva

```mermaid
sequenceDiagram
    autonumber
    actor Comprador
    participant GW as API Gateway
    participant BOOK as microservice-booking
    participant OUT as Outbox (booking_db)
    participant KAFKA as Kafka
    participant INV as microservice-inventory
    participant REDIS as Redis

    Comprador->>GW: POST /api/v1/bookings (Idempotency-Key)
    GW->>BOOK: Encaminha requisição
    activate BOOK
    BOOK->>BOOK: Valida payload (Either/Railway)
    BOOK->>OUT: Persiste Booking (status=PENDING) + evento BookingInitiated (mesma transação)
    BOOK-->>GW: 202 Accepted (bookingId, status=PROCESSING)
    GW-->>Comprador: 202 Accepted
    deactivate BOOK

    OUT->>KAFKA: Publica BookingInitiatedEvent (Outbox Relay)
    KAFKA->>INV: Consome evento
    activate INV
    INV->>REDIS: SET lock:seat:... NX PX 60000 (por assento, contíguo se possível)
    alt Assentos alocados com sucesso
        INV->>KAFKA: Publica SeatsAllocatedEvent
    else Falha de alocação
        INV->>KAFKA: Publica SeatsAllocationFailedEvent
    end
    deactivate INV

    KAFKA->>BOOK: Consome resultado da alocação
    activate BOOK
    alt Sucesso
        BOOK->>BOOK: Atualiza status=CONFIRMED, gera CancellationCode (UUID)
        BOOK->>KAFKA: Publica BookingConfirmedEvent
    else Falha
        BOOK->>BOOK: Atualiza status=FAILED
        BOOK->>KAFKA: Publica BookingFailedEvent
    end
    deactivate BOOK
```

---

## 7. Próximos Passos Sugeridos

1. Validar este documento com os times de plataforma e segurança.
2. Detalhar o modelo de dados (schemas Postgres) por serviço em documento técnico complementar.
3. Elaborar ADRs individuais para as decisões mais controversas (granularidade do lock, orquestração vs. coreografia da Saga).
4. Priorizar a Fase 1 do roadmap (`modernization_architecture.md`, seção 18) com foco no `microservice-inventory`, por concentrar o maior risco técnico herdado do legado.