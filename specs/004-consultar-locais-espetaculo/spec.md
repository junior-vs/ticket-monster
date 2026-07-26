# Feature Specification: US-CAT-04 — Consultar Locais de Espetáculo (Venues) Disponíveis

**Feature Branch**: `004-consultar-locais-espetaculo`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "from docs\spec\microservice-catalog.spec.md create spec for US-CAT-04: Consultar lista de locais de espetáculo (Venues) disponíveis para venda. respect E:\develop\repos\java-projects\ticket-monster\docs\arch\arquitetura-solucao.md"

**Origem**: `docs/spec/microservice-catalog.spec.md` (US-CAT-04, RN07), `arquitetura-solucao.md` (seções 4, 6.1, 8, 10, 15.4, 25.3) e Constituição da Modernização (Princípios I a V).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar Lista Pública de Locais de Espetáculo (Priority: P1)

Como visitante da plataforma TicketMonster, quero consultar a lista de locais de espetáculo (Venues) cadastrados para descobrir os teatros, arenas e estádios onde ocorrem os eventos disponíveis para venda.

**Why this priority**: A navegação por locais de espetáculo (P1) é uma das formas primárias de descoberta de eventos para usuários interessados em eventos na sua região física ou em arenas específicas.

**Independent Test**: Pode ser testado de forma independente realizando uma requisição HTTP GET pública ao endpoint de locais (`GET /api/v1/venues`) e verificando que a resposta 200 OK inclui a lista de Venues com nome, descrição, endereço completo (logradouro, cidade, estado, CEP) e metadados de paginação.

**Acceptance Scenarios**:

1. **Given** que existem locais de espetáculo cadastrados ("Teatro Municipal", "Arena Stadium", "Espaço Cultural"), **When** o visitante consulta `GET /api/v1/venues`, **Then** o sistema responde 200 OK exibindo a lista dos locais cadastrados com nome, descrição e dados de endereço.
2. **Given** que o visitante consulta a lista de locais sem parâmetros explícitos de paginação, **When** a requisição é processada, **Then** o sistema aplica por padrão `page=0` e `size=20`, ordenando os locais em ordem alfabética ascendente por nome (RN07).
3. **Given** que não existe nenhum local de espetáculo cadastrado no banco de dados, **When** o visitante realiza a consulta, **Then** o sistema responde 200 OK com array vazio (`[]`) e metadados de paginação indicando `totalElements: 0` e `totalPages: 0`.

---

### User Story 2 - Paginação e Filtragem de Resultados de Locais (Priority: P2)

Como visitante navegando em um catálogo extenso de salas e arenas, quero navegar de forma paginada pela lista de locais e aplicar filtros simples (ex.: por cidade ou estado) para encontrar venues próximos.

**Why this priority**: Melhora a usabilidade e reduz o tempo de busca em catálogos regionais ou nacionais abrangentes (P2).

**Independent Test**: Pode ser testado enviando requisições com parâmetros de paginação (`page`, `size`) e filtros opcionais de localização (`city`, `state`), validando os itens e metadados retornados.

**Acceptance Scenarios**:

1. **Given** 25 locais de espetáculo cadastrados, **When** o visitante solicita a primeira página com `page=0` e `size=10`, **Then** o sistema retorna os 10 primeiros locais (em ordem alfabética), informando `totalElements: 25` e `totalPages: 3`.
2. **Given** que o visitante aplica filtro por cidade `GET /api/v1/venues?city=São Paulo`, **When** a requisição é executada, **Then** o sistema retorna apenas os locais cujo endereço corresponda à cidade informada.
3. **Given** que o visitante envia um parâmetro de paginação inválido (ex.: `size=150` ou `page=-1`), **When** a requisição atinge o serviço, **Then** o sistema rejeita a chamada com HTTP 400 Bad Request no formato RFC 7807 Problem Details.

---

### User Story 3 - Visualizar Endereço Estruturado e Informações de Acessibilidade do Local (Priority: P3)

Como visitante planejando uma visita, quero que cada local na listagem apresente seu endereço físico completo e padronizado para facilitar o deslocamento.

**Why this priority**: Assegura a consistência da experiência do usuário (P3) entre os diferentes canais de consumo (web e mobile).

**Independent Test**: Pode ser testado validando o contrato DTO de resposta do endpoint de locais, garantindo que o objeto embutido de endereço contém todas as propriedades especificadas (`addressLine`, `city`, `state`, `postalCode`, `country`).

**Acceptance Scenarios**:

1. **Given** um local com endereço completo cadastrado, **When** retornado na listagem, **Then** o objeto JSON expõe o endereço no formato embutido padronizado.
2. **Given** a resposta de qualquer venue na listagem, **When** comparada entre o canal público e o painel administrativo de leitura, **Then** os nomes dos campos e a estrutura de dados são idênticos.

---

### Edge Cases

- **Locais Sem Eventos Vinculados**: Um `Venue` cadastrado mas sem nenhum `Show` ou `Performance` associado no momento continua aparecendo na listagem geral de locais (cadastro master de infraestrutura).
- **Parâmetros Malformados de Busca**: Requisições com valores sintaticamente inválidos para paginação ou filtros retornam HTTP 400 Bad Request com payload RFC 7807 Problem Details.
- **Degradação de Cache**: Em caso de falha temporária do cache Redis (`catalog:venues`), a consulta realiza fallback para o PostgreSQL (`catalog_db`), mantendo a funcionalidade e os contratos de resposta sem gerar erro 500.

