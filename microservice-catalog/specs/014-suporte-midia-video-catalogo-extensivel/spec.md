# Feature Specification: Suporte a Mídia em Vídeo via Catálogo Extensível

**Feature Branch**: `014-suporte-midia-video-catalogo-extensivel`  
**Created**: 2026-07-25  
**Status**: Draft  
**Input**: User description: "* **US-CAT-14 (nova):** Como administrador, quero cadastrar mídia em vídeo além de imagem, sem depender de alteração de código do serviço. respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gerenciar Catálogo Extensível de Tipos de Mídia (Priority: P1)

Como um Administrador do sistema, quero cadastrar e habilitar novos tipos de mídia (como `VIDEO`, `AUDIO` ou `STREAM`) no catálogo de tipos de mídia do banco de dados, para expor novas opções de formato promocional para atrações artísticas sem necessitar de alteração de código Java ou redeploy da aplicação.

**Why this priority**: Substitui a limitação do sistema legado onde os tipos de mídia eram definidos em um Enum fechado e rígido (`IMAGE`), impedindo a inclusão de trailers em vídeo ou teasers em áudio.

**Independent Test**: Pode ser testado de forma independente incluindo um novo registro na tabela de domínio `media_type_catalog` via API administrativa (`POST /api/v1/media-types`) com o código `VIDEO` e verificando que a opção passa a ser aceita imediatamente na validação de cadastro de mídias.

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, **When** ele cadastra ou habilita um novo tipo de mídia (ex.: `code = 'VIDEO'`, `description = 'Vídeo Promocional/Teaser'`, `enabled = true`), **Then** o sistema deve persistir o tipo de mídia no catálogo de domínio e retornar HTTP 201 (Created).
2. **Given** um tipo de mídia desabilitado (`enabled = false`), **When** o Administrador tenta cadastrar um novo item de mídia usando esse tipo, **Then** o sistema deve recusar o cadastro e retornar HTTP 400 (Bad Request) via RFC 7807 (Problem Details).

---# Feature Specification: Suporte a Mídia em Vídeo via Catálogo Extensível

**Feature Branch**: `014-suporte-midia-video-catalogo-extensivel`
**Created**: 2026-07-25
**Status**: Draft
**Input**: User description: "* **US-CAT-14 (nova):** Como administrador, quero cadastrar mídia em vídeo além de imagem, sem depender de alteração de código do serviço. respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

**Origem**: `microservice-catalog_spec.md` — US-CAT-14, `[ALTERA RN34]`, US-CAT-12 (RN35, RN37).

## Extensões, correções e decisões cruzadas declaradas nesta spec

1. **Referência cruzada com US-CAT-12 (resolve contradição)**: a spec de US-CAT-12 declarou explicitamente que a inclusão de novos `media_type_code` é "operação de migração de banco/deploy, fora do escopo de API" daquela feature. **Esta spec (US-CAT-14) é quem assume essa responsabilidade**: aqui é definida a API administrativa de gestão do catálogo de tipos (FR-002). A spec de US-CAT-12 deve ser lida como escopo de *consumo* de `media_type_code`; esta spec (US-CAT-14) é o escopo de *gestão* desse catálogo. Recomenda-se atualizar a Assumption equivalente em US-CAT-12 para apontar para esta feature.
2. **Seed inicial corrigido**: a DDL de origem semeia apenas `IMAGE` via migração (`INSERT ... VALUES ('IMAGE', ...)`, herdado do legado). `VIDEO` **não** é pré-semeado por Liquibase nesta spec — é inserido em runtime via a API definida em FR-002, para que o teste E2E de FR-007 comprove de fato a extensibilidade sem deploy (contradição da versão anterior, onde `VIDEO` viria pronto na migração, esvaziando SC-001).
3. **Exclusão de tipo de mídia declarada como fora de escopo intencionalmente** (FR-002a) — não é omissão, é decisão de design (evita o mesmo risco de integridade já tratado para `EventCategory`).
4. **Validação de formato de `code` adicionada** (FR-001a) — ausente na DDL/RN de origem.
5. **Autorização de `/media-types` marcada como extensão**, não decisão já confirmada na matriz 15.4.
6. **`STREAM` removido dos exemplos** — fora do escopo de `[ALTERA RN34]`, que define apenas `IMAGE`, `VIDEO`, `AUDIO`.
7. **FR-001 reescrito** para deixar claro que o schema `media_type_catalog` é pré-existente (baseline de `microservice-catalog_spec.md`); o entregável novo desta feature é a API de gestão.
8. **Estratégia de cache do catálogo de tipos declarada** (FR-008) — tabela de referência de baixa mutação, cache local em memória por instância, invalidado por evento interno de escrita.
9. Métricas de latência movidas para Assumptions.
10. Tabela de rastreabilidade adicionada.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gerenciar Catálogo Extensível de Tipos de Mídia (Priority: P1)

