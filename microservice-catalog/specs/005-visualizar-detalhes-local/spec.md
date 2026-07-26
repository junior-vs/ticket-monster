# Feature Specification: US-CAT-05 — Visualizar Detalhes de um Local de Espetáculo (Venue)

**Feature Branch**: `005-visualizar-detalhes-local`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "from docs\spec\microservice-catalog.spec.md create spec for US-CAT-05: Visualizar detalhes de um local de espetáculo (capacidade total, seções físicas e endereço). respect E:\develop\repos\java-projects\ticket-monster\docs\arch\arquitetura-solucao.md"

**Origem**: `docs/spec/microservice-catalog.spec.md` (US-CAT-05, RN07, RN11, RN12), `arquitetura-solucao.md` (seções 4, 6.1, 8, 10, 15.4, 25.3) e Constituição da Modernização (Princípios I a V).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar Detalhes Completos de um Venue por ID (Priority: P1)

Como visitante da plataforma TicketMonster, quero consultar os detalhes de um local de espetáculo específico por seu ID para visualizar seu endereço completo, a lista de seções físicas e a capacidade total de público.

**Why this priority**: A visualização detalhada do local de espetáculo (P1) é fundamental para que o visitante entenda a distribuição dos setores (plateia, camarotes, arquibancada) e a capacidade do local antes de selecionar ingressos.

**Independent Test**: Pode ser testado de forma independente realizando uma requisição HTTP GET pública ao endpoint `/api/v1/venues/{id}` com um UUID válido e verificando que a resposta 200 OK expõe o local, seu endereço, a lista de seções com capacidades calculadas e a capacidade total agregada do Venue.

**Acceptance Scenarios**:

1. **Given** um local de espetáculo cadastrado com ID válido ("Teatro Municipal") contendo as seções "Plateia VIP" (10 fileiras x 10 assentos = 100) e "Balcão" (5 fileiras x 20 assentos = 100), **When** o visitante consulta `GET /api/v1/venues/{id}`, **Then** o sistema responde 200 OK detalhando o venue, seu endereço, a lista de seções e a capacidade total calculada de 200 assentos (RN12).
2. **Given** que o visitante tenta consultar um local informando um UUID que não existe no cadastro (`catalog.venue`), **When** a requisição é processada, **Then** o sistema responde HTTP 404 Not Found com estrutura RFC 7807 Problem Details.
3. **Given** que o visitante tenta consultar um local informando um identificador com formato sintaticamente inválido (ex.: `123-abc-invalid`), **When** a requisição atinge a API, **Then** o sistema rejeita com HTTP 400 Bad Request no formato RFC 7807 Problem Details.

---

### User Story 2 - Cálculo de Capacidade por Seção e Agregação Total do Local (Priority: P2)

Como comprador planejando uma reserva, quero que a capacidade de cada seção física e o total acumulado do local sejam exibidos de forma consistente e sem discrepâncias de cálculo.

**Why this priority**: Evita inconsistências de contagem de lugares entre a camada de catálogo e o serviço de inventário (P2).# Feature Specification: US-CAT-05 — Visualizar Detalhes de um Local de Espetáculo (Venue)

**Feature Branch**: `005-visualizar-detalhes-local`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "from docs\spec\microservice-catalog.spec.md create spec for US-CAT-05: Visualizar detalhes de um local de espetáculo (capacidade total, seções físicas e endereço). respect E:\develop\repos\java-projects\ticket-monster\docs\arch\arquitetura-solucao.md"

**Origem**: `microservice-catalog_spec.md` (US-CAT-05, RN12), `arquitetura-solucao.md` (seções 4, 6.1, 8, 10, 15.4).

**Dependências**: US-CAT-10 (CRUD de `Venue`/`Section`) — dependência funcional obrigatória, pois esta feature lê dados escritos por US-CAT-10. **Não** há dependência funcional de US-CAT-04 (listagem pública) — são duas leituras paralelas, independentes entre si, ambas apoiadas em US-CAT-10; o agrupamento na mesma fase de backlog é de ordenação de entrega, não de dependência de dados.

