Aqui está uma **História de Usuário (HU)** para a implementação do **CRUD de Eventos** no **`event-service`**, utilizando o formato **BDD (Behavior-Driven Development)** com **Given, When, Then**.  

---

### **História de Usuário**  
**Título:** Gerenciar eventos no sistema  
**Como** um administrador do sistema, **eu quero** criar, visualizar, atualizar e deletar eventos, **para que** eu possa gerenciar a lista de eventos disponíveis.  

---

### **Cenário 1: Criar um novo evento com sucesso**  
**Given** que sou um administrador autenticado  
**When** envio uma requisição `POST /events` com os seguintes dados válidos:  
```json
{
  "name": "Rock in Rio",
  "description": "Maior festival de música do Brasil",
  "category": "Música",
  "startDate": "2025-09-27T20:00:00",
  "endDate": "2025-09-30T23:59:59",
  "location": "Rio de Janeiro",
  "imageUrl": "https://example.com/rockinrio.jpg"
}
```
**Then** o sistema deve retornar `201 Created`  
**And** o evento deve estar salvo no banco de dados  
**And** a resposta deve conter o ID do evento recém-criado  

---

### **Cenário 2: Buscar um evento existente por ID**  
**Given** que existe um evento cadastrado com ID `123`  
**When** envio uma requisição `GET /events/123`  
**Then** o sistema deve retornar `200 OK`  
**And** a resposta deve conter os detalhes do evento:  
```json
{
  "id": 123,
  "name": "Rock in Rio",
  "description": "Maior festival de música do Brasil",
  "category": "Música",
  "startDate": "2025-09-27T20:00:00",
  "endDate": "2025-09-30T23:59:59",
  "location": "Rio de Janeiro",
  "imageUrl": "https://example.com/rockinrio.jpg"
}
```

---

### **Cenário 3: Atualizar um evento existente**  
**Given** que existe um evento cadastrado com ID `123`  
**When** envio uma requisição `PUT /events/123` com os seguintes dados:  
```json
{
  "name": "Rock in Rio 2025",
  "description": "Edição especial do maior festival de música",
  "category": "Música",
  "startDate": "2025-09-27T20:00:00",
  "endDate": "2025-09-30T23:59:59",
  "location": "Rio de Janeiro",
  "imageUrl": "https://example.com/rockinrio2025.jpg"
}
```
**Then** o sistema deve retornar `200 OK`  
**And** o evento deve ser atualizado no banco de dados  

---

### **Cenário 4: Excluir um evento existente**  
**Given** que existe um evento cadastrado com ID `123`  
**When** envio uma requisição `DELETE /events/123`  
**Then** o sistema deve retornar `204 No Content`  
**And** o evento não deve mais existir no banco de dados  

---

### **Cenário 5: Tentar buscar um evento inexistente**  
**Given** que não existe um evento com ID `999`  
**When** envio uma requisição `GET /events/999`  
**Then** o sistema deve retornar `404 Not Found`  
**And** a resposta deve conter a mensagem `"Evento não encontrado"`  

---

Esse conjunto de histórias em **BDD** pode ser usado para guiar o desenvolvimento dos endpoints REST e garantir que o comportamento esperado do sistema seja atendido. Podemos agora transformar isso em **testes automatizados** com **JUnit + RestAssured**, ou escrever **cenários Cucumber** se quiser uma abordagem mais orientada a testes de aceitação. 🚀