Como um Administrador do sistema, quero cadastrar e habilitar/desabilitar tipos de mídia (como `VIDEO` ou `AUDIO`) no catálogo de tipos de mídia do banco de dados, para expor novas opções de formato promocional para atrações artísticas sem necessitar de alteração de código Java ou redeploy da aplicação.

**Why this priority**: Substitui a limitação do sistema legado onde os tipos de mídia eram definidos em um Enum fechado e rígido (`IMAGE`), impedindo a inclusão de trailers em vídeo ou teasers em áudio.

**Independent Test**: Pode ser testado de forma independente incluindo um novo registro na tabela de domínio `media_type_catalog` via API administrativa (`POST /api/v1/media-types`) com o código `VIDEO` — sem qualquer alteração de código ou deploy — e verificando que a opção passa a ser aceita imediatamente na validação de cadastro de mídias (US-CAT-12).

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, **When** ele cadastra um novo tipo de mídia (ex.: `code = 'VIDEO'`, `description = 'Vídeo Promocional/Teaser'`, `enabled = true`), **Then** o sistema deve persistir o tipo de mídia no catálogo de domínio e retornar HTTP 201 (Created).
2. **Given** um tipo de mídia desabilitado (`enabled = false`), **When** o Administrador tenta cadastrar um novo item de mídia usando esse tipo (via US-CAT-12), **Then** o sistema deve recusar o cadastro e retornar HTTP 400 (Bad Request) via RFC 7807 (Problem Details).
3. **Given** um Administrador autenticado, **When** ele tenta cadastrar um tipo de mídia com `code` em formato inválido (ex.: contendo espaços, minúsculas ou caracteres especiais), **Then** o sistema deve rejeitar a requisição e retornar HTTP 400 (Bad Request).
4. **Given** um Administrador autenticado, **When** ele tenta cadastrar um `code` já existente no catálogo, **Then** o sistema deve retornar HTTP 409 (Conflict).

---

### User Story 2 - Cadastrar Mídia em Vídeo para Eventos (Priority: P2)

Como um Administrador do sistema, quero cadastrar um item de mídia especificando o tipo `VIDEO` e sua URL, para vincular um teaser ou trailer em vídeo a um evento no catálogo.

**Why this priority**: Aumenta o engajamento dos compradores ao permitir a exibição de prévias audiovisuais dos espetáculos no portal público de vendas.

**Independent Test**: Pode ser testado enviando requisição HTTP POST para criar um `MediaItem` com `media_type_code = 'VIDEO'` e URL de vídeo válida, verificando o retorno HTTP 201 (Created) e a associação correta ao tipo `VIDEO`. Esta US reaproveita integralmente a validação de URL/fallback definida em US-CAT-12 (FR-002/FR-003 daquela spec); nenhuma regra nova de URL é introduzida aqui.

**Acceptance Scenarios**:

1. **Given** o tipo `VIDEO` cadastrado e habilitado no catálogo de tipos de mídia, **When** o Administrador envia uma solicitação de criação de item de mídia informando a URL do vídeo e `media_type_code = 'VIDEO'`, **Then** o sistema deve aplicar as validações de URL (RN37) e fallback (RN35) já especificadas em US-CAT-12, salvar o item de mídia e retornar HTTP 201 (Created).
2. **Given** um cadastro de mídia em vídeo, **When** a URL fornecida for duplicada (RN37) ou possuir esquema inválido, **Then** o sistema deve retornar HTTP 409 (Conflict) ou HTTP 400 (Bad Request) respectivamente.

---

### User Story 3 - Exibir Mídia em Vídeo no Catálogo Público (Priority: P3)

Como um Consumidor do Catálogo, quero visualizar o tipo de mídia (`VIDEO` ou `IMAGE`) juntamente com a URL ao consultar os detalhes de um evento, para que a interface de usuário possa renderizar o componente adequado (player de vídeo ou visualizador de imagem).