## Correções aplicadas nesta versão

1. **Citação incorreta de RN07/RN11 removida**: essas RNs tratam de unicidade de nome (Venue e Section), não de composição do payload de detalhe. Apenas RN12 (cálculo de capacidade da seção) é de fato aplicável a esta US.
2. **`totalCapacity` marcado como agregação nova, não herdada**: nem `microservice-catalog_spec.md` nem a DDL definem capacidade total agregada por Venue — apenas `Section.capacity` é coluna gerada. A soma das seções é uma decisão de produto desta spec.
3. **Citação de "Constituição da Modernização, Princípios III e V" removida** — documento não existe entre as fontes fornecidas.
4. **Chave de cache `catalog:venue:{id}` marcada como extensão proposta**, sem contrato definido em `arquitetura-solucao.md` seção 10.
5. **Dependência de US-CAT-04 removida do vínculo funcional** — mantida apenas a dependência real de US-CAT-10.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar Detalhes Completos de um Venue por ID (Priority: P1)

Como visitante da plataforma TicketMonster, quero consultar os detalhes de um local de espetáculo específico por seu ID para visualizar seu endereço completo, a lista de seções físicas e a capacidade total de público.

**Why this priority**: A visualização detalhada do local de espetáculo (P1) é fundamental para que o visitante entenda a distribuição dos setores (plateia, camarotes, arquibancada) e a capacidade do local antes de selecionar ingressos.

**Independent Test**: Pode ser testado de forma independente realizando uma requisição HTTP GET pública ao endpoint `/api/v1/venues/{id}` com um UUID válido e verificando que a resposta 200 OK expõe o local, seu endereço, a lista de seções com capacidades calculadas e a capacidade total agregada do Venue. Requer venue e seções pré-cadastrados via US-CAT-10.

**Acceptance Scenarios**:

1. **Given** um local de espetáculo cadastrado com ID válido ("Teatro Municipal") contendo as seções "Plateia VIP" (10 fileiras x 10 assentos = 100) e "Balcão" (5 fileiras x 20 assentos = 100), **When** o visitante consulta `GET /api/v1/venues/{id}`, **Then** o sistema responde 200 OK detalhando o venue, seu endereço, a lista de seções (cada uma com `capacity` derivado por RN12) e a capacidade total agregada de 200 assentos (soma das seções — ver FR-007).
2. **Given** que o visitante tenta consultar um local informando um UUID que não existe no cadastro (`catalog.venue`), **When** a requisição é processada, **Then** o sistema responde HTTP 404 Not Found com estrutura RFC 7807 Problem Details.
3. **Given** que o visitante tenta consultar um local informando um identificador com formato sintaticamente inválido (ex.: `123-abc-invalid`), **When** a requisição atinge a API, **Then** o sistema rejeita com HTTP 400 Bad Request no formato RFC 7807 Problem Details.

---

### User Story 2 - Cálculo de Capacidade por Seção e Agregação Total do Local (Priority: P2)

Como comprador planejando uma reserva, quero que a capacidade de cada seção física e o total acumulado do local sejam exibidos de forma consistente e sem discrepâncias de cálculo.

**Why this priority**: Evita inconsistências de contagem de lugares entre a camada de catálogo e o serviço de inventário (P2).

**Independent Test**: Pode ser testado validando que a propriedade `capacity` de cada seção corresponde exatamente à multiplicação `numberOfRows * rowCapacity` (calculada como coluna gerada no banco relacional — RN12) e que a propriedade `totalCapacity` do Venue é a soma exata das capacidades de suas seções, calculada em tempo de leitura (agregação de aplicação, não coluna persistida no Venue).

**Acceptance Scenarios**:

1. **Given** um Venue cadastrado que possui 3 seções físicas, **When** os detalhes do local são retornados pela API, **Then** cada seção exibe a propriedade `capacity` igual a `numberOfRows * rowCapacity` (RN12, coluna gerada) e o objeto raiz do Venue exibe `totalCapacity` correspondente à soma das 3 seções (agregação calculada na leitura, sem persistência própria).
2. **Given** um Venue cadastrado que ainda não possui nenhuma seção física associada, **When** o visitante consulta seus detalhes, **Then** o sistema responde 200 OK com a lista de seções vazia (`"sections": []`) e `totalCapacity: 0`.

---

### User Story 3 - Visualizar Endereço Estruturado e Metadados do Local (Priority: P3)

Como visitante ou aplicativo cliente, quero receber as informações de endereço e metadados do local em um formato JSON estruturado e previsível.

**Why this priority**: Assegura a padronização e interoperabilidade do contrato DTO (P3) entre web, mobile e parceiros.

**Independent Test**: Pode ser testado inspecionando a resposta JSON de `GET /api/v1/venues/{id}` e validando que o objeto embutido `address` contem todas as chaves obrigatórias (`addressLine`, `city`, `state`, `postalCode`, `country`).

**Acceptance Scenarios**:

1. **Given** um local com endereço completo cadastrado, **When** os detalhes são consultados, **Then** o objeto embutido de endereço é serializado com todas as propriedades no padrão camelCase.
2. **Given** requisições concorrentes de consulta ao mesmo local, **When** processadas pela API, **Then** a resposta é servida via cache Redis (`catalog:venue:{id}`, contrato proposto — ver Assumptions) mantendo latência reduzida.

---

### Edge Cases

- **ID Sintaticamente Inválido**: identificadores que não sejam UUIDs válidos (ex.: `/venues/abc`) retornam HTTP 400 Bad Request com payload RFC 7807 indicando parâmetro malformado.
- **Venue Inexistente**: UUIDs válidos que não correspondam a nenhum registro na tabela `catalog.venue` retornam HTTP 404 Not Found.
- **Seção sem Assentos**: seções cadastradas com fileiras ou capacidade zerada são bloqueadas na camada administrativa por constraints de banco (`ck_section_rows_positive`, `ck_section_row_capacity_positive`), garantindo que apenas seções válidas com capacidade > 0 existam no banco — regra pertencente a US-CAT-10, apenas consumida aqui.
- **Endereço Parcialmente Vazio**: campos de endereço são nullable na DDL de origem (ver spec de US-CAT-10); o detalhe deve tolerar venue com endereço parcial ou totalmente vazio, sem erro.
- **Falha Temporária de Cache**: em caso de falha de conexão com o Redis, a consulta executa fallback transparente para a base PostgreSQL (`catalog_db`), retornando os dados sem erro 500.

---

### User Experience Consistency *(mandatory)*

