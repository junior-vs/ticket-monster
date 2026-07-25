# Feature Specification: Consultar Catalogo de Eventos Ativos

**Feature Branch**: `[001-consultar-catalogo-ativos]`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "US-CAT-01: Consultar catalogo de eventos ativos. Respect e:\\develop\\repos\\java-projects\\ticket-monster\\docs\\arch\\arquitetura-solucao.md"

## Clarifications

### Session 2026-07-25

- Q: Qual a definicao de "evento ativo" para esta feature? -> A: Evento ativo = status PUBLISHED, independentemente de ter performance futura.
- Q: Qual padrao de paginacao para consultas sem parametros explicitos? -> A: page=0, size=20, size maximo=100.
- Q: Qual modelo de consistencia para navegacao paginada enquanto eventos mudam de status? -> A: Consistencia por pagina, refletindo o estado atual no momento de cada chamada.
- Q: Qual codigo de erro para parametros invalidos de paginacao (ex.: size > 100)? -> A: 400 Bad Request com Problem Details.
- Q: Qual ordenacao padrao da listagem de eventos ativos? -> A: Data de publicacao decrescente (mais recentes primeiro).
- Q: Como tratar filtro por categoria inexistente ou malformada? -> A: Categoria inexistente ou malformada retorna 400 Bad Request; categoria valida sem eventos ativos retorna 200 com lista vazia.
- Q: A consulta de catalogo ativo exige autenticacao? -> A: Nao, a consulta e publica sem token.
- Q: Como padronizar resposta sem resultados? -> A: Retornar 200 com lista vazia e metadados de paginacao.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Listar Eventos Publicados (Priority: P1)

Como visitante, quero consultar o catalogo de eventos atualmente ativos para descobrir opcoes disponiveis de compra.

**Why this priority**: Esta e a porta de entrada da jornada de compra; sem esta consulta, nao ha descoberta de eventos.

**Independent Test**: Pode ser testado isoladamente ao solicitar a listagem publica e validar que apenas eventos ativos sao retornados com informacoes essenciais para escolha.

**Acceptance Scenarios**:

1. **Given** que existem eventos com estados PUBLISHED, DRAFT e ARCHIVED, **When** o usuario consulta o catalogo publico, **Then** somente eventos com status PUBLISHED sao exibidos.
2. **Given** que nao existem eventos ativos no momento, **When** o usuario consulta o catalogo publico, **Then** o sistema retorna lista vazia com resposta bem-sucedida e mensagem apropriada para ausencia de resultados.
3. **Given** que um evento foi publicado recentemente, **When** o usuario consulta o catalogo apos a publicacao, **Then** o evento passa a aparecer na listagem de ativos.

---

### User Story 2 - Filtrar Eventos Ativos por Categoria (Priority: P2)

Como visitante, quero filtrar eventos ativos por categoria para encontrar rapidamente o tipo de evento que me interessa.

**Why this priority**: Reduz o tempo de descoberta em catalogos grandes e melhora a conversao na jornada de compra.

**Independent Test**: Pode ser testado isoladamente aplicando filtro por categoria e validando que todos os itens retornados pertencem a categoria selecionada e estao ativos.

**Acceptance Scenarios**:

1. **Given** que existem eventos ativos em multiplas categorias, **When** o usuario aplica filtro por uma categoria especifica, **Then** apenas eventos ativos daquela categoria sao retornados.
2. **Given** uma categoria sem eventos ativos, **When** o usuario aplica esse filtro, **Then** o sistema retorna lista vazia sem erro.
3. **Given** uma categoria inexistente ou malformada, **When** o usuario aplica esse filtro, **Then** o sistema retorna 400 Bad Request com payload Problem Details.

---

### User Story 3 - Visualizar Informacoes Consistentes de Catalogo (Priority: P3)

Como visitante, quero visualizar informacoes padronizadas de cada evento ativo para comparar opcoes com confianca.

**Why this priority**: Garante experiencia consistente de leitura entre canais e reduz ambiguidade de dados exibidos.

**Independent Test**: Pode ser testado isoladamente validando que os campos essenciais da resposta estao completos e seguem o mesmo padrao para todos os eventos ativos.

**Acceptance Scenarios**:

1. **Given** que existem eventos ativos com e sem midia valida, **When** o usuario consulta a listagem, **Then** todos os eventos sao apresentados com estrutura consistente e comportamento previsivel para ausencia de midia.
2. **Given** que o usuario navega entre canal publico e canal administrativo de consulta, **When** compara os dados de eventos ativos, **Then** a semantica dos campos e criterios de visibilidade permanecem consistentes.

---

### Edge Cases

- Mudanca de status durante navegacao paginada deve refletir naturalmente nas proximas chamadas; cada pagina representa o estado atual no momento da requisicao.
- Categorias validas sem eventos ativos publicados devem retornar 200 com lista vazia e metadados de paginacao.
- Parametros de filtro ou paginacao invalidos devem retornar 400 Bad Request com payload Problem Details.
- Quando a midia promocional estiver indisponivel, a listagem deve permanecer disponivel e apresentar fallback visual padronizado.