**Why this priority**: Garante que o canal público (SPA/Mobile) saiba exatamente como renderizar a mídia promocional sem ambiguidades.

**Independent Test**: Pode ser testado consultando um evento com mídia do tipo `VIDEO` através da API pública (`US-CAT-03`) e confirmando a presença do campo `mediaTypeCode: "VIDEO"` no payload de resposta.

**Acceptance Scenarios**:

1. **Given** um evento associado a um item de mídia de vídeo, **When** o cliente consulta os detalhes do evento na API pública, **Then** a resposta HTTP 200 (OK) deve retornar os dados da mídia contendo a URL, o indicador de fallback e o código do tipo de mídia (`mediaTypeCode = 'VIDEO'`).

---

### Edge Cases

- **Tentativa de cadastro de mídia com código inexistente**: o sistema deve checar a chave estrangeira e a validação de domínio em `media_type_catalog` e retornar HTTP 400 (Bad Request) — comportamento herdado de US-CAT-12, não redefinido aqui.
- **Desativação de um tipo de mídia com mídias existentes**: desativar um tipo (`enabled = false`) impede *novos* cadastros daquele tipo, mas não corrompe nem exclui os itens de mídia históricos existentes.
- **Tentativa de excluir um tipo de mídia**: não há endpoint de exclusão (`DELETE`) para `media_type_catalog` nesta feature — desativação é o único mecanismo de remoção operacional (ver FR-002a).
- **Falha de download/resolução da URL do vídeo**: o mecanismo de fallback (RN35, CA-CAT-03-MED, especificado em US-CAT-12) é acionado da mesma forma, marcando `fallback_applied = true` e atribuindo o asset de fallback local sem falhar o cadastro.
- **Acesso não autorizado aos endpoints de configuração de tipos de mídia**: retorna HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: a gestão do catálogo de tipos de mídia (`media_type_catalog`) é exclusiva de `ROLE_ADMIN` — extensão desta spec sobre a matriz de autorização (seção 15.4), que não lista `/media-types` explicitamente. A consulta pública de eventos expõe o código do tipo de mídia para consumo transparente do frontend.
- **Formato de Erros**: qualquer violação de validação de tipo de mídia é exposta no padrão RFC 7807 (Problem Details).
- **Semântica Extensível**: o modelo de dados desacopla completamente os tipos de mídia de enums do código Java, permitindo que a inclusão de novos tipos ocorra sem necessidade de deploy (`[ALTERA RN34]`) — comprovado por FR-007/SC-001 usando `VIDEO` inserido em runtime, não pré-semeado.
- **Identificadores**: tipos de mídia usam códigos textuais únicos em maiúsculas (ex.: `IMAGE`, `VIDEO`, `AUDIO`). Itens de mídia usam UUID v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE expor gestão administrativa sobre a tabela de domínio `media_type_catalog` (`catalog.media_type_catalog`) — schema pré-existente definido em `microservice-catalog_spec.md` (`[ALTERA RN34]`), com os campos `code`, `description` e `enabled`. O entregável desta feature é a API de gestão (FR-002), não a criação do schema.
- **FR-001a**: O sistema DEVE validar que `code` siga o formato `^[A-Z_]{2,30}$` (maiúsculas e underscore, 2 a 30 caracteres), rejeitando com HTTP 400 valores fora desse padrão.
- **FR-002**: O sistema DEVE fornecer endpoints REST administrativos protegidos com `ROLE_ADMIN` para listar, cadastrar e alterar o status (`enabled`) de tipos de mídia.
- **FR-002a**: O sistema NÃO DEVE expor endpoint de exclusão (`DELETE`) para `media_type_catalog` — decisão intencional de design, para preservar integridade referencial de `MediaItem` já cadastrados com um `code` desativado. Desativação (`enabled = false`) é o único mecanismo de remoção operacional.
- **FR-003**: O sistema DEVE validar que todo novo `MediaItem` cadastrado (via US-CAT-12) especifique um `media_type_code` válido e ativo (`enabled = true`) na tabela `media_type_catalog`.
- **FR-004**: O sistema DEVE permitir a criação de itens de mídia do tipo `VIDEO` aplicando-lhes as mesmas regras de validação de URL (RN37) e resiliência com fallback (RN35, CA-CAT-03-MED) já especificadas em US-CAT-12, sem duplicar ou redefinir essa lógica.
- **FR-005**: O sistema DEVE retornar o código do tipo de mídia (`mediaTypeCode`) nas respostas da API pública de consulta de eventos (US-CAT-03) e consulta de catálogo.
- **FR-006**: O sistema DEVE retornar respostas de falha formatadas segundo a especificação RFC 7807 (Problem Details).
- **FR-007**: O sistema DEVE contar com suíte de testes automatizados incluindo: testes unitários do validador de formato de `code` e do catálogo de mídia, testes de contrato REST, testes de integração com Testcontainers PostgreSQL/Redis, e um teste E2E que insira `VIDEO` em runtime via API (não via seed) e comprove seu uso imediato em um cadastro de `MediaItem`, sem redeploy de código.
- **FR-008**: O sistema DEVE manter `media_type_catalog` em cache local em memória por instância (tabela de referência, baixíssima taxa de mutação), invalidado de forma síncrona a cada escrita (POST/PATCH) via evento interno — evita round-trip a Redis/Postgres a cada validação de `MediaItem`. Contrato de invalidação novo, não definido em `arquitetura-solucao.md` seção 10.

