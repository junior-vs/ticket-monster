# Feature Specification: Gerenciar Locais Físicos e Seções Estruturais (Admin)

**Feature Branch**: `010-gerenciar-locais-secoes`
**Created**: 2026-07-25
**Revised**: 2026-08-06 — revisão de grounding contra o código real (`main@a9c38aa`) e foco em especificação de negócio
**Status**: Draft
**Input**: User description: "US-CAT-10: Gerenciar locais físicos de espetáculos e suas seções estruturais (Admin). respect: docs/spec/microservice-catalog.spec.md, docs/arch/arquitetura-solucao.md"

**Origem**: `microservice-catalog.spec.md` — US-CAT-10, RN07, RN11, RN12.

## Estado Atual Verificado no Código (informativo, não normativo)

Esta seção existe para evitar que a próxima geração de `plan`/`tasks` repita suposições desatualizadas:

- O schema `catalog.venue` e `catalog.section` **já está aplicado** via Liquibase (`db/migration/V1.0__catalog_schema.sql`), com `capacity` como `GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED` — exatamente como esta spec exige.
- As entidades de persistência `VenueEntity`, `SectionEntity` e `VenueAddressEmbeddable` **já existem** em `catalog.domain.entity`, mapeadas corretamente (inclusive `capacity` como `insertable=false, updatable=false`).
- **Nenhum caso de uso, porta de repositório/cache, mapper, DTO ou endpoint REST de Venue/Section existe ainda.** A funcionalidade descrita nesta spec não está implementada — apenas o schema e o mapeamento de persistência estão prontos.
- A feature 009 (`event-categories`) é a referência de convenção arquitetural deste serviço e deve ser seguida (ver `plan.md`).

## Extensões, correções e riscos declarados nesta spec

1. **Capacidade é coluna gerada pelo banco (`GENERATED ALWAYS ... STORED`), não recalculada pela aplicação**: confirmado no schema aplicado. O payload de escrita MUST NOT aceitar `capacity` como campo de entrada (ver FR-005a).
2. **Exclusão individual de `Section` adicionada**: a spec original só cobria exclusão de seção como efeito cascata da exclusão do Venue. Adicionada US5 para exclusão isolada de uma seção.
3. **Risco arquitetural não resolvido — ausência de evento de propagação para `microservice-inventory`**: `microservice-inventory.spec.md` declara que `section_snapshot` é sincronizado via consumo de eventos `SectionCreated/Updated` publicados pelo `microservice-catalog`. Nenhum documento define `SectionDeleted` ou `VenueDeleted` (ou uma versão de `SectionUpdated` disparada em alteração de capacidade). Sem esse evento, o inventário pode manter snapshot de uma seção excluída ou com capacidade divergente do catálogo. Esta spec declara esse gap como risco explícito (ver "Riscos") e condiciona a entrega completa da feature à existência do evento — não resolve a lacuna arquitetural por conta própria.
4. **Contradição com "Dependências: Nenhuma" do serviço**: o edge case de validar redução de capacidade contra ingressos já vendidos implicaria consulta a dados que pertencem a `microservice-inventory`. `microservice-catalog.spec.md` declara explicitamente que o catalog não tem dependências. Esta spec remove a validação síncrona do escopo e documenta a alternativa assíncrona como decisão pendente (ver "Riscos").
5. Cenário de conflito de renomeação de Venue adicionado à US3.
6. Validação de endereço reduzida ao que a DDL de fato garante (campos nullable, sem formato de CEP definido em RN) — tratada como Assumption, não como requisito.
7. Contrato de chave de cache Redis (FR-010) explicitado, reaproveitando o namespace `catalog.cache.key-prefix` já configurado em `application.properties` (não introduz um segundo esquema de nomenclatura de cache).
8. Métricas de latência movidas para Assumptions.
9. Tabela de rastreabilidade adicionada.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar Novo Local Físico de Espetáculo (Venue) (Priority: P1)

