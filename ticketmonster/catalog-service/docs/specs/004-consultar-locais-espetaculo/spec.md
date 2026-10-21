# Feature Specification: US-CAT-04 — Consultar Locais de Espetáculo (Venues) Disponíveis

**Feature Branch**: `004-consultar-locais-espetaculo`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "from docs\spec\microservice-catalog.spec.md create spec for US-CAT-04: Consultar lista de locais de espetáculo (Venues) disponíveis para venda. respect E:\develop\repos\java-projects\ticket-monster\docs\arch\arquitetura-solucao.md"

**Origem**: `microservice-catalog_spec.md` (US-CAT-04, RN07), `arquitetura-solucao.md` (seções 4, 6.1, 8, 10, 15.4, 25.3).

**Dependência**: US-CAT-10 (CRUD de `Venue`/`Section`) — esta feature é somente leitura sobre dados escritos por US-CAT-10; não há valor de teste E2E completo sem venues previamente cadastrados.

## Correções aplicadas nesta versão

1. **Ordenação alfabética não é derivada de RN07**: RN07 trata exclusivamente de unicidade de nome (`UNIQUE (name)`, não vazio). Não há RN, US ou seção de `arquitetura-solucao.md` que defina ordenação como regra de negócio. Tratada aqui como decisão de UX proposta por esta spec, não como regra herdada.
2. **Filtro por `city`/`state` marcado como extensão não coberta pela fonte**: nem `microservice-catalog_spec.md` nem `arquitetura-solucao.md` preveem esse filtro. A DDL de origem não possui índice em `city`/`state`. Mantido como capacidade proposta, com ambiguidade de match (exato vs. parcial/`ILIKE`) explicitada como decisão pendente.
3. **Citação de "Constituição da Modernização, Princípios I a V" removida** — documento não existe entre as fontes fornecidas; citação não verificável.
4. **Chave de cache `catalog:venues`** mantida como extensão proposta (não definida em `arquitetura-solucao.md` seção 10), agora referenciada explicitamente no FR correspondente, não apenas em Assumptions.
5. Dependência de US-CAT-10 declarada explicitamente.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar Lista Pública de Locais de Espetáculo (Priority: P1)

Como visitante da plataforma TicketMonster, quero consultar a lista de locais de espetáculo (Venues) cadastrados para descobrir os teatros, arenas e estádios onde ocorrem os eventos disponíveis para venda.

**Why this priority**: A navegação por locais de espetáculo (P1) é uma das formas primárias de descoberta de eventos para usuários interessados em eventos na sua região física ou em arenas específicas.

**Independent Test**: Pode ser testado de forma independente realizando uma requisição HTTP GET pública ao endpoint de locais (`GET /api/v1/venues`) e verificando que a resposta 200 OK inclui a lista de Venues com nome, descrição, endereço completo (logradouro, cidade, estado, CEP) e metadados de paginação. Requer venues pré-cadastrados via US-CAT-10.

**Acceptance Scenarios**:

1. **Given** que existem locais de espetáculo cadastrados ("Teatro Municipal", "Arena Stadium", "Espaço Cultural"), **When** o visitante consulta `GET /api/v1/venues`, **Then** o sistema responde 200 OK exibindo a lista dos locais cadastrados com nome, descrição e dados de endereço.
2. **Given** que o visitante consulta a lista de locais sem parâmetros explícitos de paginação, **When** a requisição é processada, **Then** o sistema aplica por padrão `page=0` e `size=20`, ordenando os locais em ordem alfabética ascendente por nome — decisão de UX desta spec, não derivada de RN07 (que trata apenas de unicidade).
3. **Given** que não existe nenhum local de espetáculo cadastrado no banco de dados, **When** o visitante realiza a consulta, **Then** o sistema responde 200 OK com array vazio (`[]`) e metadados de paginação indicando `totalElements: 0` e `totalPages: 0`.

---

### User Story 2 - Paginação e Filtragem de Resultados de Locais (Priority: P2)

Como visitante navegando em um catálogo extenso de salas e arenas, quero navegar de forma paginada pela lista de locais e aplicar filtros simples (ex.: por cidade ou estado) para encontrar venues próximos.

**Why this priority**: Melhora a usabilidade e reduz o tempo de busca em catálogos regionais ou nacionais abrangentes (P2).

**Independent Test**: Pode ser testado enviando requisições com parâmetros de paginação (`page`, `size`) e filtros opcionais de localização (`city`, `state`), validando os itens e metadados retornados.