### Key Entities *(include if feature involves data)*

- **MediaTypeCatalog (Tabela de Domínio Extensível — schema pré-existente, gestão via API nesta feature)**:
  - `code`: VARCHAR(30) (Chave primária, formato `^[A-Z_]{2,30}$`, ex.: 'IMAGE', 'VIDEO', 'AUDIO').
  - `description`: VARCHAR(120) (Descrição humanizada, ex.: 'Vídeo Promocional/Teaser').
  - `enabled`: BOOLEAN (Obrigatório, Padrão: TRUE).
- **MediaItem (Entidade de Mídia — definida e gerida em US-CAT-12, referenciada aqui)**:
  - `id`: UUID (Chave primária).
  - `mediaTypeCode`: VARCHAR(30) (FK para MediaTypeCatalog, Obrigatório).
  - `url`: VARCHAR(2048) (Obrigatório, Único, esquemas HTTP/HTTPS).
  - `cachedFileName`: VARCHAR(255).
  - `fallbackApplied`: BOOLEAN (Obrigatório, Padrão: FALSE).
  - `createdAt`: TIMESTAMPTZ.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A adição e habilitação de um novo tipo de mídia (como `VIDEO`) é realizada em runtime via API, com zero linhas de código Java alteradas e zero redeploys de serviço — comprovado por teste E2E que não depende de seed prévio do tipo testado.
- **SC-002**: 100% das requisições de criação de mídia em vídeo válidas retornam HTTP 201 Created e são expostas com sucesso na API pública.
- **SC-003**: 100% das tentativas de uso de tipos de mídia desabilitados, inexistentes ou com `code` em formato inválido são bloqueadas com respostas RFC 7807 (HTTP 400/409 conforme o caso).

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Taxa de erros não tratados do servidor (5xx) em steady-state < 0,1%.

> Metas de latência específicas (configuração de tipos, leitura via cache) foram movidas para Assumptions — sem lastro em `arquitetura-solucao.md`.

## Assumptions

- A funcionalidade é implementada no microsserviço `microservice-catalog` operando sobre as tabelas `catalog.media_type_catalog` (schema pré-existente) e `catalog.media_item` (gerida por US-CAT-12) no PostgreSQL (`catalog_db`).
- O banco de dados é inicializado por padrão **apenas** com o tipo `IMAGE` na carga DDL/Liquibase (herdado do legado, RN34 as-is). `VIDEO`/`AUDIO` não são pré-semeados — são inseridos via a API definida nesta feature, para preservar a demonstração de extensibilidade sem deploy (SC-001).
- Autorização `ROLE_ADMIN` para os endpoints de `/media-types` é uma extensão proposta por esta spec sobre a matriz de autorização (`arquitetura-solucao.md` seção 15.4), que não lista esse recurso explicitamente — pendente de confirmação formal.
- Exclusão de tipo de mídia está fora de escopo por decisão de design (FR-002a), não por omissão.
- Metas de latência (P95 <= 150 ms configuração, <= 50 ms leitura via cache) são propostas desta spec, não confirmadas em `arquitetura-solucao.md` — sujeitas a validação em teste de carga antes de virarem SLO.
- Autenticação e autorização providas via Keycloak JWT com a role `ROLE_ADMIN`.

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001 | `microservice-catalog_spec.md`, `[ALTERA RN34]` (DDL `media_type_catalog`) | Schema pré-existente; reescrito para não implicar criação nova |
| FR-001a | Extensão local (não herdada) | Formato de `code` ausente na DDL/RN de origem |
| FR-002, FR-002a | US-CAT-14 (nova) | API de gestão; exclusão fora de escopo por design |
| FR-003, FR-004 | RN35, RN37 (via US-CAT-12) | Reaproveitado, não redefinido |
| FR-005 | US-CAT-03 | Exposição pública do tipo de mídia |
| FR-006 | `arquitetura-solucao.md` seção 8 | RFC 7807 |
| FR-008 | Extensão local (não herdada) | Estratégia de cache não definida em seção 10 |
| Seed apenas `IMAGE` | DDL de origem (`INSERT ... 'IMAGE' ... -- herdado do legado`) | Corrige contradição da versão anterior (seed de `VIDEO`) |
| Referência cruzada com US-CAT-12 | `microservice-catalog_spec.md` US-CAT-12 (spec já refatorada) | Resolve contradição sobre dono da API de tipos |