Como um Administrador do sistema, quero cadastrar um novo local físico (Venue) com nome único, descrição e endereço completo, para disponibilizar espaços onde apresentações artísticas (shows) possam ser realizadas.

**Why this priority**: É o ponto de partida para a infraestrutura física do sistema. Sem um local físico cadastrado, não é possível criar seções de assentos nem agendar shows.

**Independent Test**: Pode ser testado de forma independente efetuando uma requisição HTTP POST para criar um Venue e verificando o retorno HTTP 201 (Created) com a entidade persistida e seu UUID gerado.

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, **When** ele envia uma solicitação de criação de Venue com nome único e não vazio e dados de endereço válidos, **Then** o sistema deve persistir o Venue e retornar HTTP 201 (Created) com o recurso criado.
2. **Given** um Administrador autenticado, **When** ele tenta cadastrar um Venue com um nome já utilizado por outro local (RN07), **Then** o sistema deve rejeitar a solicitação e retornar erro HTTP 409 (Conflict) formatado via RFC 7807 (Problem Details).
3. **Given** um Administrador autenticado, **When** ele envia um nome de Venue vazio ou contendo apenas espaços em branco (RN07), **Then** o sistema deve rejeitar o cadastro e retornar HTTP 400 (Bad Request).

---

### User Story 2 - Definir Seções Físicas Estruturais do Local (Priority: P2)

Como um Administrador do sistema, quero definir as seções físicas (ex.: "Pista Premium", "Camarote", "Balcão Nobre") dentro de um Venue, especificando o número de fileiras e a capacidade por fileira, para estipular a capacidade total do setor.

**Why this priority**: A definição de seções e assentos é indispensável para que o microsserviço de inventário possa alocar ingressos e poltronas para venda.

**Independent Test**: Pode ser testado enviando requisição HTTP POST para associar seções a um Venue, verificando se a capacidade total (`fileiras × capacidade por fileira`) (RN12) é corretamente derivada pelo banco.

**Acceptance Scenarios**:

1. **Given** um Venue cadastrado, **When** o Administrador adiciona uma nova seção especificando um nome único no local, quantidade de fileiras > 0 e capacidade por fileira > 0, **Then** o sistema deve criar a seção, o banco deve derivar automaticamente `capacity = number_of_rows * row_capacity` (RN12, coluna gerada) e o sistema deve retornar HTTP 201 (Created).
2. **Given** um Venue cadastrado, **When** o Administrador tenta adicionar duas seções com o mesmo nome dentro do mesmo Venue (RN11), **Then** o sistema deve rejeitar a segunda seção e retornar HTTP 409 (Conflict).
3. **Given** um Venue cadastrado, **When** o Administrador informa `number_of_rows` <= 0 ou `row_capacity` <= 0, **Then** o sistema deve rejeitar a requisição com HTTP 400 (Bad Request).
4. **Given** uma requisição de criação de seção, **When** o payload inclui explicitamente um valor para `capacity`, **Then** o sistema DEVE ignorar o campo — `capacity` nunca é aceito como entrada, apenas derivado.

---

### User Story 3 - Alterar Cadastro de Local ou Seção (Priority: P3)

Como um Administrador do sistema, quero atualizar as informações de um Venue (nome, descrição, endereço) ou ajustar os parâmetros de suas seções físicas, para refletir reformas ou mudanças na estrutura do local.

**Why this priority**: Garante que alterações físicas no local (ex.: expansão de um camarote) possam ser atualizadas no sistema.

**Independent Test**: Pode ser testado efetuando requisições HTTP PUT nas APIs de Venue ou Section e confirmando que a capacidade é derivada automaticamente pelo banco após a alteração de `number_of_rows`/`row_capacity`.

**Acceptance Scenarios**:

