# Feature Specification: Cadastrar Itens de Mídia com Validação e Fallback (Admin)

**Feature Branch**: `012-cadastrar-itens-midia`  
**Created**: 2026-07-25  
**Status**: Draft  
**Input**: User description: "* **US-CAT-12:** Cadastrar novos itens de mídia com validação síncrona de URL (Admin). respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar Novo Item de Mídia com Validação e Fallback (Priority: P1)

Como um Administrador do sistema, quero cadastrar um novo item de mídia promocional informando a URL e o tipo de mídia, para que o sistema valide a estrutura da URL e aplique automaticamente uma imagem de fallback em caso de inacessibilidade remota, garantindo que o catálogo nunca exiba links quebrados.

**Why this priority**: Imagens e banners promocionais são indispensáveis para a atratividade visual do catálogo de vendas de eventos. O mecanismo de fallback garante resiliência visual mesmo quando servidores externos de imagem falham.

**Independent Test**: Pode ser testado de forma independente efetuando requisições HTTP POST para criar um `MediaItem` com uma URL válida. Testar dois cenários: 1) URL remota acessível (`fallback_applied = false`), 2) URL remota inacessível (`fallback_applied = true` e indicação do arquivo local de fallback `not_available.jpg`).

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, **When** ele envia uma solicitação de criação de item de mídia com uma URL no formato `http://` ou `https://` válida e acessível (RN37) e tipo `IMAGE`, **Then** o sistema deve salvar o item com `fallback_applied = false` e retornar HTTP 201 (Created) com os dados cadastrados.
2. **Given** um Administrador autenticado, **When** ele cadastra uma URL estruturalmente válida, porém cujo servidor remoto não responde (timeout ou erro HTTP 404/500) (CA-CAT-03-MED), **Then** o sistema NÃO DEVE travar o cadastro; DEVE persistir a URL original, marcar `fallback_applied = true`, associar o fallback local (`not_available.jpg`) (RN35) e retornar HTTP 201 (Created).
3. **Given** um Administrador autenticado, **When** ele tenta cadastrar um item de mídia com uma URL que já existe na base de dados (RN37), **Then** o sistema deve rejeitar o cadastro e retornar erro HTTP 409 (Conflict) formatado via RFC 7807 (Problem Details).
4. **Given** um Administrador autenticado, **When** ele envia um esquema de URL inválido (ex.: `ftp://` ou texto malformatado) (RN37), **Then** o sistema deve rejeitar a requisição e retornar HTTP 400 (Bad Request).

---

### User Story 2 - Suportar Catálogo Extensível de Tipos de Mídia (Vídeo/Áudio) (Priority: P2)

Como um Administrador do sistema, quero cadastrar itens de mídia promocional de diferentes tipos (como vídeo ou áudio), utilizando um catálogo de tipos extensível, para enriquecer o acervo promocional dos eventos sem exigir alterações de código no microsserviço.

**Why this priority**: Atende à evolução `[ALTERA RN34]` e US-CAT-14, permitindo que mídias em vídeo ou áudio sejam vinculadas a atrações artísticas.

**Independent Test**: Pode ser testado cadastrando um `MediaItem` com `media_type_code = 'VIDEO'` habilitado no banco e confirmando a persistência com o tipo correto.

**Acceptance Scenarios**:

1. **Given** o tipo de mídia `VIDEO` ativo no catálogo de tipos (`media_type_catalog`), **When** o Administrador cadastra um item de mídia especificando `media_type_code = 'VIDEO'`, **Then** o sistema deve validar a existência do tipo no catálogo de domínio e cadastrar o recurso com HTTP 201 (Created).
2. **Given** uma solicitação de cadastro, **When** o Administrador informa um código de tipo de mídia inexistente ou desabilitado (ex.: `DOCX`), **Then** o sistema deve retornar HTTP 400 (Bad Request).

---

### User Story 3 - Consultar e Remover Itens de Mídia (Priority: P3)

Como um Administrador do sistema, quero listar itens de mídia cadastrados e remover itens obsoletos que não estão mais em uso, para manter o acervo de mídias organizado.

**Why this priority**: Permite o gerenciamento e reutilização de itens de mídia cadastrados no painel administrativo.

**Independent Test**: Pode ser testado listando itens de mídia via HTTP GET e excluindo um item desvinculado de eventos via HTTP DELETE.

**Acceptance Scenarios**:

1. **Given** um item de mídia cadastrado que não possui vínculos com nenhum evento (`media_item_id`), **When** o Administrador envia uma solicitação HTTP DELETE, **Then** o sistema deve remover o item de mídia e retornar HTTP 204 (No Content).
2. **Given** um item de mídia vinculado a um evento ativo, **When** o Administrador tenta excluí-lo, **Then** o sistema deve impedir a exclusão (`ON DELETE SET NULL` ou `ON DELETE RESTRICT`) e retornar a resposta adequada.

---

### Edge Cases

