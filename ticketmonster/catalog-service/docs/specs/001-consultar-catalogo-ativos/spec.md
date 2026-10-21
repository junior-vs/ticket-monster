# Feature Specification: Consultar Catálogo de Eventos Publicados

**Feature Branch**: `[001-consultar-catalogo-ativos]`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "US-CAT-01: Consultar catalogo de eventos ativos. Respect e:\\develop\\repos\\java-projects\\ticket-monster\\docs\\arch\\arquitetura-solucao.md"

**Origem**: `microservice-catalog_spec.md` — US-CAT-01, RN01, RN04, item `[NOVO]` de ciclo de vida (`arquitetura-solucao.md`, seção 4 / modelo de dados do Catalog).

> **Nota de terminologia:** este documento usa "evento publicado" em vez de "evento ativo". O termo "ativo" é reaproveitado com semântica diferente em `microservice-telemetry_spec.md` (RN31/RN32 — show com performance futura). Para evitar ambiguidade entre Bounded Contexts, esta feature evita o termo "ativo" fora de citação literal da US original.

## Clarifications

### Session 2026-07-25

- Q: Qual a definição de "evento ativo" para esta feature? -> A: Evento publicado = `status = PUBLISHED`, independentemente de ter performance futura. Não confundir com o critério de "ativo" usado em métricas de Telemetry (RN31/RN32), que filtra por performance futura — critério de outro Bounded Context, não aplicável a esta consulta de catálogo.
- Q: Qual padrão de paginação para consultas sem parâmetros explícitos? -> A: `page=0`, `size=20`, size máximo=100.
- Q: Qual modelo de consistência para navegação paginada enquanto eventos mudam de status? -> A: Consistência por página, refletindo o estado atual no momento de cada chamada.
- Q: Qual código de erro para parâmetros inválidos de paginação (ex.: `size > 100`)? -> A: 400 Bad Request com Problem Details.
- Q: Qual ordenação padrão da listagem de eventos publicados? -> A: Data de publicação decrescente (mais recentes primeiro).
- Q: Como tratar filtro por categoria inexistente ou malformada? -> A: Categoria com identificador sintaticamente inválido (malformada) ou identificador válido sem registro correspondente (inexistente) retornam ambos 400 Bad Request; categoria válida existente sem eventos publicados retorna 200 com lista vazia.
- Q: A consulta de catálogo publicado exige autenticação? -> A: Não, a consulta é pública sem token.
- Q: Como padronizar resposta sem resultados? -> A: Retornar 200 com lista vazia e metadados de paginação.
- Q: O que garante a ordenação por data de publicação quando o evento não possui essa data preenchida? -> A: A transição de um evento para `PUBLISHED` é obrigada a preencher `published_at`; não existe estado `PUBLISHED` com `published_at` nulo (ver FR-020).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Listar Eventos Publicados (Priority: P1)

Como visitante, quero consultar o catálogo de eventos atualmente publicados para descobrir opções disponíveis de compra.

**Why this priority**: Esta é a porta de entrada da jornada de compra; sem esta consulta, não há descoberta de eventos.

**Independent Test**: Pode ser testado isoladamente ao solicitar a listagem pública e validar que apenas eventos com `status = PUBLISHED` são retornados com informações essenciais para escolha.

**Acceptance Scenarios**:

1. **Given** que existem eventos com estados PUBLISHED, DRAFT e ARCHIVED, **When** o usuário consulta o catálogo público, **Then** somente eventos com status PUBLISHED são exibidos.
2. **Given** que não existem eventos publicados no momento, **When** o usuário consulta o catálogo público, **Then** o sistema retorna lista vazia com resposta bem-sucedida e metadados de paginação.
3. **Given** que um evento foi publicado recentemente, **When** o usuário consulta o catálogo após a publicação, **Then** o evento passa a aparecer na listagem, posicionado conforme a ordenação por `published_at` decrescente.

---

### User Story 2 - Filtrar Eventos Publicados por Categoria (Priority: P2)

Como visitante, quero filtrar eventos publicados por categoria para encontrar rapidamente o tipo de evento que me interessa.

**Why this priority**: Reduz o tempo de descoberta em catálogos grandes e melhora a conversão na jornada de compra.

**Independent Test**: Pode ser testado isoladamente aplicando filtro por categoria e validando que todos os itens retornados pertencem à categoria selecionada e estão publicados.

**Acceptance Scenarios**:

1. **Given** que existem eventos publicados em múltiplas categorias, **When** o usuário aplica filtro por uma categoria específica existente, **Then** apenas eventos publicados daquela categoria são retornados.
2. **Given** uma categoria existente sem eventos publicados, **When** o usuário aplica esse filtro, **Then** o sistema retorna 200 com lista vazia e metadados de paginação, sem erro.
3. **Given** um identificador de categoria sintaticamente malformado, **When** o usuário aplica esse filtro, **Then** o sistema retorna 400 Bad Request com payload Problem Details.
4. **Given** um identificador de categoria sintaticamente válido mas sem `EventCategory` correspondente cadastrada, **When** o usuário aplica esse filtro, **Then** o sistema retorna 400 Bad Request com payload Problem Details.