1. **Given** um Venue e suas seções cadastradas, **When** o Administrador altera o endereço do Venue ou atualiza `number_of_rows`/`row_capacity` de uma seção, **Then** o sistema deve aplicar as mudanças, o banco deve derivar a nova capacidade automaticamente (coluna gerada, RN12) e o sistema deve retornar HTTP 200 (OK).
2. **Given** uma alteração de nome de seção, **When** o novo nome entra em conflito com outra seção do mesmo Venue, **Then** o sistema deve impedir a alteração com HTTP 409 (Conflict).
3. **Given** uma alteração de nome de Venue, **When** o novo nome entra em conflito com outro Venue já cadastrado (RN07), **Then** o sistema deve impedir a alteração e retornar HTTP 409 (Conflict).

---

### User Story 4 - Excluir Local Físico sem Agendamentos (Priority: P4)

Como um Administrador do sistema, quero excluir um Venue que foi cadastrado erroneamente e não possui nenhum show agendado, para manter o cadastro de locais limpo.

**Why this priority**: Permite o expurgo de cadastros de teste ou incorretos sem violar o histórico de vendas de shows existentes.

**Independent Test**: Pode ser testado solicitando a exclusão de um Venue sem shows vinculados e confirmando a remoção em cascata das suas seções (HTTP 204 No Content).

**Acceptance Scenarios**:

1. **Given** um Venue que NÃO possui nenhum `Show` associado, **When** o Administrador solicita a sua exclusão via HTTP DELETE, **Then** o sistema deve remover o Venue e todas as suas seções físicas em cascata, retornando HTTP 204 (No Content).
2. **Given** um Venue que possui um ou mais `Show`s associados, **When** o Administrador tenta excluí-lo, **Then** o sistema deve bloquear a exclusão (`ON DELETE RESTRICT`) e retornar HTTP 409 (Conflict).

---

### User Story 5 - Excluir Seção Individual (Priority: P4)

Como um Administrador do sistema, quero excluir uma seção física isolada (sem excluir o Venue inteiro), para corrigir cadastros incorretos de seção sem afetar as demais seções do local.

**Why this priority**: A spec original só previa exclusão de seção via cascata do Venue; sem esta US, não há forma de remover uma seção cadastrada por engano em um Venue que permanece válido.

**Independent Test**: Pode ser testado criando um Venue com duas seções e excluindo apenas uma via HTTP DELETE em `/sections/{id}`, confirmando que a outra permanece intacta.

**Acceptance Scenarios**:

1. **Given** uma seção sem visibilidade de vendas conhecida pelo `microservice-catalog` (este serviço não possui essa visibilidade — ver "Riscos"), **When** o Administrador solicita a exclusão via HTTP DELETE, **Then** o sistema deve remover a seção e retornar HTTP 204 (No Content).
2. **Given** a exclusão de uma seção, **Then** o sistema deve publicar o evento de propagação correspondente (ver FR-011 / "Riscos") para que `microservice-inventory` possa reconciliar seu `section_snapshot` — pré-condição não implementada nesta feature, tratada como dependência externa.

---

### Edge Cases