- **Canal Público Sem Autenticação**: o endpoint `GET /api/v1/venues/{id}` é público e não exige token Bearer JWT (`arquitetura-solucao.md`, seção 15.4).
- **Formato de Erro Padronizado**: respostas de erro 400 (Bad Request) e 404 (Not Found) devem utilizar a estrutura RFC 7807 Problem Details.
- **Capacidade Calculada Transparente**: a capacidade de cada seção deve utilizar o resultado da coluna gerada no banco relacional (`capacity = number_of_rows * row_capacity`) (RN12); `totalCapacity` é agregação de leitura, não persistida.
- **Nomenclatura DTO**: propriedades em JSON devem seguir a convenção camelCase (`addressLine`, `numberOfRows`, `rowCapacity`, `totalCapacity`).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST disponibilizar o endpoint público `GET /api/v1/venues/{id}` para consultar os detalhes de um local de espetáculo por seu ID (UUID).
- **FR-002**: O sistema MUST retornar para o local consultado seu ID, nome (`name`), descrição (`description`), o objeto de endereço completo (`addressLine`, `city`, `state`, `postalCode`, `country`), a lista de seções físicas (`sections`) e a capacidade total agregada do local (`totalCapacity`).
- **FR-003**: O sistema MUST retornar para cada seção física listada seu ID, nome da seção (`name`), número de fileiras (`numberOfRows`), capacidade por fileira (`rowCapacity`) e capacidade calculada da seção (`capacity = numberOfRows * rowCapacity`), valor lido diretamente da coluna gerada pelo banco (RN12).
- **FR-004**: O sistema MUST responder HTTP 404 Not Found no formato RFC 7807 Problem Details quando o ID informado não corresponder a um local cadastrado no banco de dados.
- **FR-005**: O sistema MUST responder HTTP 400 Bad Request no formato RFC 7807 Problem Details quando o ID do local for um UUID sintaticamente malformado.
- **FR-006**: O sistema MUST aceitar chamadas ao endpoint de detalhes do local sem exigir token Bearer JWT.
- **FR-007**: O sistema MUST calcular `totalCapacity` como a soma exata do campo `capacity` de todas as seções físicas pertencentes ao venue, em tempo de leitura (agregação de aplicação) — capacidade nova desta feature, sem coluna equivalente na DDL de `Venue`.
- **FR-008**: O sistema MUST responder 200 OK exibindo `"sections": []` e `totalCapacity: 0` quando o venue consultado for válido mas não possuir seções cadastradas.
- **FR-009**: O sistema MUST possuir suíte automatizada de testes cobrindo:
  - Teste unitário para agregação de capacidade de seções e venue (incluindo caso de zero seções).
  - Teste de contrato REST para o schema dos detalhes do venue e respostas de erro 400/404 RFC 7807.
  - Teste de integração via Testcontainers para PostgreSQL (`catalog_db`) e Redis.
  - Teste E2E cobrindo a jornada de consulta de detalhes de local (dependente de dados criados via US-CAT-10).
- **FR-010**: O sistema SHOULD atender ao orçamento de desempenho proposto (p95 <= 250 ms sob carga utilizando Redis Cache-Aside na chave `catalog:venue:{id}`) — meta não confirmada em `arquitetura-solucao.md`, sujeita a validação em teste de carga (ver Assumptions).

---

### Key Entities *(include if feature involves data)*

- **Venue (Local de Espetáculo)**: agregado raiz de catálogo contendo ID (UUID), nome único (`name`), descrição, endereço (`addressLine`, `city`, `state`, `postalCode`, `country`, todos nullable) e coleção de seções físicas. Entidade escrita por US-CAT-10.
- **Section (Seção Física)**: entidade pertencente ao Venue contendo ID (UUID), nome da seção, número de fileiras (`numberOfRows`), capacidade por fileira (`rowCapacity`) e capacidade calculada armazenada em coluna gerada (`capacity`). Entidade escrita por US-CAT-10.
- **VenueDetailsDTO**: objeto de transferência de dados contendo as informações completas do Venue, a lista de DTOs de seção e o atributo agregado calculado em leitura `totalCapacity` (não persistido).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das consultas com IDs válidos retornam status 200 OK exibindo a lista de seções com capacidades calculadas corretamente.
- **SC-002**: 100% das requisições com IDs inexistentes ou malformados resultam em respostas RFC 7807 com códigos 404 ou 400, respectivamente.
- **SC-003**: 100% das seções retornadas expõem a capacidade exata calculada por `numberOfRows * rowCapacity` (RN12).
- **SC-004**: Zero discrepâncias entre a soma das capacidades de seção persistidas no banco e o valor de `totalCapacity` exposto pela API.