---

### User Story 3 - Visualizar Informações Consistentes de Catálogo (Priority: P3)

Como visitante, quero visualizar informações padronizadas de cada evento publicado para comparar opções com confiança.

**Why this priority**: Garante experiência consistente de leitura entre canais e reduz ambiguidade de dados exibidos.

**Independent Test**: Pode ser testado isoladamente validando que os campos essenciais da resposta estão completos e seguem o mesmo padrão para todos os eventos publicados.

**Acceptance Scenarios**:

1. **Given** que existem eventos publicados com e sem mídia válida, **When** o usuário consulta a listagem, **Then** todos os eventos são apresentados com estrutura consistente e comportamento previsível para ausência de mídia (RN35 — fallback local).
2. **Given** que o usuário navega entre canal público e canal administrativo de consulta, **When** compara os dados de eventos publicados, **Then** a semântica dos campos e critérios de visibilidade permanecem consistentes.

---

### Edge Cases

- Mudança de status durante navegação paginada deve refletir naturalmente nas próximas chamadas; cada página representa o estado atual no momento da requisição (sem snapshot de sessão).
- Categorias existentes sem eventos publicados devem retornar 200 com lista vazia e metadados de paginação.
- Parâmetros de filtro ou paginação inválidos (malformados ou inexistentes) devem retornar 400 Bad Request com payload Problem Details.
- Quando a mídia promocional estiver indisponível, a listagem deve permanecer disponível e apresentar fallback visual padronizado (RN35).
- Evento em `PUBLISHED` sem `published_at` preenchido é um estado inválido — não deve ocorrer (garantido por FR-020); se ocorrer por inconsistência de dados legados, tratar como defeito de dados, não como comportamento esperado da consulta.

### User Experience Consistency *(mandatory)*

- O comportamento de visibilidade de eventos publicados deve ser idêntico para todos os consumidores de leitura do catálogo.
- A consulta de eventos publicados deve permanecer pública, sem exigência de token.
- A experiência de erro deve seguir formato único de Problem Details para erros de validação e regras de negócio.
- Convenções de paginação devem usar `page` e `size` com índice de página base 0.
- Quando `page` e `size` não forem informados, o padrão deve ser `page=0` e `size=20`.
- O tamanho máximo permitido por página deve ser 100; acima disso retorna erro de validação.
- A ordenação padrão da listagem deve ser por `published_at` em ordem decrescente.
- Campos de data e hora devem seguir um padrão único e comparável entre respostas (ISO 8601 / `timestamptz`).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir consulta pública do catálogo de eventos publicados.
- **FR-002**: Para esta feature, evento elegível para exibição pública MUST ser definido como evento com `status = PUBLISHED`. Este critério é local ao Catalog Context e não deve ser confundido com o critério de "ativo" usado em `microservice-telemetry` (RN31/RN32).
- **FR-003**: O sistema MUST excluir da consulta pública eventos com status diferente de PUBLISHED (DRAFT, ARCHIVED).
- **FR-004**: O sistema MUST permitir filtro por categoria mantendo o critério de evento publicado.
- **FR-005**: O sistema MUST tratar identificador de categoria sintaticamente malformado como erro de validação (400, Problem Details).
- **FR-005a**: O sistema MUST tratar identificador de categoria sintaticamente válido, porém sem `EventCategory` correspondente cadastrada, como erro de validação (400, Problem Details) — distinto do caso de categoria válida sem eventos publicados, que retorna 200 com lista vazia.
- **FR-006**: O sistema MUST retornar estrutura de dados consistente para cada item de evento publicado, com identificação, título, categoria e resumo descritivo.
- **FR-007**: O sistema MUST suportar paginação com convenção padronizada `page` e `size`, base 0.
- **FR-008**: O sistema MUST aplicar padrão `page=0` e `size=20` quando os parâmetros não forem enviados.
- **FR-009**: O sistema MUST rejeitar `size` maior que 100 com erro de validação em Problem Details.
- **FR-010**: O sistema MUST responder entradas inválidas com erro padronizado em Problem Details.
- **FR-012**: O sistema MUST definir cobertura de testes automatizados para regra de visibilidade de status, filtros, paginação e cenários de erro.
- **FR-013**: O sistema MUST garantir experiência de leitura consistente entre canais público e administrativo para os mesmos eventos publicados.
- **FR-014**: O sistema MUST definir metas de desempenho e resiliência para consultas de catálogo publicado e validar seu atendimento (ver premissa de cache em Assumptions).
- **FR-015**: O sistema MUST adotar consistência por página: cada requisição paginada retorna o estado atual no instante da chamada, sem snapshot de sessão entre páginas.
- **FR-016**: O sistema MUST retornar 400 Bad Request para parâmetros inválidos de paginação e filtro, com estrutura Problem Details.
- **FR-017**: O sistema MUST aplicar ordenação padrão por `published_at` em ordem decrescente na listagem de eventos publicados.
- **FR-018**: O sistema MUST manter o endpoint de consulta de eventos publicados como público, sem autenticação obrigatória.
- **FR-019**: O sistema MUST retornar 200 com lista vazia e metadados de paginação quando não houver eventos publicados para os critérios consultados.
- **FR-020**: O sistema MUST garantir, na transição de um evento para `status = PUBLISHED`, o preenchimento de `published_at` com o instante da transição — pré-condição necessária para a ordenação determinística de FR-017. Regra de aplicação (`PublishEventUseCase`), não de schema.