- **Tentativa de criação de seção vinculada a Venue inexistente**: o sistema deve retornar HTTP 404 (Not Found).
- **Alteração de capacidade de seções com ingressos já vendidos**: esta feature NÃO realiza validação síncrona contra o estado de vendas do `microservice-inventory`, por este ser declarado como serviço sem dependências (`microservice-catalog.spec.md`, "Dependências: Nenhuma"). A alteração de `number_of_rows`/`row_capacity` é aceita no `catalog_db` independentemente do estado de vendas; a reconciliação com `inventory` depende de evento de propagação ainda não especificado (ver "Riscos").
- **Validação de campos de endereço**: `address_line`, `city`, `state`, `postal_code`, `country` são nullable na DDL de origem e não possuem RN de formato definida. Esta spec não impõe validação de formato de CEP/endereço além de tipo de dado — ver Assumptions.
- **Requisições de escrita sem credencial `ROLE_ADMIN`**: devem retornar HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: as APIs de inclusão, edição e exclusão de Venues e Seções são administrativas e exigem `ROLE_ADMIN`. As APIs de consulta pública de Venues e detalhes de seções são abertas para exibição no canal de vendas (US-CAT-04, US-CAT-05).
- **Padronização de Erros**: qualquer falha de validação ou restrição relacional é exposta via RFC 7807 (Problem Details), no mesmo formato já usado por `microservice-catalog.spec.md` seção 8 e pela feature de categorias de evento.
- **Capacidade Calculada**: a capacidade total da seção NUNCA é aceita como entrada de escrita; é um valor estritamente derivado pelo banco de dados (coluna gerada) a partir de `fileiras × capacidade por fileira` (RN12).
- **Identificadores**: Venues e Seções usam UUID v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE disponibilizar endpoints REST para gestão cadastral completa de locais físicos (Venues) e suas seções estruturais (Sections), incluindo exclusão individual de seção.
- **FR-002**: O sistema DEVE garantir que o nome de um Venue seja único em toda a base e não possa ser vazio ou composto apenas por espaços (RN07), inclusive na alteração (não apenas na criação).
- **FR-003**: O sistema DEVE garantir que o nome de uma seção física seja único dentro do contexto do seu mesmo Venue (RN11).
- **FR-004**: O sistema DEVE exigir que os campos `number_of_rows` e `row_capacity` de uma seção sejam números inteiros estritamente maiores que zero (> 0).
- **FR-005**: A capacidade total de uma seção física MUST ser derivada pelo banco de dados via coluna gerada (`GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED`), nunca calculada ou persistida pela camada de aplicação (RN12).
- **FR-005a**: O sistema MUST NOT aceitar `capacity` como campo de entrada em payloads de criação ou alteração de seção. Se recebido, o valor MUST ser ignorado pela aplicação antes de montar o comando de escrita.
- **FR-006**: O sistema DEVE proibir a exclusão de qualquer Venue vinculado a um ou mais registros de `Show` (`ON DELETE RESTRICT`), retornando HTTP 409 (Conflict).
- **FR-007**: O sistema DEVE remover automaticamente todas as seções físicas de um Venue (`ON DELETE CASCADE`) quando a exclusão de um Venue válido e desvinculado for efetuada.
- **FR-008**: O sistema DEVE formatar todas as respostas de erro segundo a especificação RFC 7807 (Problem Details), reutilizando o formato já estabelecido no serviço.
- **FR-009**: O sistema DEVE implementar cobertura de testes automatizados obrigatória: testes unitários das invariantes de domínio (incluindo rejeição de `capacity` como entrada), testes de contrato REST, testes de integração com Testcontainers PostgreSQL/Redis, e teste E2E cobrindo o fluxo P1.
- **FR-010**: O sistema DEVE manter cache-aside em Redis para leitura de Venue/Section sob as chaves `{prefix}:venue:{id}` e `{prefix}:venue:{id}:sections` (onde `{prefix}` = `catalog.cache.key-prefix`, já configurado como `catalog`), invalidadas de forma síncrona sempre que um Venue ou Seção for alterado ou excluído.
- **FR-011**: O sistema DEVE permitir a exclusão individual de uma `Section` sem exigir a exclusão do `Venue` associado (US5). A publicação de um evento de propagação (`SectionDeleted`/`SectionUpdated`) para consumo por `microservice-inventory` é uma dependência externa não coberta pela implementação desta feature — ver "Riscos".

### Key Entities *(include if feature involves data)*

- **Venue (Entidade Raiz do Agregado)**:
  - `id`: UUID (Chave primária).
  - `name`: String (Obrigatório, Único, não vazio).
  - `description`: Text (Descrição do espaço).
  - `addressLine`, `city`, `state`, `postalCode`, `country`: nullable, sem RN de formato.
  - `createdAt`: TIMESTAMPTZ.
- **Section (Entidade Filha de Venue)**:
  - `id`: UUID (Chave primária).
  - `venueId`: UUID (FK para Venue, ON DELETE CASCADE).
  - `name`: String (Obrigatório, Único por Venue).
  - `numberOfRows`: Int (> 0).
  - `rowCapacity`: Int (> 0).
  - `capacity`: Int (Somente leitura — coluna gerada pelo banco; não é campo de entrada em nenhuma operação de escrita).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos valores de capacidade de seções refletem exatamente `fileiras × assentos por fileira`, garantidos pelo banco (coluna gerada), sem intervenção manual da aplicação.
