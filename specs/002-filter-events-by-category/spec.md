# Feature Specification: US-CAT-02 — Filtrar Eventos Catalogados por Categoria de Interesse

**Feature Branch**: `002-filter-events-by-category`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "US-CAT-02: Filtrar eventos catalogados por categoria de interesse. respect E:\develop\repos\java-projects\ticket-monster\docs\arch\arquitetura-solucao.md"

**Origem**: `microservice-catalog` — US-CAT-02, RN01, RN04, alinhado com `arquitetura-solucao.md` (seções 4, 6.1, 8, 10, 15.4, 25.3) e Constituição da Modernização (Princípios I, II, IV, V).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Filtrar Eventos Publicados por Categoria Existente (Priority: P1)

Como visitante da plataforma TicketMonster, quero filtrar os eventos catalogados selecionando uma categoria de meu interesse (ex.: Shows, Teatro, Esportes) para encontrar rapidamente atrações do tipo desejado.

**Why this priority**: O filtro por categoria é uma funcionalidade essencial de navegação (P1). Permite que o usuário reduza a lista total de eventos para uma subseleção relevante, aumentando a conversão na descoberta de ingressos.

**Independent Test**: Pode ser testado de forma independente realizando uma requisição de leitura pública ao catálogo fornecendo um identificador de categoria válido e verificando que todos os eventos retornados pertencem exclusivamente àquela categoria e possuem `status = PUBLISHED`.

**Acceptance Scenarios**:

1. **Given** que existem eventos publicados cadastrados nas categorias "Shows", "Teatro" e "Esportes", **When** o visitante aplica o filtro pela categoria "Teatro", **Then** o sistema retorna 200 OK exibindo apenas os eventos publicados vinculados à categoria "Teatro".
2. **Given** que um evento pertence à categoria "Shows" mas está com status `DRAFT` ou `ARCHIVED`, **When** o visitante filtra pela categoria "Shows", **Then** o sistema exclui esse evento da listagem retornada.
3. **Given** que o visitante não informa parâmetro de categoria na consulta de eventos, **When** a requisição é processada, **Then** o sistema retorna a listagem geral de todos os eventos publicados (comportamento padrão de navegação).

---

### User Story 2 - Tratar Categorias Sem Eventos e Validação de Parâmetros de Filtro (Priority: P2)

Como sistema de catálogo de alto desempenho, quero validar criteriosamente os parâmetros do filtro de categoria e tratar graciosamente categorias sem eventos para fornecer respostas determinísticas e sem ambiguidades aos clientes HTTP.

**Why this priority**: Garante a resiliência da API (P2), previne exceções não tratadas no servidor e assegura a adesão ao padrão de erro RFC 7807 para integradores e clientes web.

**Independent Test**: Pode ser testado isoladamente enviando requisições com identificadores de categoria inexistentes, malformados ou para categorias sem eventos associados, validando os códigos de status HTTP e o formato Problem Details.

**Acceptance Scenarios**:

1. **Given** uma categoria válida e cadastrada no sistema que no momento não possui nenhum evento com status `PUBLISHED`, **When** o visitante filtra por essa categoria, **Then** o sistema responde 200 OK com uma lista vazia (`[]`) e os metadados de paginação corretos (`totalElements: 0`, `totalPages: 0`).
2. **Given** um parâmetro de categoria com formato sintaticamente malformado (ex.: texto quando se espera ID numérico/UUID), **When** o visitante executa a consulta, **Then** o sistema responde 400 Bad Request com estrutura RFC 7807 Problem Details detalhando o parâmetro inválido.
3. **Given** um identificador de categoria sintaticamente válido mas que não existe no cadastro de `EventCategory`, **When** o visitante executa a consulta, **Then** o sistema responde 400 Bad Request com estrutura RFC 7807 Problem Details indicando categoria não encontrada.

---

### User Story 3 - Paginação e Ordenação Consistente dos Resultados Filtrados (Priority: P3)

Como visitante navegando em uma categoria com grande volume de eventos, quero navegar de forma paginada e previsível pelos resultados filtrados para visualizar a lista completa sem perda de desempenho.

**Why this priority**: Suporta a usabilidade em categorias populares com muitos eventos (P3) e garante a conformidade com o Princípio IV da Constituição (UX consistente com `page`/`size` base 0).