- **Timeout no check síncrono da URL remota**: O tempo limite para a resolução HTTP da URL remota durante o cadastro não deve ultrapassar 250 ms. Se estourar o timeout, a requisição é tratada como falha de resolução remota e a mídia é gravada com `fallback_applied = true` sem falhar o endpoint do admin (CA-CAT-03-MED).
- **Tentativa de cadastro de URL com esquema proibido**: URLs que não iniciem por `http://` ou `https://` devem ser rejeitadas via constraint no banco de dados (`ck_media_item_url_scheme`) e validação prévia de domínio (RN37).
- **Concorrência ao tentar cadastrar a mesma URL simultaneamente**: O banco de dados garante a unicidade via `uq_media_item_url` retornando HTTP 409 Conflict.
- **Acesso sem a role `ROLE_ADMIN`**: Deve retornar HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: As operações de escrita (POST, DELETE) de itens de mídia são restritas a `ROLE_ADMIN`. A leitura e consumo das URLs tratadas/fallback são expostas ao catálogo público.
- **Transparência de Fallback**: O cliente da API recebe no objeto retornado da mídia os campos `url` (URL original persistida), `fallbackApplied` (booleano) e `cachedFileName` (caminho para o fallback local `not_available.jpg` se `fallbackApplied == true`).
- **Formato de Erros**: Qualquer violação de validação é exposta via RFC 7807 (Problem Details).
- **Identificadores**: Itens de mídia utilizam UUID v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE fornecer endpoints REST administrativos restritos a `ROLE_ADMIN` para cadastro, consulta e exclusão de itens de mídia (`MediaItem`).
- **FR-002**: O sistema DEVE exigir obrigatoriamente que a URL de um item de mídia possua esquema `http://` ou `https://` e seja única em toda a base de dados (RN37), retornando HTTP 409 (Conflict) para URLs duplicadas e HTTP 400 (Bad Request) para esquemas inválidos.
- **FR-003**: O sistema DEVE realizar verificação síncrona de acessibilidade da URL remota durante o cadastro. Caso a resolução remota falhe ou estoure o tempo limite de 250 ms, o sistema DEVE persistir a URL original, definir `fallback_applied = true` e vincular a imagem de fallback local (`not_available.jpg`) sem interromper ou travar o cadastro (RN35, CA-CAT-03-MED).
- **FR-004**: O sistema DEVE suportar um catálogo extensível de tipos de mídia (`IMAGE`, `VIDEO`, `AUDIO`, etc.) gerenciado pela tabela de domínio `media_type_catalog`, validando os tipos de mídia aceitos sem necessidade de alteração de código ou redeploy (`[ALTERA RN34]`, US-CAT-14).
- **FR-005**: O sistema DEVE responder com o formato estrito RFC 7807 (Problem Details) para todas as ocorrências de erros de validação ou conflitos de URL.
- **FR-006**: O sistema DEVE incluir suite de testes automatizados completa: testes unitários da lógica de validação de URL e fallback, testes de contrato REST, testes de integração com Testcontainers PostgreSQL e Redis, e um teste E2E cobrindo a jornada P1 com mock de servidor remoto (sucesso e falha de download).
- **FR-007**: O sistema DEVE atualizar ou invalidar o cache Redis de itens de mídia sempre que um item for alterado ou removido.

### Key Entities *(include if feature involves data)*

- **MediaTypeCatalog (Tabela de Domínio Extensível)**:
  - `code`: VARCHAR(30) (Chave primária, ex.: 'IMAGE', 'VIDEO', 'AUDIO').
  - `description`: VARCHAR(120) (Descrição humanizada do tipo).
  - `enabled`: BOOLEAN (Indica se o tipo de mídia está ativo para novos cadastros).
- **MediaItem (Entidade de Domínio)**:
  - `id`: UUID (Chave primária).
  - `mediaTypeCode`: VARCHAR(30) (FK para MediaTypeCatalog, Obrigatório).
  - `url`: VARCHAR(2048) (Obrigatório, Único, esquemas HTTP/HTTPS).
  - `cachedFileName`: VARCHAR(255) (Nome do arquivo de fallback local, ex.: 'not_available.jpg').
  - `fallbackApplied`: BOOLEAN (Obrigatório, Padrão: FALSE).
  - `createdAt`: TIMESTAMPTZ.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O cadastro de itens de mídia é concluído com tempo de resposta P95 <= 300 ms (incluindo a verificação síncrona com timeout de 250 ms).
- **SC-002**: 100% das URLs remotas inacessíveis são salvas com sucesso marcadas com `fallback_applied = true`, sem gerar erros HTTP 500 no cliente.
- **SC-003**: 100% das tentativas de cadastro de URLs duplicadas ou formatos de URL inválidos são rejeitadas com erros amigáveis RFC 7807.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Tempo limite (timeout) para a verificação síncrona da URL remota fixado em no máximo 250 ms.
- **PR-002**: Latência P95 das operações de escrita de mídia <= 300 ms.
- **PR-003**: Taxa de erros não tratados do servidor (5xx) em steady-state < 0,1%.

## Assumptions

- O cadastro e validação de mídias é executado no microsserviço `microservice-catalog` utilizando o schema `catalog.media_item` e `catalog.media_type_catalog` no PostgreSQL (`catalog_db`).
- O arquivo de imagem de fallback padrão (`not_available.jpg`) está previamente empacotado nos assets locais do serviço.
- Autenticação e autorização providas pelo Keycloak JWT exigindo a role `ROLE_ADMIN`.
