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

**Why this priority**: Evita inconsistências de contagem de lugares entre a camada de catálogo e o serviço de inventário (P2).

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