**Independent Test**: Pode ser testado isoladamente aplicando o filtro por categoria juntamente com parâmetros de paginação (`page`, `size`) e ordenação, validando os metadados e os itens de cada página.

**Acceptance Scenarios**:

1. **Given** uma categoria com 35 eventos publicados, **When** o visitante solicita a primeira página filtrada com `page=0` e `size=20`, **Then** o sistema retorna os 20 primeiros eventos mais recentes daquela categoria, informando `totalPages: 2` e `totalElements: 35`.
2. **Given** os resultados de uma consulta filtrada por categoria, **When** o visitante consulta os eventos sem especificar a ordenação, **Then** os eventos são exibidos em ordem decrescente pela data de publicação (`published_at`).
3. **Given** um parâmetro de paginação inválido como `size=150` (acima do limite máximo de 100), **When** o visitante executa a requisição, **Then** o sistema rejeita a chamada com 400 Bad Request no formato RFC 7807 Problem Details.

---

### Edge Cases

- **Alteração de Status Durante Navegação**: Se um evento da categoria filtrada for despublicado (`PUBLISHED` -> `ARCHIVED`) durante a navegação entre páginas, a próxima requisição de página refletirá o estado atualizado (consistência por página, sem lock de sessão).
- **Exclusão/Inativação de Categoria**: Se a categoria for removida ou inativada no cadastro master de catálogo, consultas com o ID dessa categoria passam a retornar 400 Bad Request (categoria inexistente).
- **Eventos com Múltiplas Categorias**: Na arquitetura de referência, cada evento pertence a exatamente uma `EventCategory` primária. Filtros por múltiplas categorias simultâneas na mesma requisição estão fora do escopo da v1.
- **Degradação de Cache no Redis**: Caso o Redis fique indisponível, a consulta por categoria deve ser executada no PostgreSQL (`catalog_db`), mantendo os mesmos contratos HTTP sem retornar erro 500.

---

### User Experience Consistency *(mandatory)*

- **Canal Público Sem Autenticação**: O endpoint de filtragem por categoria (`GET /api/v1/events?categoryId={id}`) DEVE ser público e não exigir token Bearer JWT (`arquitetura-solucao.md`, seção 15.4).
- **Formato de Erro**: Todos os erros 400 (parâmetros malformados, categoria inexistente, paginação inválida) DEVEM retornar JSON no padrão RFC 7807 Problem Details.
- **Convenção de Paginação**: Paginação DEVE utilizar os parâmetros query `page` (base 0) e `size` (padrão 20, máximo 100).
- **Ordenação Padrão**: A ordenação padrão dos resultados filtrados DEVE ser por `published_at` decrescente (eventos publicados mais recentemente primeiro).
- **Representação Temporal**: Campos de data/hora na resposta DEVEM utilizar o padrão ISO 8601 / `timestamptz`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST disponibilizar endpoint público no `microservice-catalog` para consultar eventos filtrados por categoria (`categoryId`).
- **FR-002**: O sistema MUST retornar na consulta filtrada por categoria apenas eventos com `status = PUBLISHED`.
- **FR-003**: O sistema MUST filtrar rigorosamente a lista excluindo eventos em rascunho (`DRAFT`) ou arquivados (`ARCHIVED`).
- **FR-004**: O sistema MUST validar a existência da categoria informada no cadastro de `EventCategory` antes de processar a busca.
- **FR-005**: O sistema MUST responder HTTP 400 Bad Request no formato RFC 7807 quando o parâmetro de categoria for sintaticamente malformado ou inexistente no banco de dados.
- **FR-006**: O sistema MUST responder HTTP 200 OK com array vazio (`[]`) e metadados de paginação zerados quando a categoria for válida mas não possuir eventos publicados.
- **FR-007**: O sistema MUST aplicar paginação `page` (base 0) e `size` (padrão 20, máximo 100) sobre o conjunto de eventos filtrados por categoria.
- **FR-008**: O sistema MUST aplicar ordenação padrão decrescente pelo campo `published_at` para todos os eventos da categoria filtrada.
- **FR-009**: O sistema MUST aceitar chamadas de filtragem por categoria sem exigir cabeçalho de autorização JWT (`ROLE_CUSTOMER` ou `ROLE_ADMIN`).
- **FR-010**: O sistema MUST definir suite de testes automatizados completa para a funcionalidade:
  - Testes unitários no agregado de domínio para filtragem e ordenação.
  - Testes de contrato REST verificando o schema JSON e Problem Details.
  - Testes de integração via Testcontainers (PostgreSQL `catalog_db` + Redis).
  - Teste end-to-end (E2E) cobrindo a jornada de filtragem P1.
