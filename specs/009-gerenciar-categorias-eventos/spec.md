# Feature Specification: Gerenciar Catálogo de Categorias de Eventos (Admin)

**Feature Branch**: `009-gerenciar-categorias-eventos`  
**Created**: 2026-07-25  
**Status**: Draft  
**Input**: User description: "* **US-CAT-09:** Gerenciar catálogo de categorias de eventos (Admin). respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar Nova Categoria de Evento (Priority: P1)

Como um Administrador do sistema, quero cadastrar uma nova categoria de evento (ex.: "Rock", "Teatro", "Futebol"), para organizar os eventos do catálogo e permitir que os clientes encontrem atrações de seu interesse.

**Why this priority**: Categorias são entidades fundamentais de classificação. Sem elas, eventos não podem ser cadastrados conforme exigido pela regra de negócio RN04 (Categoria Obrigatória).

**Independent Test**: Pode ser testado de forma independente enviando uma requisição HTTP POST para a API administrativa com uma descrição válida e única, verificando a geração de UUID e o status HTTP 201 (Created).

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, **When** ele envia uma solicitação de criação de categoria com uma descrição válida e não nula (ex.: "Orquestra"), **Then** o sistema deve persistir a categoria, atribuir um UUID único e retornar HTTP 201 (Created) com os dados da entidade criada.
2. **Given** um Administrador autenticado, **When** ele tenta cadastrar uma categoria com descrição já existente na base de dados (RN06), **Then** o sistema deve rejeitar o cadastro e retornar HTTP 409 (Conflict) formatado via RFC 7807 (Problem Details).
3. **Given** um Administrador autenticado, **When** ele tenta cadastrar uma categoria com descrição vazia, em branco ou nula, **Then** o sistema deve retornar HTTP 400 (Bad Request) detalhando a falha de validação via RFC 7807.

---

### User Story 2 - Alterar Descrição de Categoria Existente (Priority: P2)

Como um Administrador do sistema, quero atualizar a descrição de uma categoria de evento existente, para corrigir erros de digitação ou padronizar a nomenclatura das categorias.

**Why this priority**: Permite a manutenção preventiva e corretiva dos termos do catálogo sem afetar os eventos vinculados.

**Independent Test**: Pode ser testado enviando uma solicitação HTTP PUT/PATCH para uma categoria cadastrada com a nova descrição e verificando que os eventos previamente vinculados a ela mantêm a associação correta.

**Acceptance Scenarios**:

1. **Given** uma categoria existente, **When** o Administrador altera a descrição para uma nova descrição válida e única, **Then** o sistema deve atualizar o registro e retornar HTTP 200 (OK).
2. **Given** uma categoria existente, **When** o Administrador tenta alterar a sua descrição para um nome que já é utilizado por outra categoria (RN06), **Then** o sistema deve impedir a alteração e retornar HTTP 409 (Conflict).

---

### User Story 3 - Excluir Categoria Sem Eventos Associados (Priority: P3)

Como um Administrador do sistema, quero excluir categorias que não possuem eventos vinculados, para manter o cadastro de domínio limpo de categorias obsoletas ou criadas por engano.

**Why this priority**: Permite a limpeza cadastral e impede a proliferação de categorias vazias no portal de vendas.

**Independent Test**: Pode ser testado criando uma categoria sem eventos e enviando a solicitação HTTP DELETE, confirmando a remoção do registro (HTTP 204 No Content).

**Acceptance Scenarios**:

1. **Given** uma categoria de evento que NÃO possui nenhum evento associado, **When** o Administrador envia uma solicitação de exclusão HTTP DELETE, **Then** o sistema deve remover a categoria e retornar HTTP 204 (No Content).
2. **Given** uma categoria de evento que possui um ou mais eventos vinculados, **When** o Administrador solicita a sua exclusão, **Then** o sistema DEVE impedir a remoção (`ON DELETE RESTRICT`) e retornar HTTP 409 (Conflict) detalhando a restrição de integridade referencial.

---

### User Story 4 - Listar e Consultar Categorias de Eventos (Priority: P4)

Como um Administrador ou Consumidor do Catálogo, quero listar todas as categorias de eventos cadastradas, para selecionar categorias disponíveis no cadastro de eventos ou nos filtros de busca do portal público.