> Nota: o filtro por `city`/`state` é uma capacidade proposta por esta spec, sem lastro em `microservice-catalog_spec.md` ou `arquitetura-solucao.md`. A DDL de origem não indexa esses campos — se o volume de venues justificar, recomenda-se avaliar índice (`btree` para match exato, ou `pg_trgm`/`GIN` para busca parcial) antes de expor o filtro em produção.

**Acceptance Scenarios**:

1. **Given** 25 locais de espetáculo cadastrados, **When** o visitante solicita a primeira página com `page=0` e `size=10`, **Then** o sistema retorna os 10 primeiros locais (em ordem alfabética), informando `totalElements: 25` e `totalPages: 3`.
2. **Given** que o visitante aplica filtro por cidade `GET /api/v1/venues?city=São Paulo` (valor URL-encoded), **When** a requisição é executada, **Then** o sistema retorna apenas os locais cujo campo `city` corresponda **exatamente** ao valor informado (case-insensitive) — match parcial fica fora de escopo desta versão, para evitar scan sem índice adequado.
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

- **Locais Sem Eventos Vinculados**: um `Venue` cadastrado mas sem nenhum `Show` ou `Performance` associado no momento continua aparecendo na listagem geral de locais (cadastro master de infraestrutura).
- **Parâmetros Malformados de Busca**: requisições com valores sintaticamente inválidos para paginação ou filtros retornam HTTP 400 Bad Request com payload RFC 7807 Problem Details.
- **Campos de Endereço Nulos**: como `addressLine`, `city`, `state`, `postalCode`, `country` são nullable na DDL de origem (ver spec de US-CAT-10), a listagem deve tolerar venues com endereço parcialmente ou totalmente vazio, sem erro.
- **Degradação de Cache**: em caso de falha temporária do cache Redis (`catalog:venues`), a consulta realiza fallback para o PostgreSQL (`catalog_db`), mantendo a funcionalidade e os contratos de resposta sem gerar erro 500.

---

### User Experience Consistency *(mandatory)*

- **Canal Público Sem Autenticação**: o endpoint `GET /api/v1/venues` é público e não exige token de autenticação (`arquitetura-solucao.md`, seção 15.4).
- **Formato de Erro Padronizado**: erros de validação e de parâmetros de requisição devem utilizar o formato RFC 7807 Problem Details.
- **Convenção de Paginação**: paginação deve utilizar os parâmetros `page` (base 0) e `size` (padrão 20, máximo 100), consistente com a correção de RN42 (seção 25.3).
- **Ordenação Padrão**: ordenação alfabética ascendente pelo nome do local (`name` ASC) — decisão de UX desta spec, não regra herdada.
- **Estrutura de Endereço**: dados de localização devem seguir a nomenclatura de propriedades DTO camelCase (`addressLine`, `city`, `state`, `postalCode`, `country`).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST disponibilizar o endpoint público `GET /api/v1/venues` para consulta da lista de locais de espetáculo, dependente dos dados cadastrados via US-CAT-10.
- **FR-002**: O sistema MUST retornar para cada local listado o seu identificador único (UUID), nome (`name`), descrição (`description`) e o objeto de endereço completo (`addressLine`, `city`, `state`, `postalCode`, `country`).
- **FR-003**: O sistema MUST aceitar chamadas ao endpoint de locais sem exigir token Bearer JWT.
- **FR-004**: O sistema MUST suportar paginação `page` (base 0) e `size` (padrão 20, máximo 100) na listagem de locais.
- **FR-005**: O sistema MAY aceitar filtros opcionais por cidade (`city`) e estado (`state`), com match exato (case-insensitive) — capacidade proposta, sem lastro na especificação de origem (ver nota em US2).
- **FR-006**: O sistema MUST aplicar ordenação padrão ascendente pelo nome do local (`name` ASC) — decisão de UX desta spec, não derivada de RN07.
- **FR-007**: O sistema MUST responder HTTP 200 OK com array vazio (`[]`) e metadados de paginação zerados quando não houver locais cadastrados ou que atendam aos filtros informados.
- **FR-008**: O sistema MUST responder HTTP 400 Bad Request no formato RFC 7807 Problem Details quando os parâmetros de paginação ou filtro forem sintaticamente inválidos.
- **FR-009**: O sistema MUST possuir suíte automatizada de testes cobrindo:
  - Teste unitário para ordenação e filtragem do repositório/agregado de Venue.
  - Teste de contrato REST para o schema da listagem e erros RFC 7807.
  - Teste de integração via Testcontainers para PostgreSQL (`catalog_db`) e Redis.
  - Teste E2E cobrindo a jornada de consulta de locais (dependente de dados criados via US-CAT-10).