- **SC-002**: 100% das tentativas de cadastro com nomes de Venues duplicados (na criação ou alteração) ou seções duplicadas no mesmo local são bloqueadas com respostas RFC 7807 (HTTP 409 Conflict).

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Taxa de erros não tratados do servidor (5xx) em steady-state < 0,1%.

> Metas de latência (P95 de escrita, tempo de invalidação de cache) foram movidas para Assumptions — sem lastro em `arquitetura-solucao.md`.

## Riscos

- **Ausência de evento de propagação `SectionDeleted`/`SectionUpdated`/`VenueDeleted`**: `microservice-inventory.spec.md` documenta apenas o consumo de `SectionCreated/Updated` e `PerformanceScheduled` para popular `section_snapshot`/`performance_snapshot`. Não há especificação de evento para exclusão de seção/Venue nem para redução de capacidade. Sem isso, `inventory` pode reter snapshot de uma seção inexistente ou com capacidade desatualizada no `catalog_db`, criando risco de venda de assentos que não existem mais fisicamente. **Recomendação**: abrir ADR complementar antes da implementação de FR-011 em produção; até lá, a exclusão de seção/Venue nesta feature afeta apenas `catalog_db`.
- **Validação de capacidade contra vendas existentes não é responsabilidade deste serviço**: dado que `microservice-catalog` declara "Dependências: Nenhuma", esta feature não implementa nenhuma checagem síncrona ou assíncrona contra `seat_ledger`/`ticket_price` do `microservice-inventory` antes de permitir redução de `row_capacity`/`number_of_rows`. Se essa validação for exigida pelo negócio, é uma capacidade nova a ser modelada como consumo de evento pelo `inventory`, não como chamada síncrona do `catalog`.

## Assumptions

- O gerenciamento de Venues e Seções é realizado no microsserviço `microservice-catalog` operando sobre o schema `catalog.venue` e `catalog.section` no PostgreSQL, já aplicado.
- O cache das estruturas físicas de locais é mantido em Redis para acelerar a consulta pública de detalhes de locais (US-CAT-05), reaproveitando o namespace de cache já configurado no serviço.
- Campos de endereço são validados apenas quanto a tipo/tamanho de dado; não há exigência de formato de CEP por RN ou constraint de banco nesta versão.
- Metas de latência (P95 <= 200 ms escrita, invalidação de cache <= 50 ms) são propostas desta spec, não confirmadas em `arquitetura-solucao.md` — sujeitas a validação em teste de carga antes de virarem SLO.
- Autenticação e autorização providas pelo Keycloak JWT exigindo a role `ROLE_ADMIN`.

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-002 | RN07 | Estendido para cobrir também alteração, não só criação |
| FR-003 | RN11 | — |
| FR-004, FR-005, FR-005a | RN12; DDL `section.capacity GENERATED ALWAYS ... STORED` (aplicada) | Capacidade nunca é entrada nem cálculo de aplicação |
| FR-006, FR-007 | DDL `show.venue_id ON DELETE RESTRICT`, `section.venue_id ON DELETE CASCADE` (aplicada) | — |
| FR-008 | `arquitetura-solucao.md` seção 8; convenção já em produção na feature de categorias | RFC 7807 |
| FR-010 | Extensão do namespace de cache já configurado (`catalog.cache.key-prefix`) | Chave específica de Venue/Section, não definida em `arquitetura-solucao.md` seção 10 |
| FR-011 / US5 | Gap identificado — não coberto por `microservice-catalog.spec.md` | Exclusão individual de seção; depende de evento de propagação não especificado |
| Risco de propagação `inventory` | `microservice-inventory.spec.md` (Notas de Operação, sincronização de snapshots) | Gap arquitetural cross-serviço, não resolvido nesta feature |
| Remoção de validação síncrona de vendas | `microservice-catalog.spec.md`, "Dependências: Nenhuma" | Contradição identificada e removida do escopo |