**Why this priority**: Garante que o painel administrativo e a interface pública possam obter as opções de categorias ativas de forma eficiente e rápida.

**Independent Test**: Pode ser testado efetuando requisições GET para a listagem de categorias e verificando o retorno ordenado por descrição.

**Acceptance Scenarios**:

1. **Given** categorias cadastradas na base, **When** uma consulta HTTP GET é realizada na listagem de categorias, **Then** o sistema deve retornar HTTP 200 (OK) com a lista de categorias ordenadas alfabeticamente por descrição.

---

### Edge Cases

- **Tentativa de exclusão de categoria com eventos vinculados**: O sistema deve impor restrição de banco de dados (`ON DELETE RESTRICT`) e tratar a exceção no serviço retornando HTTP 409 (Conflict).
- **Tentativa de alteração/exclusão de categoria inexistente**: Deve retornar HTTP 404 (Not Found).
- **Sensibilidade a maiúsculas/minúsculas e espaços em branco na descrição**: A validação de unicidade (RN06) deve considerar espaços extras (usando `trim`) para evitar categorias duplicadas por formatação.
- **Acesso não autorizado**: Endpoints de alteração/exclusão/inclusão sem token JWT de `ROLE_ADMIN` devem retornar HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: As operações de escrita (POST, PUT, DELETE) exigem autenticação com role `ROLE_ADMIN`. A leitura (GET) é pública para permitir o preenchimento de filtros de busca e telas de cadastro.
- **Representação de Erros**: Erros de validação e conflitos devem utilizar estritamente o formato RFC 7807 (Problem Details).
- **Semântica HTTP**: 201 Created para inclusão, 200 OK para alteração/consulta, 204 No Content para exclusão, 400 Bad Request para validação, 409 Conflict para duplicidade/restrição de integridade e 404 Not Found para IDs inexistentes.
- **Identificadores**: Todas as categorias utilizam UUIDs v4 como chave primária.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE fornecer endpoints REST administrativos para o gerenciamento de categorias de eventos (criação, alteração, exclusão e listagem).
- **FR-002**: O sistema DEVE exigir que a descrição de cada categoria seja única em toda a base de dados e não nula/vazia (RN06).
- **FR-003**: O sistema DEVE aceitar apenas descrições de categoria com até 120 caracteres.
- **FR-004**: O sistema DEVE proibir expressamente a exclusão de qualquer categoria que possua um ou mais eventos associados (`ON DELETE RESTRICT`), retornando HTTP 409 (Conflict).
- **FR-005**: O sistema DEVE formatar todas as respostas de falha utilizando o padrão RFC 7807 (Problem Details).
- **FR-006**: O sistema DEVE implementar suite de testes automatizados incluindo: testes unitários de validação da entidade `EventCategory`, testes de contrato REST, testes de integração com Testcontainers PostgreSQL e Redis, e um teste E2E cobrindo o fluxo P1 de criação e validação de unicidade.
- **FR-007**: O sistema DEVE invalidar ou atualizar o cache Redis de categorias sempre que uma categoria for incluída, alterada ou excluída.

### Key Entities *(include if feature involves data)*

- **EventCategory (Entidade de Domínio)**:
  - `id`: UUID (Chave primária).
  - `description`: String (Obrigatório, Único, até 120 caracteres).
  - `createdAt`: TIMESTAMPTZ (Data de criação).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Administradores realizam o cadastro ou alteração de categorias com tempo de resposta da API <= 150 ms (P95).
- **SC-002**: 100% das tentativas de exclusão de categorias com eventos associados são bloqueadas com resposta informativa HTTP 409 RFC 7807.
- **SC-003**: Zero categorias duplicadas ou vazias no banco de dados.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Latência P95 para operações de escrita de categoria <= 150 ms.
- **PR-002**: Consulta e cache-aside Redis da listagem de categorias respondem em <= 50 ms (P95).
- **PR-003**: Taxa de erro não tratado (5xx) em steady-state < 0,1%.

## Assumptions

- O gerenciamento de categorias é executado no microsserviço `microservice-catalog` sobre o schema `catalog.event_category` no PostgreSQL.
- O cache de categorias é mantido em Redis para otimizar as consultas de filtros públicos de catálogo.
- Autenticação e autorização via Keycloak com role `ROLE_ADMIN`.
