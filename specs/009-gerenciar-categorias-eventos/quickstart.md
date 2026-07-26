# Quickstart & Scenario Validation Guide: Gerenciar Categorias de Eventos (US-CAT-09)

Este guia apresenta o procedimento operacional e os comandos necessários para validar end-to-end as funcionalidades e critérios de aceite da funcionalidade de **Gerenciamento de Categorias de Eventos (US-CAT-09)**.

---

## 1. Pré-requisitos & Ambiente

- Java 21 JDK + Maven 3.9+
- Docker / Podman (para subir PostgreSQL + Redis via Testcontainers ou Docker Compose)
- Obter Token JWT com a role `ROLE_ADMIN` junto ao Keycloak ou simular através do ambiente de testes Quarkus Security Test (`@TestSecurity(user = "admin", roles = "ROLE_ADMIN")`).

---

## 2. Execução dos Testes Automatizados

### Rodar a suite completa de testes de `microservice-catalog`
```bash
cd microservice-catalog
./mvnw clean test
```

### Rodar especificamente a suite de categorias (Unitários, Integração e E2E P1)
```bash
cd microservice-catalog
./mvnw test -Dtest=EventCategoryResourceTest,EventCategoryServiceTest,EventCategoryRepositoryTest
```

---

## 3. Validação Manual via Endpoints REST (`curl`)

Assuma a variável de ambiente `ADMIN_TOKEN` preenchida com um token JWT válido contendo `ROLE_ADMIN`.

### Cenário 1: Criar Nova Categoria (User Story 1 - P1)
```bash
curl -X POST http://localhost:8080/api/v1/event-categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Orquestra"
  }'
```
**Resultado Esperado**:
- Status Code: `201 Created`
- Header: `Location: /api/v1/event-categories/{uuid}`
- Response Body:
```json
{
  "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "description": "Orquestra",
  "createdAt": "2026-07-25T14:30:00Z"
}
```

---

### Cenário 2: Validação de Duplicidade (RN06)
Tentar cadastrar novamente uma categoria com a mesma descrição:
```bash
curl -X POST http://localhost:8080/api/v1/event-categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Orquestra"
  }'
```
**Resultado Esperado**:
- Status Code: `409 Conflict`
- Response Body (RFC 7807):
```json
{
  "type": "https://ticketmonster.com/errors/conflict",
  "title": "Categoria Duplicada",
  "status": 409,
  "detail": "Já existe uma categoria cadastrada com a descrição 'Orquestra'.",
  "instance": "/api/v1/event-categories"
}
```

---

### Cenário 3: Alterar Descrição de Categoria Existente (User Story 2 - P2)
```bash
curl -X PUT http://localhost:8080/api/v1/event-categories/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Orquestra Sinfônica"
  }'
```
**Resultado Esperado**:
- Status Code: `200 OK`
- Response Body:
```json
{
  "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "description": "Orquestra Sinfônica",
  "createdAt": "2026-07-25T14:30:00Z"
}
```

---

### Cenário 4: Exclusão Bloqueada por Eventos Vinculados (User Story 3 - P3 / `ON DELETE RESTRICT`)
Assumindo que exista um evento vinculado a esta categoria:
```bash
curl -X DELETE http://localhost:8080/api/v1/event-categories/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```
**Resultado Esperado**:
- Status Code: `409 Conflict`
- Response Body (RFC 7807):
```json
{
  "type": "https://ticketmonster.com/errors/conflict",
  "title": "Restrição de Integridade Referencial",
  "status": 409,
  "detail": "Não é possível excluir a categoria pois existem eventos associados a ela.",
  "instance": "/api/v1/event-categories/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

---

### Cenário 5: Listagem Pública e Validação do Cache Redis (User Story 4 - P4)
```bash
curl -X GET http://localhost:8080/api/v1/event-categories
```
**Resultado Esperado**:
- Status Code: `200 OK`
- Response Body: Array JSON com categorias ordenadas alfabeticamente por descrição.
- O primeiro request busca no PostgreSQL e armazena em Redis; subconsequentes GETs respondem direto do Redis em <= 50 ms.