### User Story 2 - Cadastrar Mídia em Vídeo para Eventos (Priority: P2)

Como um Administrador do sistema, quero cadastrar um item de mídia especificando o tipo `VIDEO` e sua URL, para vincular um teaser ou trailer em vídeo a um evento no catálogo.

**Why this priority**: Aumenta o engajamento dos compradores ao permitir a exibição de prévias audiovisuais dos espetáculos no portal público de vendas.

**Independent Test**: Pode ser testado enviando requisição HTTP POST para criar um `MediaItem` com `media_type_code = 'VIDEO'` e URL de vídeo válida, verificando o retorno HTTP 201 (Created) e a associação correta ao tipo `VIDEO`.

**Acceptance Scenarios**:

1. **Given** o tipo `VIDEO` cadastrado e habilitado no catálogo de tipos de mídia, **When** o Administrador envia uma solicitação de criação de item de mídia informando a URL do vídeo e `media_type_code = 'VIDEO'`, **Then** o sistema deve aplicar as validações de URL (RN37) e fallback (RN35), salvar o item de mídia e retornar HTTP 201 (Created).
2. **Given** um cadastro de mídia em vídeo, **When** a URL fornecida for duplicada (RN37) ou possuir esquema inválido, **Then** o sistema deve retornar HTTP 409 (Conflict) ou HTTP 400 (Bad Request) respectivamente.

---

### User Story 3 - Exibir Mídia em Vídeo no Catálogo Público (Priority: P3)

Como um Consumidor do Catálogo, quero visualizar o tipo de mídia (`VIDEO` ou `IMAGE`) juntamente com a URL ao consultar os detalhes de um evento, para que a interface de usuário possa renderizar o componente adequado (player de vídeo ou visualizador de imagem).

**Why this priority**: Garante que o canal público (SPA/Mobile) saiba exatamente como renderizar a mídia promocional sem ambiguidades.

**Independent Test**: Pode ser testado consultando um evento com mídia do tipo `VIDEO` através da API pública (`US-CAT-03`) e confirmando a presença do campo `mediaTypeCode: "VIDEO"` no payload de resposta.

**Acceptance Scenarios**:

1. **Given** um evento associado a um item de mídia de vídeo, **When** o cliente consulta os detalhes do evento na API pública, **Then** a resposta HTTP 200 (OK) deve retornar os dados da mídia contendo a URL, o indicador de fallback e o código do tipo de mídia (`mediaTypeCode = 'VIDEO'`).

---

### Edge Cases

