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
# Feature Specification: Gerenciar Catálogo de Categorias de Eventos (Admin)

**Feature Branch**: `009-gerenciar-categorias-eventos`
**Created**: 2026-07-25
**Status**: Draft
**Input**: User description: "* **US-CAT-09:** Gerenciar catálogo de categorias de eventos (Admin). respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

**Origem**: `microservice-catalog_spec.md` — US-CAT-09, RN06, item `[NOVO]` (proteção contra exclusão com Event associado).

## Extensões declaradas nesta spec (não decididas nos documentos-fonte)

Os itens abaixo são necessários para US-CAT-09 mas não estão explicitamente resolvidos em `microservice-catalog_spec.md` ou `arquitetura-solucao.md`. São tratados aqui como decisões locais desta feature, sujeitas a confirmação:

1. **Leitura pública de categorias**: a matriz de autorização (`arquitetura-solucao.md`, seção 15.4) lista `/event-categories` apenas na linha de escrita (`ROLE_ADMIN`); não há linha explícita de GET público para categorias, diferente de `/events`, `/venues`, `/shows`, `/performances`. Esta spec assume GET público como extensão natural da matriz (necessário para filtro de catálogo — US-CAT-02), mas isso deve ser confirmado e formalizado na seção 15.4 antes da implementação.
2. **Normalização de unicidade (RN06)**: a DDL de origem define `UNIQUE (description)` como constraint simples, sensível a espaços e caixa. O requisito de tratar `"Rock "` e `"Rock"` como duplicatas (RN06) não é garantido por essa constraint sozinha. Esta spec declara a extensão de schema necessária (ver FR-002a).
3. **Contrato de cache Redis**: `arquitetura-solucao.md` seção 10 não define chave de cache para listagem de categorias (só define `catalog:event:{id}` e `catalog:shows:performance:{id}`). Esta spec propõe a chave (ver FR-007).
4. **"Categoria ativa" (RN04)**: `microservice-catalog_spec.md` RN04 menciona "categoria de evento **ativa**", mas a entidade `EventCategory` não possui coluna de status. Assumido nesta spec que não existe conceito de categoria inativa nesta versão — RN04 lida como "categoria existente" (ver Assumptions).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar Nova Categoria de Evento (Priority: P1)

Como um Administrador do sistema, quero cadastrar uma nova categoria de evento (ex.: "Rock", "Teatro", "Futebol"), para organizar os eventos do catálogo e permitir que os clientes encontrem atrações de seu interesse.

**Why this priority**: Categorias são entidades fundamentais de classificação. Sem elas, eventos não podem ser cadastrados conforme exigido pela regra de negócio RN04 (Categoria Obrigatória).

**Independent Test**: Pode ser testado de forma independente enviando uma requisição HTTP POST para a API administrativa com uma descrição válida e única, verificando a geração de UUID e o status HTTP 201 (Created).

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, **When** ele envia uma solicitação de criação de categoria com uma descrição válida e não nula (ex.: "Orquestra"), **Then** o sistema deve persistir a categoria, atribuir um UUID único e retornar HTTP 201 (Created) com os dados da entidade criada.
2. **Given** um Administrador autenticado, **When** ele tenta cadastrar uma categoria com descrição já existente na base de dados (RN06), **Then** o sistema deve rejeitar o cadastro e retornar HTTP 409 (Conflict) formatado via RFC 7807 (Problem Details).
3. **Given** um Administrador autenticado, **When** ele tenta cadastrar uma categoria com descrição vazia, em branco ou nula, **Then** o sistema deve retornar HTTP 400 (Bad Request) detalhando a falha de validação via RFC 7807.
4. **Given** um Administrador autenticado, **When** ele tenta cadastrar uma categoria cuja descrição, após normalização (trim de espaços nas bordas), coincide com uma categoria já existente (ex.: `"Rock "` quando já existe `"Rock"`), **Then** o sistema deve rejeitar o cadastro e retornar HTTP 409 (Conflict), tratando-as como a mesma categoria.

---

### User Story 2 - Alterar Descrição de Categoria Existente (Priority: P2)

Como um Administrador do sistema, quero atualizar a descrição de uma categoria de evento existente, para corrigir erros de digitação ou padronizar a nomenclatura das categorias.

