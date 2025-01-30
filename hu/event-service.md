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

