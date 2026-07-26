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

---

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