---

### User Experience Consistency *(mandatory)*

- **Canal Público Sem Autenticação**: O endpoint `GET /api/v1/venues` É público e NÃO exige token de autenticação (`arquitetura-solucao.md`, seção 15.4).
- **Formato de Erro Padronizado**: Erros de validação e de parâmetros de requisição DEVEM utilizar o formato RFC 7807 Problem Details.
- **Convenção de Paginação**: Paginação DEVE utilizar os parâmetros `page` (base 0) e `size` (padrão 20, máximo 100).
- **Ordenação Padrão**: A ordenação padrão da lista de locais DEVE ser alfabética ascendente pelo nome do local (`name` ASC).
- **Estrutura de Endereço**: Dados de localização DEVEM seguir a nomenclatura de propriedades DTO camelCase (`addressLine`, `city`, `state`, `postalCode`, `country`).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST disponibilizar o endpoint público `GET /api/v1/venues` para consulta da lista de locais de espetáculo.
- **FR-002**: O sistema MUST retornar para cada local listado o seu identificador único (UUID), nome (`name`), descrição (`description`) e o objeto de endereço completo (`addressLine`, `city`, `state`, `postalCode`, `country`).
- **FR-003**: O sistema MUST aceitar chamadas ao endpoint de locais sem exigir token Bearer JWT (`ROLE_CUSTOMER` ou `ROLE_ADMIN`).
- **FR-004**: O sistema MUST suportar paginação `page` (base 0) e `size` (padrão 20, máximo 100) na listagem de locais.
- **FR-005**: O sistema MUST aceitar filtros opcionais por cidade (`city`) e estado (`state`) na consulta de locais.
- **FR-006**: O sistema MUST aplicar ordenação padrão ascendente pelo nome do local (`name` ASC).
- **FR-007**: O sistema MUST responder HTTP 200 OK com array vazio (`[]`) e metadados de paginação zerados quando não houver locais cadastrados ou que atendam aos filtros informados.
- **FR-008**: O sistema MUST responder HTTP 400 Bad Request no formato RFC 7807 Problem Details quando os parâmetros de paginação ou filtro forem sintaticamente inválidos.
- **FR-009**: O sistema MUST possuir suíte automatizada de testes cobrindo:
  - Teste unitário para ordenação e filtragem do repositório/agregado de Venue.
  - Teste de contrato REST para o schema da listagem e erros RFC 7807.
  - Teste de integração via Testcontainers para PostgreSQL (`catalog_db`) e Redis.
  - Teste E2E cobrindo a jornada de consulta de locais.
- **FR-010**: O sistema MUST atender ao orçamento de desempenho (p95 <= 250 ms sob carga com Redis Cache-Aside).

---

### Key Entities *(include if feature involves data)*

- **Venue (Local de Espetáculo)**: Agregado raiz de catálogo contendo ID (`id` UUID), nome único (`name`), descrição (`description`), endereço físico (`addressLine`, `city`, `state`, `postalCode`, `country`) e data de criação (`created_at`).
- **VenuePage (Página de Locais)**: Value Object que contem a lista de locais DTO e os metadados de paginação (`page`, `size`, `totalElements`, `totalPages`).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das consultas públicas a `GET /api/v1/venues` retornam status 200 OK com os dados de endereço estruturados corretamente.
- **SC-002**: Visitantes conseguem localizar os locais de uma determinada cidade/estado em até 2 interações.
- **SC-003**: 100% dos parâmetros de requisição inválidos resultam em respostas RFC 7807 400 Bad Request formatadas.
- **SC-004**: Zero divergências na estrutura de campos entre o canal público e os contratos administrativos de leitura de venues.

---

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: A latência p95 para a consulta da lista de locais DEVE ser <= 250 ms sob o perfil de carga definido (utilizando cache-aside no Redis para a chave `catalog:venues`).
- **PR-002**: O serviço DEVE sustentar a vazão de pico esperada em consultas de locais sem ultrapassar a taxa de erro de 1% no servidor (5xx).
- **PR-003**: Em caso de falha de conexão com o Redis, a aplicação DEVE realizar fallback transparente de consulta diretamente no PostgreSQL (`catalog_db`), preservando a funcionalidade e os contratos de resposta sem gerar erro 500.

---

## Assumptions

- O `microservice-catalog` gerencia e possui autoridade sobre a tabela `catalog.venue` no PostgreSQL `catalog_db`.
- A estratégia de cache no Redis utiliza a chave `catalog:venues` com TTL de 1 hora, invalidada sempre que um local for cadastrado, alterado ou excluído via painel administrativo.
- O endpoint `GET /api/v1/venues` é público e livre de controle de acesso por token JWT (`arquitetura-solucao.md`, seção 15.4).
- Respostas de erro utilizam estritamente o formato RFC 7807 Problem Details.

---

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001, FR-002 | `microservice-catalog.spec.md` US-CAT-04, RN07 | Consulta da lista de locais (Venues) |
| FR-003 | `arquitetura-solucao.md` seção 15.4 | Acesso público ao endpoint `GET /venues` |
| FR-004, FR-005, FR-006 | `arquitetura-solucao.md` seção 25.3 (RN42) | Paginação `page`/`size` e filtros |
| FR-007, FR-008 | `arquitetura-solucao.md` seção 8 | Tratamento de resposta vazia e erro RFC 7807 |
| FR-009, FR-010 | Constituição da Modernização, Princípios III e V | Qualidade de testes e metas de latência p95 <= 250ms |