---

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: A latência p95 para requisições de detalhes de venue é a meta proposta de <= 250 ms sob o perfil de carga definido, condicionada ao cache-aside no Redis (chave `catalog:venue:{id}`) — ver Assumptions quanto à ausência de lastro documental.
- **PR-002**: O serviço DEVE sustentar a vazão de pico esperada em navegações de catálogo sem ultrapassar a taxa de erro de 1% no servidor (5xx).
- **PR-003**: Em caso de falha no Redis, o sistema DEVE realizar fallback de leitura direto na base PostgreSQL (`catalog_db`), preservando os contratos de resposta sem gerar erro 500.

---

## Assumptions

- O `microservice-catalog` gerencia e possui autoridade exclusiva sobre as tabelas `catalog.venue` e `catalog.section` no banco PostgreSQL `catalog_db`; os dados consultados aqui são escritos por US-CAT-10.
- A capacidade da seção física é mantida como coluna gerada no banco relacional (`capacity INT GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED`) (RN12). `totalCapacity` do Venue não tem equivalente na DDL — é uma agregação de leitura desta feature, recalculada a cada consulta (ou a cada atualização de cache).
- A estratégia de cache no Redis utiliza a chave `catalog:venue:{id}` com TTL de 1 hora, invalidada sempre que o venue ou suas seções sofrerem alteração administrativa — contrato de chave proposto por esta spec, não definido em `arquitetura-solucao.md` seção 10.
- O endpoint `GET /api/v1/venues/{id}` é público e não exige autenticação JWT (`arquitetura-solucao.md`, seção 15.4).
- Meta de latência p95 <= 250 ms (PR-001/FR-010) é proposta desta spec, não confirmada em documento de arquitetura — sujeita a validação em teste de carga antes de virar SLO.

---

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001 | `microservice-catalog_spec.md` US-CAT-05 | Endpoint de detalhe |
| FR-002 | US-CAT-05 | Campos do payload; `totalCapacity` é extensão (ver FR-007) |
| FR-003 | RN12 | Capacidade de seção via coluna gerada |
| FR-004, FR-005 | `arquitetura-solucao.md` seção 8 | Erros 404 e 400 em formato RFC 7807 Problem Details |
| FR-006 | `arquitetura-solucao.md` seção 15.4 | Endpoint público `GET /venues/{id}` |
| FR-007 | Extensão local (não herdada) | Agregação `totalCapacity` — sem RN ou coluna correspondente na DDL |
| FR-008 | Consequência lógica de FR-007 com zero seções | — |
| FR-009 | Padrão de testes adotado nas specs anteriores do `microservice-catalog` | — |
| FR-010 / PR-001 | Meta proposta, sem lastro documental | Citação anterior a "Constituição da Modernização" removida (documento não existe entre as fontes) |
| Dependência de US-CAT-10 | Análise de ordenação de tarefas do `microservice-catalog` | Leitura depende de escrita prévia; dependência de US-CAT-04 removida por não ser funcional |

**Independent Test**: Pode ser testado validando que a propriedade `capacity` de cada seção corresponde exatamente à multiplicação `numberOfRows * rowCapacity` (calculada como coluna gerada no banco relacional - RN12) e que a propriedade `totalCapacity` do Venue é a soma exata das capacidades de suas seções.

**Acceptance Scenarios**:

1. **Given** um Venue cadastrado que possui 3 seções físicas, **When** os detalhes do local são retornados pela API, **Then** cada seção exibe a propriedade `capacity` igual a `numberOfRows * rowCapacity` e o objeto raiz do Venue exibe `totalCapacity` correspondente à soma das 3 seções.
2. **Given** um Venue cadastrado que ainda não possui nenhuma seção física associada, **When** o visitante consulta seus detalhes, **Then** o sistema responde 200 OK com a lista de seções vazia (`"sections": []`) e `totalCapacity: 0`.

---

### User Story 3 - Visualizar Endereço Estruturado e Metadados do Local (Priority: P3)

Como visitante ou aplicativo cliente, quero receber as informações de endereço e metadados do local em um formato JSON estruturado e previsível.

**Why this priority**: Assegura a padronização e interoperabilidade do contrato DTO (P3) entre web, mobile e parceiros.