### User Experience Consistency *(mandatory)*

- O comportamento de visibilidade de eventos ativos deve ser identico para todos os consumidores de leitura do catalogo.
- A consulta de eventos ativos deve permanecer publica, sem exigencia de token.
- A experiencia de erro deve seguir formato unico de Problem Details para erros de validacao e regras de negocio.
- Convencoes de paginacao devem usar page e size com indice de pagina base 0.
- Quando page e size nao forem informados, o padrao deve ser page=0 e size=20.
- O tamanho maximo permitido por pagina deve ser 100; acima disso retorna erro de validacao.
- A ordenacao padrao da listagem deve ser por data de publicacao em ordem decrescente.
- Campos de data e hora de agenda devem seguir um padrao unico e comparavel entre respostas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir consulta publica do catalogo de eventos ativos.
- **FR-002**: Para esta feature, evento ativo MUST ser definido como evento com status PUBLISHED.
- **FR-003**: O sistema MUST excluir da consulta publica eventos com status diferente de PUBLISHED.
- **FR-004**: O sistema MUST permitir filtro por categoria mantendo o criterio de evento ativo.
- **FR-005**: O sistema MUST tratar categoria inexistente ou malformada como erro de validacao com resposta 400 e Problem Details.
- **FR-006**: O sistema MUST retornar estrutura de dados consistente para cada item de evento ativo, com identificacao, titulo, categoria e resumo descritivo.
- **FR-007**: O sistema MUST suportar paginacao com convencao padronizada page e size, base 0.
- **FR-008**: O sistema MUST aplicar padrao page=0 e size=20 quando os parametros nao forem enviados.
- **FR-009**: O sistema MUST rejeitar size maior que 100 com erro de validacao em Problem Details.
- **FR-010**: O sistema MUST responder entradas invalidas com erro padronizado em Problem Details.
- **FR-011**: O sistema MUST definir contratos de leitura e criterios de compatibilidade antes de alterar o formato de resposta.
- **FR-012**: O sistema MUST definir cobertura de testes automatizados para regra de visibilidade de status, filtros, paginacao e cenarios de erro.
- **FR-013**: O sistema MUST garantir experiencia de leitura consistente entre canais publico e administrativo para os mesmos eventos ativos.
- **FR-014**: O sistema MUST definir metas de desempenho e resiliencia para consultas de catalogo ativo e validar seu atendimento.
- **FR-015**: O sistema MUST adotar consistencia por pagina: cada requisicao paginada retorna o estado atual no instante da chamada, sem snapshot de sessao entre paginas.
- **FR-016**: O sistema MUST retornar 400 Bad Request para parametros invalidos de paginacao e filtro, com estrutura Problem Details.
- **FR-017**: O sistema MUST aplicar ordenacao padrao por data de publicacao em ordem decrescente na listagem de eventos ativos.
- **FR-018**: O sistema MUST manter o endpoint de consulta de eventos ativos como publico sem autenticacao obrigatoria.
- **FR-019**: O sistema MUST retornar 200 com lista vazia e metadados de paginacao quando nao houver eventos ativos para os criterios consultados.

### Key Entities *(include if feature involves data)*

- **EventoAtivo**: Representa o evento visivel ao publico, com atributos de identificacao, nome, descricao resumida, categoria, referencia de midia e status de publicacao.
- **CategoriaEvento**: Representa a classificacao tematica usada para filtro, contendo identificador e descricao unica.
- **PaginaCatalogo**: Representa o resultado paginado da consulta, incluindo itens, pagina atual, tamanho da pagina e total de resultados.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos eventos exibidos na consulta publica pertencem ao conjunto de eventos ativos durante testes funcionais.
- **SC-002**: Usuarios conseguem encontrar um evento ativo relevante em ate 3 interacoes (abrir catalogo, aplicar filtro opcional, selecionar item).
- **SC-003**: Pelo menos 95% das consultas com filtro por categoria retornam resultados corretos ou lista vazia apropriada sem erro de regra.
- **SC-004**: Incidentes de inconsistencia entre canais para exibicao de eventos ativos sao reduzidos a zero no ciclo de validacao da release.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Consultas de catalogo ativo atendem p95 de latencia menor ou igual a 250 ms no perfil de carga acordado.
- **PR-002**: O servico sustenta o volume de consultas definido para picos de navegacao sem violar os criterios funcionais de filtragem e paginacao.
- **PR-003**: Taxa de falha server-side em consultas de catalogo ativo permanece abaixo de 1% em testes de carga estavel.
- **PR-004**: Em degradacao parcial de dependencia de midia, o catalogo continua retornando eventos ativos com fallback de apresentacao sem indisponibilizar a consulta.

## Assumptions

- O ciclo de vida do evento ja distingue claramente quando um evento esta ativo para exibicao publica.
- O escopo desta feature cobre leitura e filtro do catalogo ativo; manutencao administrativa de eventos fica fora deste incremento.
- Consumidores do catalogo adotam paginacao por page e size com base 0 conforme padrao de plataforma.
- Existe mecanismo de observabilidade para medir latencia, taxa de falha e volume de consultas durante validacao.