**Why this priority**: Permite a manutenção preventiva e corretiva dos termos do catálogo sem afetar os eventos vinculados.

**Independent Test**: Pode ser testado enviando uma solicitação HTTP PUT/PATCH para uma categoria cadastrada com a nova descrição e verificando que os eventos previamente vinculados a ela mantêm a associação correta.

**Acceptance Scenarios**:

1. **Given** uma categoria existente, **When** o Administrador altera a descrição para uma nova descrição válida e única (após normalização), **Then** o sistema deve atualizar o registro e retornar HTTP 200 (OK).
2. **Given** uma categoria existente, **When** o Administrador tenta alterar a sua descrição para um nome que já é utilizado por outra categoria, considerando a normalização de RN06, **Then** o sistema deve impedir a alteração e retornar HTTP 409 (Conflict).

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

**Why this priority**: Garante que o painel administrativo e a interface pública possam obter as opções de categorias de forma eficiente e rápida.

**Independent Test**: Pode ser testado efetuando requisições GET para a listagem de categorias e verificando o retorno ordenado por descrição.

> Nota: o acesso público (sem token) a este endpoint é uma extensão desta spec, não uma decisão já registrada na matriz de autorização (seção 15.4) — ver "Extensões declaradas nesta spec", item 1.

**Acceptance Scenarios**:

1. **Given** categorias cadastradas na base, **When** uma consulta HTTP GET é realizada na listagem de categorias, **Then** o sistema deve retornar HTTP 200 (OK) com a lista de categorias ordenadas alfabeticamente por descrição.

---

### Edge Cases

- **Tentativa de exclusão de categoria com eventos vinculados**: o sistema deve impor restrição de banco de dados (`ON DELETE RESTRICT`) e tratar a exceção no serviço retornando HTTP 409 (Conflict).
- **Tentativa de alteração/exclusão de categoria inexistente**: deve retornar HTTP 404 (Not Found).
- **Sensibilidade a maiúsculas/minúsculas e espaços em branco na descrição**: a validação de unicidade (RN06) considera espaços extras nas bordas (trim) para evitar categorias duplicadas por formatação. Diferenciação de caixa (`"Rock"` vs `"rock"`) permanece sensível nesta versão — não incluída no escopo de normalização (ver FR-002a); se exigida, requer decisão explícita adicional (ex.: `citext`).
- **Acesso não autorizado**: endpoints de alteração/exclusão/inclusão sem token JWT de `ROLE_ADMIN` devem retornar HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: as operações de escrita (POST, PUT, DELETE) exigem autenticação com role `ROLE_ADMIN`, conforme `arquitetura-solucao.md` seção 15.4. A leitura (GET) é tratada como pública nesta spec (extensão — ver nota acima).
- **Representação de Erros**: erros de validação e conflitos devem utilizar estritamente o formato RFC 7807 (Problem Details).
- **Semântica HTTP**: 201 Created para inclusão, 200 OK para alteração/consulta, 204 No Content para exclusão, 400 Bad Request para validação, 409 Conflict para duplicidade/restrição de integridade e 404 Not Found para IDs inexistentes.
- **Identificadores**: todas as categorias utilizam UUIDs v4 como chave primária.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE fornecer endpoints REST administrativos para o gerenciamento de categorias de eventos (criação, alteração, exclusão e listagem).
- **FR-002**: O sistema DEVE exigir que a descrição de cada categoria seja única em toda a base de dados e não nula/vazia (RN06).
- **FR-002a**: O sistema DEVE normalizar a descrição (trim de espaços nas bordas) antes de persistir e antes de comparar unicidade, garantindo a invariante a nível de banco — não apenas na camada de aplicação — por meio de constraint (`CHECK (description = btrim(description))`) combinada com normalização no *use case* de escrita, para evitar condição de corrida entre duas inserções concorrentes com strings visualmente distintas mas semanticamente iguais.
- **FR-003**: O sistema DEVE aceitar apenas descrições de categoria com até 120 caracteres (após normalização).
- **FR-004**: O sistema DEVE proibir expressamente a exclusão de qualquer categoria que possua um ou mais eventos associados (`ON DELETE RESTRICT`), retornando HTTP 409 (Conflict).
- **FR-005**: O sistema DEVE formatar todas as respostas de falha utilizando o padrão RFC 7807 (Problem Details).
- **FR-006**: O sistema DEVE implementar suíte de testes automatizados incluindo: testes unitários de validação da entidade `EventCategory` (incluindo normalização de FR-002a), testes de contrato REST, testes de integração com Testcontainers PostgreSQL e Redis, e um teste E2E cobrindo o fluxo P1 de criação e validação de unicidade.
- **FR-007**: O sistema DEVE manter cache-aside em Redis para a listagem de categorias, sob a chave `catalog:categories:list`, sem TTL fixo — invalidada explicitamente (não expirada) a cada escrita (ver FR-007a). Contrato de chave novo, não definido em `arquitetura-solucao.md` seção 10.
- **FR-007a**: O sistema DEVE invalidar a chave `catalog:categories:list` no Redis de forma síncrona, na mesma operação de escrita, sempre que uma categoria for incluída, alterada ou excluída — evitando leitura de lista desatualizada após mutação administrativa.