**Independent Test**: Pode ser testado inspecionando a resposta JSON de `GET /api/v1/venues/{id}` e validando que o objeto embutido `address` contem todas as chaves obrigatórias (`addressLine`, `city`, `state`, `postalCode`, `country`).

**Acceptance Scenarios**:

1. **Given** um local com endereço completo cadastrado, **When** os detalhes são consultados, **Then** o objeto embutido de endereço é serializado com todas as propriedades no padrão camelCase.
2. **Given** requisições concorrentes de consulta ao mesmo local, **When** processadas pela API, **Then** a resposta é servida via cache Redis (`catalog:venue:{id}`) mantendo latência reduzida.

---

### Edge Cases

- **ID Sintaticamente Inválido**: Identificadores que não sejam UUIDs válidos (ex.: `/venues/abc`) retornam HTTP 400 Bad Request com payload RFC 7807 indicando parâmetro malformado.
- **Venue Inexistente**: UUIDs válidos que não correspondam a nenhum registro na tabela `catalog.venue` retornam HTTP 404 Not Found.
- **Seção sem Assentos**: Seções cadastradas com fileiras ou capacidade zerada são bloqueadas na camada administrativa por constraints de banco (`ck_section_rows_positive`, `ck_section_row_capacity_positive`), garantindo que apenas seções válidas com capacidade > 0 existam no banco.
- **Falha Temporária de Cache**: Em caso de falha de conexão com o Redis, a consulta executa fallback transparente para a base PostgreSQL (`catalog_db`), retornando os dados sem erro 500.

---

### User Experience Consistency *(mandatory)*

- **Canal Público Sem Autenticação**: O endpoint `GET /api/v1/venues/{id}` É público e NÃO exige token Bearer JWT (`arquitetura-solucao.md`, seção 15.4).
- **Formato de Erro Padronizado**: Respostas de erro 400 (Bad Request) e 404 (Not Found) DEVEM utilizar a estrutura RFC 7807 Problem Details.
- **Capacidade Calculada Transparente**: A capacidade das seções DEVE utilizar o resultado da coluna gerada no banco relacional (`capacity = number_of_rows * row_capacity`) (RN12).
- **Nomenclatura DTO**: Propriedades em JSON DEVEM seguir a convenção camelCase (`addressLine`, `numberOfRows`, `rowCapacity`, `totalCapacity`).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST disponibilizar o endpoint público `GET /api/v1/venues/{id}` para consultar os detalhes de um local de espetáculo por seu ID (UUID).
- **FR-002**: O sistema MUST retornar para o local consultado seu ID, nome (`name`), descrição (`description`), o objeto de endereço completo (`addressLine`, `city`, `state`, `postalCode`, `country`), a lista de seções físicas (`sections`) e a capacidade total agregada do local (`totalCapacity`).
- **FR-003**: O sistema MUST retornar para cada seção física listada seu ID, nome da seção (`name`), número de fileiras (`numberOfRows`), capacidade por fileira (`rowCapacity`) e capacidade calculada da seção (`capacity = numberOfRows * rowCapacity`) (RN12).
- **FR-004**: O sistema MUST responder HTTP 404 Not Found no formato RFC 7807 Problem Details quando o ID informado não corresponder a um local cadastrado no banco de dados.
- **FR-005**: O sistema MUST responder HTTP 400 Bad Request no formato RFC 7807 Problem Details quando o ID do local for um UUID sintaticamente malformado.
- **FR-006**: O sistema MUST aceitar chamadas ao endpoint de detalhes do local sem exigir token Bearer JWT (`ROLE_CUSTOMER` ou `ROLE_ADMIN`).
- **FR-007**: O sistema MUST calcular `totalCapacity` como a soma exata do campo `capacity` de todas as seções físicas pertencentes ao venue.
- **FR-008**: O sistema MUST responder 200 OK exibindo `"sections": []` e `totalCapacity: 0` quando o venue consultado for válido mas não possuir seções cadastradas.
- **FR-009**: O sistema MUST possuir suíte automatizada de testes cobrindo:
  - Teste unitário para agregação de capacidade de seções e venue.
  - Teste de contrato REST para o schema dos detalhes do venue e respostas de erro 400/404 RFC 7807.
  - Teste de integração via Testcontainers para PostgreSQL (`catalog_db`) e Redis.
  - Teste E2E cobrindo a jornada de consulta de detalhes de local.
