# Feature Specification: Gerenciar Cadastro de Eventos (Admin)

**Feature Branch**: `008-gerenciar-cadastro-eventos`  
**Created**: 2026-07-25  
**Status**: Draft  
**Input**: User description: "* **US-CAT-08:** Gerenciar cadastro de eventos (Inclusão, Alteração, Exclusão) (Admin). respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar Novo Evento em Modo Rascunho (Priority: P1)

Como um Administrador do sistema, quero cadastrar um novo evento com nome, descrição, categoria e mídia opcional no catálogo, para que o evento possa ser preparado e revisado antes de sua disponibilização pública.

**Why this priority**: É a jornada fundamental para a entrada de novas atrações artísticas e conteúdo de vendas na plataforma. Sem o cadastro inicial em rascunho (`DRAFT`), nenhum show ou performance pode ser agendado.

**Independent Test**: Pode ser testado de forma independente efetuando a requisição HTTP POST para criação de evento no endpoint administrativo com credenciais de `ROLE_ADMIN` e verificando a persistência no banco de dados com o status `DRAFT`, sem visibilidade no catálogo público.

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN` e uma categoria de evento ativa existente, **When** ele envia uma solicitação de inclusão de evento com nome único entre 5 e 50 caracteres e descrição entre 20 e 1000 caracteres, **Then** o sistema deve criar o evento com status `DRAFT`, atribuir um UUID único e retornar confirmação com status HTTP 201 (Created) e o recurso criado.
2. **Given** um Administrador autenticado, **When** ele tenta cadastrar um evento informando um nome que já existe na base de dados (RN01), **Then** o sistema deve rejeitar o cadastro e retornar erro HTTP 409 (Conflict) formatado via RFC 7807 (Problem Details).
3. **Given** um Administrador autenticado, **When** ele envia dados violando as restrições de tamanho (nome < 5 ou > 50 chars, ou descrição < 20 ou > 1000 chars) (RN02, RN03), **Then** o sistema deve retornar HTTP 400 (Bad Request) com a lista de violações nos detalhes da RFC 7807.

---

### User Story 2 - Alterar Dados de Evento Existente (Priority: P2)

Como um Administrador do sistema, quero atualizar as informações de um evento existente (nome, descrição, categoria ou mídia), para manter o catálogo artístico sempre correto e atualizado.

**Why this priority**: Permite corrigir digitações, atualizar descrições artísticas ou trocar itens de mídia promocional de eventos já cadastrados.

**Independent Test**: Pode ser testado de forma independente realizando alterações via HTTP PUT/PATCH em um evento cadastrado e confirmando que as novas informações são refletidas na consulta administrativa por ID.

**Acceptance Scenarios**:

1. **Given** um evento previamente cadastrado, **When** o Administrador envia novos dados válidos (nome único, descrição no tamanho permitido, categoria existente), **Then** o sistema deve atualizar o evento, atualizar o campo de data de modificação (`updated_at`) e retornar HTTP 200 (OK) com os dados atualizados.
2. **Given** um evento existente, **When** o Administrador tenta alterar seu nome para um nome já utilizado por outro evento, **Then** o sistema deve impedir a alteração e retornar HTTP 409 (Conflict).

---

### User Story 3 - Transicionar Ciclo de Vida do Evento (Publicar / Arquivar) (Priority: P3)

Como um Administrador do sistema, quero alterar o estado de um evento de `DRAFT` para `PUBLISHED` (para torná-lo visível no catálogo público) ou para `ARCHIVED` (para desativá-lo), garantindo o controle explícito da divulgação.

**Why this priority**: Evita a exibição precoce de eventos incompletos no portal público de vendas e permite encerrar o ciclo de exibição de atrações passadas.

**Independent Test**: Pode ser testado alternando o status do evento para `PUBLISHED` e verificando que ele passa a figurar nas respostas das APIs de consulta pública (`US-CAT-01`), e subsequentemente alterando para `ARCHIVED` e confirmando sua remoção da listagem pública.

**Acceptance Scenarios**:

1. **Given** um evento em estado `DRAFT` com dados válidos e categoria ativa, **When** o Administrador solicita sua publicação, **Then** o sistema deve alterar seu estado para `PUBLISHED`, registrar o carimbo de data/hora de publicação (`published_at`) e invalidar/atualizar o cache público Redis.
2. **Given** um evento em estado `PUBLISHED`, **When** o Administrador solicita seu arquivamento, **Then** o sistema deve alterar seu estado para `ARCHIVED` e removê-lo imediatamente das respostas do catálogo público.

---

### User Story 4 - Excluir Evento Sem Vinculações (Priority: P4)

Como um Administrador do sistema, quero excluir um evento que foi cadastrado indevidamente e não possui shows ou agendamentos vinculados, para manter a base limpa de registros rascunho descartados.

**Why this priority**: Permite a limpeza administrativa de cadastros acidentais antes que entrem no fluxo de vendas ou agendamento de locais.

**Independent Test**: Pode ser testado enviando uma solicitação de exclusão HTTP DELETE para um evento sem vínculos e confirmando que a consulta posterior retorna HTTP 404 (Not Found).

**Acceptance Scenarios**:

1. **Given** um evento em estado `DRAFT` que não possui nenhum `Show` associado, **When** o Administrador envia uma solicitação de exclusão HTTP DELETE, **Then** o sistema deve remover o evento e retornar HTTP 204 (No Content).
2. **Given** um evento que já possui um ou mais `Show`s associados, **When** o Administrador solicita a sua exclusão, **Then** o sistema deve bloquear a operação e retornar HTTP 409 (Conflict) indicando a restrição de integridade referencial.

---

### Edge Cases

- **Tentativa de cadastro com categoria inexistente ou inativa (RN04)**: O sistema deve validar a existência do `event_category_id` e retornar HTTP 400 (Bad Request) ou 404 (Not Found) detalhando que a categoria informada é inválida.
- **Tentativa de vinculação a item de mídia inexistente (RN05)**: Se um `media_item_id` for informado, o sistema deve verificar sua existência no banco/catálogo de mídias antes de efetuar a gravação.
- **Concorrência ao alterar o mesmo evento**: Atualizações simultâneas devem utilizar controle de concorrência (ex.: campo de versão ou validação de concorrência otimista) para evitar sobreescrita silenciosa (*lost update*).
- **Inativação de Categoria com Eventos Vinculados**: O banco/serviço deve impor restrição (`ON DELETE RESTRICT`) impedindo a remoção de categorias vinculadas a eventos ativos.

### User Experience Consistency *(mandatory)*

- **Canais**: As operações de inclusão, alteração, publicação e exclusão são exclusivas da API administrativa protegida. A API pública expõe apenas a leitura de eventos no estado `PUBLISHED`.
- **Autorização e Autenticação**: Solicitantes sem token JWT válido recebem HTTP 401 (Unauthorized). Solicitantes autenticados sem a role `ROLE_ADMIN` recebem HTTP 403 (Forbidden).
- **Representação de Erros**: Todos os erros de validação e de regras de negócio (RN01, RN02, RN03, RN04) devem ser retornados no formato estrito RFC 7807 (Problem Details), contendo `type`, `title`, `status`, `detail` e `instance`.
- **Paginação e Ordenação**: A API de listagem administrativa de eventos deve suportar paginação base zero (`page=0`, `size=20`), ordenação por `name` ou `createdAt`, e filtros por `status`, `name` e `eventCategoryId`.
- **Identificadores**: Todos os IDs de evento são UUIDs padrão v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE fornecer endpoints administrativos restritos a usuários com a role `ROLE_ADMIN` para gerenciamento completo do ciclo de vida de eventos.
- **FR-002**: O sistema DEVE permitir a criação de um evento com nome (5 a 50 caracteres), descrição (20 a 1000 caracteres), ID de categoria ativa obrigatória e ID de item de mídia promocional opcional. O estado inicial do evento será obrigatoriamente `DRAFT`.
- **FR-003**: O sistema DEVE garantir a unicidade do nome do evento em toda a base de dados (RN01), retornando HTTP 409 (Conflict) em caso de tentativa de duplicidade.
- **FR-004**: O sistema DEVE validar rigorosamente o tamanho do nome (RN02: 5 a 50 caracteres) e da descrição (RN03: 20 a 1000 caracteres) antes de persistir as alterações.
- **FR-005**: O sistema DEVE permitir a alteração dos dados cadastrais (nome, descrição, categoria e item de mídia) de eventos existentes.
- **FR-006**: O sistema DEVE suportar transições de estado explícitas no ciclo de vida do evento: `DRAFT` → `PUBLISHED` → `ARCHIVED`. Eventos que não estejam no estado `PUBLISHED` NÃO PODEM ser retornados pelas consultas do catálogo público de vendas (US-CAT-01).
- **FR-007**: O sistema DEVE permitir a exclusão física de um evento somente se ele estiver no estado `DRAFT` e não possuir nenhum `Show` associado. Caso existam vínculos, a exclusão deve ser rejeitada com HTTP 409 (Conflict).
- **FR-008**: O sistema DEVE definir previamente os contratos de API REST (OpenAPI) e schemas de erro RFC 7807 antes do desenvolvimento das alterações.
- **FR-009**: O sistema DEVE possuir suite de testes automatizados completa: testes unitários para regras do domínio de evento, testes de contrato para a API administrativa REST, testes de integração utilizando Testcontainers para PostgreSQL e Redis, e um teste end-to-end cobrindo a jornada P1 de cadastro e publicação.
- **FR-010**: O sistema DEVE emitir invalidação ou atualização no cache Redis do catálogo sempre que o status de um evento for alterado para ou de `PUBLISHED`.

### Key Entities *(include if feature involves data)*

- **Event (Entidade Raiz do Agregado Catalog)**:
  - `id`: UUID (Chave primária).
  - `name`: String (Obrigatório, Único, 5 a 50 caracteres).
  - `description`: String (Obrigatório, 20 a 1000 caracteres).
  - `eventCategoryId`: UUID (FK para EventCategory, Obrigatório).
  - `mediaItemId`: UUID (FK para MediaItem, Opcional).
  - `status`: Enum (`DRAFT`, `PUBLISHED`, `ARCHIVED`, Padrão: `DRAFT`).
  - `createdAt`: TIMESTAMPTZ (Data de criação).
  - `updatedAt`: TIMESTAMPTZ (Data da última modificação).
  - `publishedAt`: TIMESTAMPTZ (Data de publicação pública, nulo se DRAFT).
- **EventCategory (Entidade de Domínio)**:
  - `id`: UUID (Chave primária).
  - `description`: String (Descrição única da categoria, ex.: "Rock", "Teatro").
- **MediaItem (Entidade de Mídia Opcional)**:
  - `id`: UUID (Chave primária).
  - `url`: String (URL da imagem/mídia promocional).
  - `mediaTypeCode`: String (Tipo de mídia, ex.: "IMAGE").

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Administradores conseguem cadastrar, atualizar e alterar o estado de um evento em menos de 2 segundos de tempo total de interação na interface/API.
- **SC-002**: 100% das falhas de validação de tamanho de texto e duplicidade de nome retornam respostas estruturadas no padrão RFC 7807 contendo descrições claras dos erros.
- **SC-003**: Zero vazamento de eventos em estado `DRAFT` ou `ARCHIVED` nas APIs públicas do catálogo de vendas.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Latência de resposta (P95) das operações administrativas de gravação e atualização de eventos <= 200 ms.
- **PR-002**: A API administrativa deve suportar até 50 requisições simultâneas de alteração sem provocar degradação no tempo de resposta do catálogo público de leitura.
- **PR-003**: Taxa de erros não tratados do servidor (HTTP 5xx) mantida em < 0,1% durante execuções de testes de carga estável.
- **PR-004**: Invalidação do cache Redis de catálogo concluída em menos de 50 ms após a publicação ou alteração de um evento `PUBLISHED`.

## Assumptions

- O gerenciamento de categorias de eventos (US-CAT-09) e mídias (US-CAT-12) possui APIs próprias, sendo utilizadas aqui por referência via UUIDs existentes.
- O microsserviço responsável pela implementação é o `microservice-catalog` utilizando o banco de dados PostgreSQL (`catalog_db`) e cache Redis.
- A autenticação de administradores é provida via token OAuth2/JWT emitido pelo Keycloak, contendo a role `ROLE_ADMIN` no claim `realm_access.roles`.
- As regras de negócio RN01, RN02, RN03, RN04 e RN05 são aplicadas rigorosamente pela camada de domínio da aplicação Quarkus.