> **Removido desta feature:** o requisito de "definir contratos de leitura e critérios de compatibilidade antes de alterar formato de resposta" (política de versionamento de API) não é comportamento desta US, não possui cenário de aceite correspondente e não é medido por nenhum Success Criteria aqui. Deve ser tratado como constraint/NFR de governança do `microservice-catalog`, não como FR desta feature.

### Key Entities *(include if feature involves data)*

- **EventoPublicado**: Representa o evento visível ao público, com atributos de identificação, nome, descrição resumida, categoria, referência de mídia, status de publicação e `published_at`.
- **CategoriaEvento**: Representa a classificação temática usada para filtro, contendo identificador e descrição única.
- **PaginaCatalogo**: Representa o resultado paginado da consulta, incluindo itens, página atual, tamanho da página e total de resultados.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos eventos exibidos na consulta pública pertencem ao conjunto de eventos com `status = PUBLISHED` durante testes funcionais.
- **SC-002**: Usuários conseguem encontrar um evento publicado relevante em até 3 interações (abrir catálogo, aplicar filtro opcional, selecionar item).
- **SC-003**: Pelo menos 95% das consultas com filtro por categoria retornam resultados corretos ou lista vazia apropriada sem erro de regra.
- **SC-004**: Incidentes de inconsistência entre canais para exibição de eventos publicados são reduzidos a zero no ciclo de validação da release.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Consultas de catálogo publicado atendem p95 de latência menor ou igual a 250 ms no perfil de carga acordado, condicionado à premissa de cache-aside descrita em Assumptions.
- **PR-002**: O serviço sustenta o volume de consultas definido para picos de navegação sem violar os critérios funcionais de filtragem e paginação.
- **PR-003**: Taxa de falha server-side em consultas de catálogo publicado permanece abaixo de 1% em testes de carga estável.
- **PR-004**: Em degradação parcial de dependência de mídia, o catálogo continua retornando eventos publicados com fallback de apresentação, sem indisponibilizar a consulta.

## Assumptions

- O ciclo de vida do evento (`DRAFT` → `PUBLISHED` → `ARCHIVED`) já distingue claramente quando um evento está publicado para exibição pública (`arquitetura-solucao.md`, item `[NOVO]` do Catalog).
- A meta de latência (PR-001) depende do padrão Cache-Aside no Redis descrito em `arquitetura-solucao.md`, seção 10 (`catalog:event:{id}`, `catalog:shows:performance:{id}`); sem cache-aside ativo, a meta de p95 não é garantida apenas pelo PostgreSQL.
- O escopo desta feature cobre leitura e filtro do catálogo publicado; manutenção administrativa de eventos (transições de status, CRUD) fica fora deste incremento.
- Consumidores do catálogo adotam paginação por `page` e `size` com base 0 conforme padrão de plataforma (corrige RN42 as-is — ver `arquitetura-solucao.md`, seção 25.3).
- Existe mecanismo de observabilidade (OpenTelemetry/Prometheus, seção 14) para medir latência, taxa de falha e volume de consultas durante validação.

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001 a FR-003 | `microservice-catalog_spec.md` US-CAT-01; `arquitetura-solucao.md` item `[NOVO]` ciclo de vida do evento | Critério de "publicado" substitui a definição original ambígua de "ativo" |
| FR-004, FR-005, FR-005a | `microservice-catalog_spec.md` US-CAT-02 | Filtro por categoria |
| FR-007 a FR-009, FR-015, FR-016 | `arquitetura-solucao.md` seção 25.3 (correção de RN42 as-is) | Paginação `page`/`size` base 0 |
| FR-017, FR-020 | Novo (não coberto explicitamente em `microservice-catalog_spec.md`) | Necessário para suportar ordenação determinística |
| FR-018 | `arquitetura-solucao.md` seção 15.4 — matriz de autorização (`catalog GET /events` público) | — |
| RN35 (mídia fallback) | `projeto-legado.md` RN35; `microservice-catalog_spec.md` CA-CAT-03-MED | Reaproveitada sem alteração |
| PR-001 | `arquitetura-solucao.md` seção 10 (Redis Cache-Aside) | Meta condicionada à premissa de cache |