- **FR-010**: O sistema SHOULD atender ao orçamento de desempenho proposto (p95 <= 250 ms sob carga com Redis Cache-Aside) — meta não confirmada em `arquitetura-solucao.md`, sujeita a validação em teste de carga (ver Assumptions).

---

### Key Entities *(include if feature involves data)*

- **Venue (Local de Espetáculo)**: agregado raiz de catálogo contendo ID (`id` UUID), nome único (`name`), descrição (`description`), endereço físico (`addressLine`, `city`, `state`, `postalCode`, `country`, todos nullable) e data de criação (`created_at`). Entidade escrita por US-CAT-10.
- **VenuePage (Página de Locais)**: Value Object que contém a lista de locais DTO e os metadados de paginação (`page`, `size`, `totalElements`, `totalPages`).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das consultas públicas a `GET /api/v1/venues` retornam status 200 OK com os dados de endereço estruturados corretamente.
- **SC-002**: Visitantes conseguem localizar os locais de uma determinada cidade/estado em até 2 interações (condicionado à disponibilidade do filtro de FR-005).
- **SC-003**: 100% dos parâmetros de requisição inválidos resultam em respostas RFC 7807 400 Bad Request formatadas.
- **SC-004**: Zero divergências na estrutura de campos entre o canal público e os contratos administrativos de leitura de venues.

---

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: A latência p95 para a consulta da lista de locais é a meta proposta de <= 250 ms sob o perfil de carga definido, condicionada ao cache-aside no Redis (chave `catalog:venues`) — ver Assumptions quanto à ausência de lastro documental para esse número.
- **PR-002**: O serviço DEVE sustentar a vazão de pico esperada em consultas de locais sem ultrapassar a taxa de erro de 1% no servidor (5xx).
- **PR-003**: Em caso de falha de conexão com o Redis, a aplicação DEVE realizar fallback transparente de consulta diretamente no PostgreSQL (`catalog_db`), preservando a funcionalidade e os contratos de resposta sem gerar erro 500.

---

## Assumptions

- O `microservice-catalog` gerencia e possui autoridade sobre a tabela `catalog.venue` no PostgreSQL `catalog_db`; os dados consultados aqui são escritos por US-CAT-10.
- A estratégia de cache no Redis utiliza a chave `catalog:venues` com TTL de 1 hora, invalidada sempre que um local for cadastrado, alterado ou excluído via painel administrativo — contrato de chave proposto por esta spec, não definido em `arquitetura-solucao.md` seção 10 (que só define `catalog:event:{id}` e `catalog:shows:performance:{id}`).
- O endpoint `GET /api/v1/venues` é público e livre de controle de acesso por token JWT (`arquitetura-solucao.md`, seção 15.4).
- Ordenação alfabética e filtro por cidade/estado são decisões de UX/produto propostas por esta spec, não requisitos herdados de `microservice-catalog_spec.md`.
- Meta de latência p95 <= 250 ms (PR-001/FR-010) é proposta desta spec, não confirmada em documento de arquitetura — sujeita a validação em teste de carga antes de virar SLO.
- Respostas de erro utilizam estritamente o formato RFC 7807 Problem Details.

---

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001, FR-002 | `microservice-catalog_spec.md` US-CAT-04 | Consulta da lista de locais (Venues) |
| FR-003 | `arquitetura-solucao.md` seção 15.4 | Acesso público ao endpoint `GET /venues` |
| FR-004 | `arquitetura-solucao.md` seção 25.3 (correção de RN42) | Paginação `page`/`size` base 0 |
| FR-005 | Extensão local (não herdada) | Filtro por cidade/estado — sem índice na DDL de origem |
| FR-006 | Extensão local (não herdada) | Ordenação alfabética — não é RN07 (unicidade), correção de citação |
| FR-007, FR-008 | `arquitetura-solucao.md` seção 8 | Tratamento de resposta vazia e erro RFC 7807 |
| FR-009 | Padrão de testes adotado nas specs anteriores do `microservice-catalog` | — |
| FR-010 / PR-001 | Meta proposta, sem lastro documental | Citação anterior a "Constituição da Modernização" removida (documento não existe entre as fontes) |
| Dependência de US-CAT-10 | Análise de ordenação de tarefas do `microservice-catalog` | Leitura depende de escrita prévia |