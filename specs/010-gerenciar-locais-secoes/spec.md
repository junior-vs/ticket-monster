# Feature Specification: Gerenciar Locais Físicos e Seções Estruturais (Admin)

**Feature Branch**: `010-gerenciar-locais-secoes`  
**Created**: 2026-07-25  
**Status**: Draft  
**Input**: User description: "* **US-CAT-10:** Gerenciar locais físicos de espetáculos e suas seções estruturais (Admin). respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

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

**Independent Test**: Pode ser testado enviando requisição HTTP POST para associar seções a um Venue, verificando se o cálculo de capacidade total (`fileiras × capacidade por fileira`) (RN12) é realizado corretamente.

**Acceptance Scenarios**:

1. **Given** um Venue cadastrado, **When** o Administrador adiciona uma nova seção especificando um nome único no local, quantidade de fileiras > 0 e capacidade por fileira > 0, **Then** o sistema deve criar a seção, calcular automaticamente a capacidade total (`capacity = number_of_rows * row_capacity`) (RN12) e retornar HTTP 201 (Created).
2. **Given** um Venue cadastrado, **When** o Administrador tenta adicionar duas seções com o mesmo nome dentro do mesmo Venue (RN11), **Then** o sistema deve rejeitar a segunda seção e retornar HTTP 409 (Conflict).
3. **Given** um Venue cadastrado, **When** o Administrador informa `number_of_rows` <= 0 ou `row_capacity` <= 0, **Then** o sistema deve rejeitar a requisição com HTTP 400 (Bad Request).

---

### User Story 3 - Alterar Cadastro de Local ou Seção (Priority: P3)

Como um Administrador do sistema, quero atualizar as informações de um Venue (nome, descrição, endereço) ou ajustar os parâmetros de suas seções físicas, para refletir reformas ou mudanças na estrutura do local.

**Why this priority**: Garante que alterações físicas no local (ex.: expansão de um camarote) possam ser atualizadas no sistema.

**Independent Test**: Pode ser testado efetuando requisições HTTP PUT/PATCH nas APIs de Venue ou Section e confirmando o recálculo imediato de capacidade e a atualização cadastral.

**Acceptance Scenarios**:

1. **Given** um Venue e suas seções cadastradas, **When** o Administrador altera o endereço do Venue ou atualiza as fileiras de uma seção, **Then** o sistema deve aplicar as mudanças, recalcular a capacidade da seção afetada e retornar HTTP 200 (OK).
2. **Given** uma alteração de nome de seção, **When** o novo nome entra em conflito com outra seção do mesmo Venue, **Then** o sistema deve impedir a alteração com HTTP 409 (Conflict).

---

### User Story 4 - Excluir Local Físico sem Agendamentos (Priority: P4)

Como um Administrador do sistema, quero excluir um Venue que foi cadastrado erroneamente e não possui nenhum show agendado, para manter o cadastro de locais limpo.

**Why this priority**: Permite o expurgo de cadastros de teste ou incorretos sem violar o histórico de vendas de shows existentes.

**Independent Test**: Pode ser testado solicitando a exclusão de um Venue sem shows vinculados e confirmando a remoção em cascata das suas seções (HTTP 204 No Content).

**Acceptance Scenarios**:

1. **Given** um Venue que NÃO possui nenhum `Show` associado, **When** o Administrador solicita a sua exclusão via HTTP DELETE, **Then** o sistema deve remover o Venue e todas as suas seções físicas em cascata, retornando HTTP 204 (No Content).
2. **Given** um Venue que possui um ou mais `Show`s associados, **When** o Administrador tenta excluí-lo, **Then** o sistema deve bloquear a exclusão (`ON DELETE RESTRICT`) e retornar HTTP 409 (Conflict).

---

### Edge Cases

