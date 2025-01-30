Aqui está uma lista de atividades para o desenvolvimento do Microsserviço de Eventos (event-service):

1. Configuração do Projeto

1.1 Criar um repositório Git para o microsserviço.
1.2 Configurar o projeto usando Quarkus com Maven ou Gradle.
1.3 Definir o banco de dados relacional (PostgreSQL) e configurar o application.properties.
1.4 Criar o Dockerfile e o docker-compose.yml para ambiente local.

2. Modelagem e Persistência

2.1 Criar as entidades JPA:

Event (Evento)

EventCategory (Categoria do Evento)

MediaItem (Mídia do Evento)
2.2 Configurar o persistence.xml e o uso do Hibernate.
2.3 Criar o esquema do banco de dados e configurar flyway ou Liquibase para versionamento.
2.4 Criar repositórios (Repository ou Panache) para acesso aos dados.
2.5 Popular o banco de dados com dados iniciais (import.sql).


3. Implementação da API REST

3.1 Criar a estrutura de controllers (EventResource).
3.2 Criar endpoints para CRUD de eventos:

Criar evento (POST /events)

Buscar evento por ID (GET /events/{id})

Listar todos os eventos (GET /events)

Atualizar evento (PUT /events/{id})

Deletar evento (DELETE /events/{id})
3.3 Implementar paginação e filtros nos endpoints (GET /events?category=...&date=...).
3.4 Configurar MapStruct para conversão entre DTOs e Entidades.
3.5 Implementar documentação da API com Swagger/OpenAPI.


4. Comunicação Assíncrona e Mensageria

4.1 Criar integração com Kafka ou RabbitMQ para envio de eventos assíncronos.
4.2 Disparar evento de novo evento criado (event.created).
4.3 Criar um consumer para escutar eventos externos, se necessário.

5. Testes e Qualidade

5.1 Criar testes unitários para serviços e repositórios (JUnit e Mockito).
5.2 Criar testes de integração para API (RestAssured).
5.3 Configurar cobertura de código com JaCoCo.
5.4 Implementar testes de carga com Gatling ou JMeter.

6. Segurança e Monitoramento

6.1 Implementar autenticação e autorização usando Keycloak ou JWT.
6.2 Configurar logs estruturados com Quarkus Logging.
6.3 Expor métricas com Micrometer e monitoramento via Prometheus/Grafana.

7. Deploy e Infraestrutura

7.1 Criar o Dockerfile e configurar CI/CD para deploy automatizado.
7.2 Configurar deploy em Kubernetes/OpenShift.
7.3 Criar Helm Chart para o microsserviço.
7.4 Criar uma pipeline no GitHub Actions ou Jenkins para automação.


---

Caso precise de mais detalhes ou queira seguir uma abordagem incremental, podemos quebrar essas tarefas em sprints menores. 🚀

-----

Aqui está uma História de Usuário (HU) para a implementação do CRUD de Eventos no event-service, utilizando o formato BDD (Behavior-Driven Development) com Given, When, Then.


---

História de Usuário

Título: Gerenciar eventos no sistema
Como um administrador do sistema, eu quero criar, visualizar, atualizar e deletar eventos, para que eu possa gerenciar a lista de eventos disponíveis.


---

Cenário 1: Criar um novo evento com sucesso

Given que sou um administrador autenticado
When envio uma requisição POST /events com os seguintes dados válidos:

{
  "name": "Rock in Rio",
  "description": "Maior festival de música do Brasil",
  "category": "Música",
  "startDate": "2025-09-27T20:00:00",
  "endDate": "2025-09-30T23:59:59",
  "location": "Rio de Janeiro",
  "imageUrl": "https://example.com/rockinrio.jpg"
}

Then o sistema deve retornar 201 Created
And o evento deve estar salvo no banco de dados
And a resposta deve conter o ID do evento recém-criado


---

Cenário 2: Buscar um evento existente por ID

Given que existe um evento cadastrado com ID 123
When envio uma requisição GET /events/123
Then o sistema deve retornar 200 OK
And a resposta deve conter os detalhes do evento:

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


---

Cenário 3: Atualizar um evento existente

Given que existe um evento cadastrado com ID 123
When envio uma requisição PUT /events/123 com os seguintes dados:

{
  "name": "Rock in Rio 2025",
  "description": "Edição especial do maior festival de música",
  "category": "Música",
  "startDate": "2025-09-27T20:00:00",
  "endDate": "2025-09-30T23:59:59",
  "location": "Rio de Janeiro",
  "imageUrl": "https://example.com/rockinrio2025.jpg"
}

Then o sistema deve retornar 200 OK
And o evento deve ser atualizado no banco de dados


---

Cenário 4: Excluir um evento existente

Given que existe um evento cadastrado com ID 123
When envio uma requisição DELETE /events/123
Then o sistema deve retornar 204 No Content
And o evento não deve mais existir no banco de dados


---

Cenário 5: Tentar buscar um evento inexistente

Given que não existe um evento com ID 999
When envio uma requisição GET /events/999
Then o sistema deve retornar 404 Not Found
And a resposta deve conter a mensagem "Evento não encontrado"


---

Esse conjunto de histórias em BDD pode ser usado para guiar o desenvolvimento dos endpoints REST e garantir que o comportamento esperado do sistema seja atendido. Podemos agora transformar isso em testes automatizados com JUnit + RestAssured, ou escrever cenários Cucumber se quiser uma abordagem mais orientada a testes de aceitação. 🚀

