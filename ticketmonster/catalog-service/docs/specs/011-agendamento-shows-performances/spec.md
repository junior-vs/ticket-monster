# Feature Specification: Criar e Alterar Agendamentos de Shows e Performances (Admin)

**Feature Branch**: `011-agendamento-shows-performances`  
**Created**: 2026-07-25  
**Status**: Draft  
**Input**: User description: "* **US-CAT-11:** Criar e alterar agendamentos de shows e suas performances temporais (Admin). respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Criar Agendamento de Show (Vinculação Evento + Venue) (Priority: P1)

Como um Administrador do sistema, quero associar um Evento a um Local Físico (Venue) criando um Show, para estabelecer em qual espaço físico uma atração artística será apresentada.

**Why this priority**: O `Show` é a entidade central que une a atração cultural (`Event`) ao local físico de realização (`Venue`). Sem o `Show`, não é possível criar a agenda temporal de sessões (`Performance`).

**Independent Test**: Pode ser testado de forma independente efetuando uma requisição HTTP POST para vincular um `eventId` e um `venueId` existentes, verificando a criação do registro com status HTTP 201 (Created) e geração de UUID.

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, um Evento cadastrado e um Venue cadastrado, **When** ele envia uma solicitação de criação de Show vinculando o Evento ao Venue, **Then** o sistema deve criar o Show e retornar HTTP 201 (Created).
2. **Given** um Show já existente entre o Evento A e o Venue B, **When** o Administrador tenta criar outro Show exatamente com a mesma associação Evento A + Venue B (RN08), **Then** o sistema deve rejeitar a criação e retornar HTTP 409 (Conflict) formatado via RFC 7807 (Problem Details).
3. **Given** um Administrador autenticado, **When** ele tenta criar um Show utilizando um `eventId` ou `venueId` inexistente, **Then** o sistema deve retornar HTTP 404 (Not Found) ou HTTP 400 (Bad Request).

---

### User Story 2 - Agendar Performances Temporais (Sessões) para um Show (Priority: P2)

Como um Administrador do sistema, quero agendar datas e horários específicos (Performances) para um Show cadastrado, definindo a agenda de apresentações disponíveis para venda de ingressos.

**Why this priority**: É a agenda temporal real em que o público assiste ao espetáculo e compra ingressos para assentos específicos.

**Independent Test**: Pode ser testado enviando uma solicitação HTTP POST contendo o `showId`, a data/hora ISO-8601 da apresentação e uma descrição opcional (ex.: "Sessão Noturna"), verificando o retorno HTTP 201 (Created).

**Acceptance Scenarios**:

1. **Given** um Show existente, **When** o Administrador cadastra uma Performance informando uma data/hora futura válida (RN09), **Then** o sistema deve persistir a Performance, notificar o microsserviço de inventário para preparação do mapa de assentos e retornar HTTP 201 (Created).
2. **Given** uma Performance já agendada para o Show A na data/hora "2026-08-15T20:00:00Z", **When** o Administrador tenta agendar outra Performance para o mesmo Show A exatamente na mesma data e hora (RN10), **Then** o sistema deve bloquear o agendamento duplicado e retornar HTTP 409 (Conflict).
3. **Given** um Administrador autenticado, **When** ele envia uma solicitação de criação de Performance sem data/hora ou com data/hora nula (RN09), **Then** o sistema deve retornar HTTP 400 (Bad Request).

---

### User Story 3 - Alterar Data/Hora ou Detalhes de uma Performance (Priority: P3)

Como um Administrador do sistema, quero alterar a data/hora ou a descrição de uma Performance agendada, para ajustar horários em caso de mudanças de programação.

**Why this priority**: Permite correções e remanejamentos de horário da agenda artística.

**Independent Test**: Pode ser testado enviando requisição HTTP PUT/PATCH para o ID da Performance com a nova data/hora e verificando a atualização do registro (HTTP 200 OK).

**Acceptance Scenarios**:

1. **Given** uma Performance agendada, **When** o Administrador envia uma nova data/hora válida sem conflitos de horário com o mesmo Show, **Then** o sistema deve atualizar a Performance, atualizar o carimbo de data da sessão e retornar HTTP 200 (OK).
2. **Given** uma Performance existente, **When** o Administrador altera seu horário para um horário onde já existe outra sessão agendada do mesmo Show, **Then** o sistema deve impedir a alteração com HTTP 409 (Conflict).

---

### User Story 4 - Consultar e Cancelar Agendamento de Performance (Priority: P4)

Como um Administrador do sistema, quero cancelar uma Performance agendada que foi descontinuada, para remover a sessão da agenda de vendas.

**Why this priority**: Permite a gestão de cancelamentos de sessões artísticas antes da liberação ou encerramento das vendas.

**Independent Test**: Pode ser testado enviando uma solicitação HTTP DELETE para o ID de uma Performance e confirmando a sua remoção (HTTP 204 No Content).

**Acceptance Scenarios**:

1. **Given** uma Performance agendada sem ingressos confirmados, **When** o Administrador envia uma solicitação de exclusão HTTP DELETE, **Then** o sistema deve remover a Performance e retornar HTTP 204 (No Content).

---

### Edge Cases

- **Tentativa de agendamento de Show para Evento no status `ARCHIVED`**: O sistema deve impedir a vinculação e retornar HTTP 400 (Bad Request) informando que eventos arquivados não podem receber novos agendamentos.
- **Fuso horário e formato de data/hora**: As datas/horas das Performances devem ser obrigatoriamente trafegadas e persistidas no padrão ISO-8601 UTC (`TIMESTAMPTZ`).
- **Concorrência ao criar agendamentos no mesmo segundo**: O banco de dados deve impor as constraints de unicidade (`uq_show_event_venue` e `uq_performance_show_date`) para garantir integridade mesmo sob requisições concorrentes simultâneas (CA-CAT-02-UNI).
- **Acesso sem a role `ROLE_ADMIN`**: Deve retornar HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: Endpoints de criação, alteração e exclusão de Shows e Performances são protegidos com `ROLE_ADMIN`. Endpoints de leitura de agenda de shows e performances são abertos para consulta pública do catálogo de compras (US-CAT-06, US-CAT-07).
- **Representação de Erros**: Erros de validação de datas ou conflitos relacionais são retornados no formato RFC 7807 (Problem Details).
- **Formatos de Data/Hora**: O formato padrão de entrada e saída para `performanceDate` é ISO-8601 UTC (`YYYY-MM-DDTHH:mm:ssZ`).
- **Identificadores**: Shows e Performances usam UUID v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE fornecer endpoints REST administrativos restritos a `ROLE_ADMIN` para criação, alteração, exclusão e consulta de Shows e Performances.
- **FR-002**: O sistema DEVE proibir a duplicação do vínculo entre o mesmo `Event` e o mesmo `Venue` (RN08), garantindo que um Show represente uma associação única e retornando HTTP 409 (Conflict) sob tentativa de duplicidade.
- **FR-003**: O sistema DEVE exigir obrigatoriamente a presença de data/hora válida (`performance_date`) para o cadastro de qualquer Performance (RN09).
- **FR-004**: O sistema DEVE garantir a unicidade de data/hora por Show (RN10), impedindo o agendamento de duas Performances do mesmo Show exatamente na mesma data e hora e retornando HTTP 409 (Conflict).
- **FR-005**: O sistema DEVE utilizar a especificação RFC 7807 (Problem Details) para a estrutura de respostas de erro e conflito de agendamento (CA-CAT-02-UNI).
- **FR-006**: O sistema DEVE notificar o contexto de inventário (via evento de domínio ou mensageria) quando uma nova Performance for criada, permitindo a inicialização da carga de assentos das seções do local.
- **FR-007**: O sistema DEVE implementar suite de testes automatizados incluindo: testes unitários para regras de unicidade de agendamento, testes de contrato REST, testes de integração com Testcontainers PostgreSQL e Redis, e um teste E2E cobrindo a jornada P1/P2.
- **FR-008**: O sistema DEVE invalidar/atualizar o cache Redis do catálogo de shows e performances sempre que uma Performance for criada, alterada ou desmarcada.

### Key Entities *(include if feature involves data)*

- **Show (Agregado Catalog / Scheduling)**:
  - `id`: UUID (Chave primária).
  - `eventId`: UUID (FK para Event, Obrigatório).
  - `venueId`: UUID (FK para Venue, Obrigatório).
  - `createdAt`: TIMESTAMPTZ.
  - *Constraint*: `UNIQUE (eventId, venueId)`.
- **Performance (Entidade Filha de Show)**:
  - `id`: UUID (Chave primária).
  - `showId`: UUID (FK para Show, ON DELETE CASCADE).
  - `performanceDate`: TIMESTAMPTZ (Obrigatório).
  - `description`: String (Opcional, até 255 caracteres).
  - *Constraint*: `UNIQUE (showId, performanceDate)`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Operações de agendamento de Shows e Performances respondem com latência P95 <= 200 ms.
- **SC-002**: 100% dos conflitos de duplicação de Show (Evento + Venue) ou choque de horário na mesma sessão são bloqueados com HTTP 409 RFC 7807.
- **SC-003**: 0% de erros de parsing ou ambiguidade em fusos horários no tráfego de datas de apresentações.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Latência P95 nas APIs administrativas de escrita de agendamento <= 200 ms.
- **PR-002**: Consulta e cache Redis de agendas de Shows respondem em <= 50 ms (P95).
- **PR-003**: Taxa de erros não tratados do servidor (5xx) em steady-state < 0,1%.

## Assumptions

- Implementação realizada no microsserviço `microservice-catalog` utilizando o schema `catalog.show` e `catalog.performance` no PostgreSQL (`catalog_db`).
- Autenticação e autorização providas pelo Keycloak JWT exigindo a role `ROLE_ADMIN`.
- A integração com `microservice-inventory` para inicialização do mapa de assentos é disparada após o agendamento bem-sucedido de uma Performance.