- **FR-011**: O sistema MUST definir e monitorar orçamentos de desempenho para a rota de filtragem por categoria (p95 <= 250ms sob carga).

---

### Key Entities *(include if feature involves data)*

- **EventCategory (Categoria de Evento)**: Agregado/Entidade de catálogo que possui identificador único (`id`), nome exclusivo (`name`), código/slug (`code`) e descrição.
- **Event (Evento)**: Agregado raiz de catálogo contendo identificador único (`id`), título (`title`), descrição (`description`), status (`status` = DRAFT | PUBLISHED | ARCHIVED), referência para `EventCategory`, referência de mídia (`mediaItem`) e timestamp de publicação (`published_at`).
- **FilteredEventPage (Página de Eventos Filtrados)**: Value Object que engloba a lista de eventos DTO pertencentes à categoria e os metadados de paginação (`page`, `size`, `totalElements`, `totalPages`).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos eventos retornados no filtro por categoria possuem `status = PUBLISHED` e estão vinculados à categoria solicitada.
- **SC-002**: Visitantes conseguem filtrar e visualizar eventos de uma categoria com tempo de resposta imperceptível (interação completa em até 2 passos no frontend).
- **SC-003**: 100% das chamadas com categorias inexistentes ou parâmetros malformados resultam em respostas RFC 7807 400 Bad Request válidas.
- **SC-004**: Zero regressões nos contratos REST do `microservice-catalog` validados por testes de contrato automatizados.

---

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: A latência p95 para requisições de eventos filtrados por categoria DEVE ser <= 250 ms sob o perfil de carga definido (utilizando estratégia Cache-Aside no Redis para a chave `catalog:category:{id}:events`).
- **PR-002**: O endpoint DEVE sustentar a vazão de pico esperada sem exceder a taxa de erro de 1% no lado do servidor (5xx).
- **PR-003**: A taxa de falha server-side em steady-state DEVE permanecer em < 1%.
- **PR-004**: Em caso de falha de conexão com o Redis, a aplicação DEVE realizar fallback transparente de consulta diretamente no PostgreSQL (`catalog_db`), mantendo a disponibilidade do serviço.

---

## Assumptions

- O `microservice-catalog` gerencia a tabela de categorias (`event_category`) e eventos (`event`) em seu banco de dados isolado `catalog_db`.
- A estratégia de cache distribuído em Redis utiliza chaves do tipo `catalog:category:{id}:events` com expiração (TTL) de 1 hora, invalidada via eventos de atualização de catálogo (`arquitetura-solucao.md`, seção 10).
- O parâmetro query `categoryId` será a convenção padrão para passar o identificador da categoria no endpoint de listagem de eventos (`GET /api/v1/events?categoryId={id}`).
- Erros de validação seguem rigorosamente a convenção RFC 7807 Problem Details estabelecida na seção 8 de `arquitetura-solucao.md` e na Constituição.

---

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001, FR-002, FR-003 | `microservice-catalog` US-CAT-02 | Filtro por categoria para eventos publicados |
| FR-004, FR-005, FR-006 | `arquitetura-solucao.md` seção 8 (RFC 7807) | Validação e erro de categoria |
| FR-007, FR-008 | `arquitetura-solucao.md` seção 25.3 (RN42) | Paginação `page`/`size` base 0 e ordenação |
| FR-009 | `arquitetura-solucao.md` seção 15.4 | Acesso público ao catálogo GET `/events` |
| FR-010 | Constituição da Modernização, Princípio III | Testes unitários, de contrato, integração e E2E |
| PR-001, PR-004 | Constituição da Modernização, Princípio V & `arquitetura-solucao.md` seção 10 | Meta de latência p95 <= 250ms e fallback Redis |