- **Tentativa de cadastro de mídia com código inexistente**: O sistema deve checar a chave estrangeira e a validação de domínio em `media_type_catalog` e retornar HTTP 400 (Bad Request).
- **Desativação de um tipo de mídia com mídias existentes**: Desativar um tipo (`enabled = false`) impede *novos* cadastros daquele tipo, mas não corrompe nem exclui os itens de mídia históricos existentes.
- **Falha de download/resolução da URL do vídeo**: O mecanismo de fallback síncrono (RN35, CA-CAT-03-MED) é acionado da mesma forma, marcando `fallback_applied = true` e atribuindo o asset de fallback local sem falhar o cadastro.
- **Acesso não autorizado aos endpoints de configuração de tipos de mídia**: Retorna HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: A gestão do catálogo de tipos de mídia (`media_type_catalog`) e criação de mídias é exclusiva de `ROLE_ADMIN`. A consulta pública de eventos expõe o código do tipo de mídia para consumo transparente do frontend.
- **Formato de Erros**: Qualquer violação de validação de tipo de mídia é exposta no padrão RFC 7807 (Problem Details).
- **Semântica Extensível**: O modelo de dados desacopla completamente os tipos de mídia de enums do código Java, permitindo que a inclusão de novos tipos ocorra sem necessidade de deploy (`[ALTERA RN34]`).
- **Identificadores**: Tipos de mídia usam códigos textuais únicos (ex.: `IMAGE`, `VIDEO`, `AUDIO`). Itens de mídia usam UUID v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE substituir o Enum fechado de mídia por uma tabela de domínio dinâmica `media_type_catalog` no PostgreSQL (`catalog.media_type_catalog`), contendo os campos `code`, `description` e `enabled` (`[ALTERA RN34]`).
- **FR-002**: O sistema DEVE fornecer endpoints REST administrativos protegidos com `ROLE_ADMIN` para listar, cadastrar e alterar o status (`enabled`) de tipos de mídia.
- **FR-003**: O sistema DEVE validar que todo novo `MediaItem` cadastrado especifique um `media_type_code` válido e ativo (`enabled = true`) na tabela `media_type_catalog`.
- **FR-004**: O sistema DEVE permitir a criação de itens de mídia do tipo `VIDEO` aplicandolhes as mesmas regras de validação de URL (RN37) e resiliência com fallback (RN35, CA-CAT-03-MED) aplicadas aos itens de imagem.
- **FR-005**: O sistema DEVE retornar o código do tipo de mídia (`mediaTypeCode`) nas respostas da API pública de consulta de eventos (US-CAT-03) e consulta de catálogo.
- **FR-006**: O sistema DEVE retornar respostas de falha formatadas segundo a especificação RFC 7807 (Problem Details).
- **FR-007**: O sistema DEVE contar com suíte de testes automatizados incluindo: testes unitários do validador de catálogo de mídia, testes de contrato REST, testes de integração com Testcontainers PostgreSQL/Redis, e teste E2E comprovando a adição de um tipo `VIDEO` sem redeploy de código.

### Key Entities *(include if feature involves data)*

- **MediaTypeCatalog (Tabela de Domínio Extensível)**:
  - `code`: VARCHAR(30) (Chave primária, ex.: 'IMAGE', 'VIDEO', 'AUDIO').
  - `description`: VARCHAR(120) (Descrição humanizada, ex.: 'Vídeo Promocional/Teaser').
  - `enabled`: BOOLEAN (Obrigatório, Padrão: TRUE).
- **MediaItem (Entidade de Mídia Atualizada)**:
  - `id`: UUID (Chave primária).
  - `mediaTypeCode`: VARCHAR(30) (FK para MediaTypeCatalog, Obrigatório).
  - `url`: VARCHAR(2048) (Obrigatório, Único, esquemas HTTP/HTTPS).
  - `cachedFileName`: VARCHAR(255).
  - `fallbackApplied`: BOOLEAN (Obrigatório, Padrão: FALSE).
  - `createdAt`: TIMESTAMPTZ.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A adição e habilitação de um novo tipo de mídia (como `VIDEO`) na aplicação é realizada com zero linhas de código alteradas e zero redeploys de serviço.
- **SC-002**: 100% das requisições de criação de mídia em vídeo válidas retornam HTTP 201 Created e são expostas com sucesso na API pública.
- **SC-003**: 100% das tentativas de uso de tipos de mídia desabilitados ou inexistentes são bloqueadas com respostas RFC 7807 (HTTP 400 Bad Request).

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Latência P95 na configuração de tipos de mídia e cadastro de itens <= 150 ms.
- **PR-002**: O catálogo de tipos de mídia mantido em cache Redis responde em <= 50 ms (P95).
- **PR-003**: Taxa de erros não tratados do servidor (5xx) em steady-state < 0,1%.

## Assumptions

- A funcionalidade é implementada no microsserviço `microservice-catalog` operando sobre as tabelas `catalog.media_type_catalog` e `catalog.media_item` no PostgreSQL (`catalog_db`).
- O banco de dados é inicializado por padrão com os tipos `IMAGE` e `VIDEO` na carga DDL via Liquibase.
- Autenticação e autorização providas via Keycloak JWT com a role `ROLE_ADMIN`.