### Key Entities *(include if feature involves data)*

- **EventCategory (Entidade de Domínio)**:
  - `id`: UUID (Chave primária).
  - `description`: String (Obrigatório, Único após normalização/trim, até 120 caracteres).
  - `createdAt`: TIMESTAMPTZ (Data de criação).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das tentativas de exclusão de categorias com eventos associados são bloqueadas com resposta informativa HTTP 409 RFC 7807.
- **SC-002**: Zero categorias duplicadas (inclusive por diferença de espaçamento nas bordas) ou vazias no banco de dados.

> Metas de latência (antigo SC-001 desta seção) foram movidas para "Assumptions", por não terem lastro em `arquitetura-solucao.md` — são metas propostas por esta spec, não requisito herdado dos documentos de origem.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Taxa de erro não tratado (5xx) em steady-state < 0,1%.

> PR-001/PR-002 originais (150 ms escrita, 50 ms leitura cache) foram movidos para Assumptions como metas propostas, não confirmadas em documento de arquitetura.

## Assumptions

- O gerenciamento de categorias é executado no microsserviço `microservice-catalog` sobre o schema `catalog.event_category` no PostgreSQL.
- O cache de categorias é mantido em Redis para otimizar as consultas de filtros públicos de catálogo, sob a chave proposta em FR-007 (contrato novo, não herdado da arquitetura de referência).
- Autenticação e autorização via Keycloak com role `ROLE_ADMIN` para escrita; leitura pública assumida como extensão da matriz de autorização (seção 15.4), pendente de confirmação formal (ver "Extensões declaradas nesta spec", item 1).
- RN04 ("categoria ativa") é interpretada, nesta versão, como "categoria existente" — não há coluna de status/enabled em `EventCategory`. Se um conceito de categoria inativa for necessário no futuro, requer extensão de schema fora do escopo desta feature.
- Metas de latência (P95 <= 150 ms escrita, <= 50 ms leitura via cache) são propostas desta spec, não requisitos formalmente definidos em `arquitetura-solucao.md` — sujeitas a validação em teste de carga antes de virarem SLO.
- Diferenciação de caixa (case sensitivity) na unicidade de descrição não é tratada nesta versão; apenas espaços nas bordas são normalizados (FR-002a).

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001, FR-004 | `microservice-catalog_spec.md` US-CAT-09; item `[NOVO]` (proteção contra exclusão) | CRUD + `ON DELETE RESTRICT` |
| FR-002 | RN06 | Unicidade e não-nulidade |
| FR-002a | Extensão local (não herdada) | Necessária para RN06 valer sob espaços/whitespace, ausente na DDL original |
| FR-003 | DDL `catalog.event_category.description VARCHAR(120)` | — |
| FR-005 | `arquitetura-solucao.md` seção 8 (Padrão de Erro RFC 7807) | — |
| FR-007, FR-007a | Extensão local (não herdada) | Chave de cache não definida em `arquitetura-solucao.md` seção 10 |
| GET público (US4) | Extensão local | Matriz de autorização (15.4) não lista GET de `/event-categories` explicitamente |
| RN04 "ativa" | `microservice-catalog_spec.md` RN04 | Termo não modelado na entidade; assumido como "existente" |
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