- **FR-010**: O sistema MUST atender ao orçamento de desempenho (p95 <= 250 ms sob carga utilizando Redis Cache-Aside na chave `catalog:venue:{id}`).

---

### Key Entities *(include if feature involves data)*

- **Venue (Local de Espetáculo)**: Agregado raiz de catálogo contendo ID (UUID), nome único (`name`), descrição, endereço (`addressLine`, `city`, `state`, `postalCode`, `country`) e coleção de seções físicas.
- **Section (Seção Física)**: Entidade pertencente ao Venue contendo ID (UUID), nome da seção, número de fileiras (`numberOfRows`), capacidade por fileira (`rowCapacity`) e capacidade calculada armazenada em coluna gerada (`capacity`).
- **VenueDetailsDTO**: Objeto de transferência de dados contendo as informações completas do Venue, a lista de DTOs de seção e o atributo acumulado `totalCapacity`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das consultas com IDs válidos retornam status 200 OK exibindo a lista de seções com capacidades calculadas corretamente.
- **SC-002**: 100% das requisições com IDs inexistentes ou malformados resultam em respostas RFC 7807 com códigos 404 ou 400, respectivamente.
- **SC-003**: 100% das seções retornadas expõem a capacidade exata calculada por `numberOfRows * rowCapacity` (RN12).
- **SC-004**: Zero discrepâncias de valores entre a capacidade calculada no banco de dados relacional e os DTOs expostos pela API REST.

---

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: A latência p95 para requisições de detalhes de venue DEVE ser <= 250 ms sob o perfil de carga definido (utilizando cache-aside no Redis na chave `catalog:venue:{id}`).
- **PR-002**: O serviço DEVE sustentar a vazão de pico esperada em navegações de catálogo sem ultrapassar a taxa de erro de 1% no servidor (5xx).
- **PR-003**: Em caso de falha no Redis, o sistema DEVE realizar fallback de leitura direto na base PostgreSQL (`catalog_db`), preservando os contratos de resposta sem gerar erro 500.

---

## Assumptions

- O `microservice-catalog` gerencia e possui autoridade exclusiva sobre as tabelas `catalog.venue` e `catalog.section` no banco PostgreSQL `catalog_db`.
- A capacidade da seção física é mantida como coluna gerada no banco relacional (`capacity INT GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED`) (RN12).
- A estratégia de cache no Redis utiliza a chave `catalog:venue:{id}` com TTL de 1 hora, invalidada sempre que o venue ou suas seções sofrerem alteração administrativa.
- O endpoint `GET /api/v1/venues/{id}` é público e não exige autenticação JWT (`arquitetura-solucao.md`, seção 15.4).

---

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001, FR-002, FR-003 | `microservice-catalog.spec.md` US-CAT-05, RN07, RN11, RN12 | Detalhes do local e seções físicas |
| FR-004, FR-005 | `arquitetura-solucao.md` seção 8 | Erros 404 e 400 em formato RFC 7807 Problem Details |
| FR-006 | `arquitetura-solucao.md` seção 15.4 | Endpoint público `GET /venues/{id}` |
| FR-007, FR-008 | `microservice-catalog.spec.md` RN12 | Agregação de capacidade total do local |
| FR-009, FR-010 | Constituição da Modernização, Princípios III e V | Qualidade de testes automatizados e latência p95 <= 250ms |