- **Tentativa de criação de seção vinculada a Venue inexistente**: O sistema deve retornar HTTP 404 (Not Found).
- **Alteração das dimensões de uma seção cujas apresentações já possuem ingressos vendidos**: Alterações de capacidade física de seções com shows ativos devem ser validadas para evitar incompatibilidade com ingressos já comercializados pelo microsserviço de inventário.
- **Validação de formato de CEP/código postal e endereço**: Campos de endereço embutidos (`address_line`, `city`, `state`, `postal_code`, `country`) devem ser validados quanto a preenchimento básico.
- **Requisições de escrita sem credencial `ROLE_ADMIN`**: Devem retornar HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: As APIs de inclusão, edição e exclusão de Venues e Seções são administrativas e exigem `ROLE_ADMIN`. As APIs de consulta pública de Venues e detalhes de seções são abertas para exibição no canal de vendas (US-CAT-04, US-CAT-05).
- **Padronização de Erros**: Qualquer falha de validação ou restrição relacional é exposta via RFC 7807 (Problem Details).
- **Capacidade Calculada**: A capacidade total da seção NUNCA é informada manualmente pelo usuário na escrita; ela é um valor estritamente derivado da multiplicação de `fileiras × capacidade por fileira` (RN12).
- **Identificadores**: Venues e Seções usam UUID v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE disponibilizar endpoints REST para gestão cadastral completa de locais físicos (Venues) e suas seções estruturais (Sections).
- **FR-002**: O sistema DEVE garantir que o nome de um Venue seja único em toda a base e não possa ser vazio ou composto apenas por espaços (RN07).
- **FR-003**: O sistema DEVE garantir que o nome de uma seção física seja único dentro do contexto do seu mesmo Venue (RN11).
- **FR-004**: O sistema DEVE exigir que os campos `number_of_rows` e `row_capacity` de uma seção sejam números inteiros estritamente maiores que zero (> 0).
- **FR-005**: O sistema DEVE calcular a capacidade total de uma seção física obrigatoriamente através da multiplicação `number_of_rows * row_capacity` (RN12).
- **FR-006**: O sistema DEVE proibir a exclusão de qualquer Venue vinculado a um ou mais registros de `Show` (`ON DELETE RESTRICT`), retornando HTTP 409 (Conflict).
- **FR-007**: O sistema DEVE remover automaticamente todas as seções físicas de um Venue (`ON DELETE CASCADE`) quando a exclusão de um Venue válido e desvinculado for efetuada.
- **FR-008**: O sistema DEVE formatar todas as respostas de erro segundo a especificação RFC 7807 (Problem Details).
- **FR-009**: O sistema DEVE implementar cobertura de testes automatizados obrigatória: testes unitários do cálculo de capacidade e invariantes de domínio, testes de contrato REST, testes de integração com Testcontainers PostgreSQL/Redis, e teste E2E cobrindo o fluxo P1.
- **FR-010**: O sistema DEVE atualizar ou invalidar o cache Redis de locais e seções sempre que um Venue ou Seção for alterado ou excluído.

### Key Entities *(include if feature involves data)*

- **Venue (Entidade Raiz do Agregado)**:
  - `id`: UUID (Chave primária).
  - `name`: String (Obrigatório, Único, não vazio).
  - `description`: Text (Descrição do espaço).
  - `addressLine`: String (Logradouro e número).
  - `city`: String (Cidade).
  - `state`: String (Estado/UF).
  - `postalCode`: String (CEP/Código postal).
  - `country`: String (País).
  - `createdAt`: TIMESTAMPTZ.
- **Section (Entidade Filha de Venue)**:
  - `id`: UUID (Chave primária).
  - `venueId`: UUID (FK para Venue, ON DELETE CASCADE).
  - `name`: String (Obrigatório, Único por Venue).
  - `numberOfRows`: Int (> 0, Número de fileiras).
  - `rowCapacity`: Int (> 0, Assentos por fileira).
  - `capacity`: Int (Calculado gerado: `numberOfRows * rowCapacity`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Administradores realizam a criação de locais e seções com latência de resposta da API <= 200 ms (P95).
- **SC-002**: 100% dos cálculos de capacidade de seções são exatamente consistentes com `fileiras × assentos por fileira` sem intervenção manual.
- **SC-003**: 100% das tentativas de cadastro com nomes de Venues duplicados ou seções duplicadas no mesmo local são bloqueadas com respostas RFC 7807 (HTTP 409 Conflict).

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Latência P95 em operações de escrita/atualização de Venues e Seções <= 200 ms.
- **PR-002**: Tempo de invalidação/atualização do cache Redis de locais <= 50 ms.
- **PR-003**: Taxa de erros não tratados do servidor (5xx) em steady-state < 0,1%.

## Assumptions

- O gerenciamento de Venues e Seções é realizado no microsserviço `microservice-catalog` operando sobre o schema `catalog.venue` e `catalog.section` no PostgreSQL.
- O cache das estruturas físicas de locais é mantido em Redis para acelerar a consulta pública de detalhes de locais (US-CAT-05).
- Autenticação e autorização providas pelo Keycloak JWT exigindo a role `ROLE_ADMIN`.
