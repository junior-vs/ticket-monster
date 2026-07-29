# Research: US-CAT-10 - Venue e Section

## Decision: Manter `capacity` como coluna gerada no PostgreSQL

**Rationale**: A DDL atual de `catalog.section` define `capacity INT GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED`. Isso coloca RN12 no banco como fonte autoritativa e evita divergencia entre codigo e persistencia. O dominio pode expor `capacity` como valor de leitura, mas comandos de criacao/alteracao devem conter somente `name`, `numberOfRows` e `rowCapacity`.

**Alternatives considered**: Calcular em Java antes de persistir foi rejeitado porque duplicaria regra ja expressa no banco. Aceitar `capacity` no payload e comparar contra o calculo foi rejeitado porque transforma dado derivado em entrada de usuario.

## Decision: Ignorar `capacity` em payloads de escrita antes da persistencia

**Rationale**: A spec exige neutralizar `capacity` sem provocar erro cru do banco. A politica escolhida e ignorar/deserializar apenas campos permitidos nos DTOs de comando; respostas de leitura retornam `capacity` como campo read-only. Isso atende FR-005a e evita HTTP 500 por tentativa de escrever coluna gerada.

**Alternatives considered**: Rejeitar com HTTP 400 tambem e permitido pela historia, mas a spec da FR-005a recomenda ignorar antes do comando de escrita. Permitir campo no DTO e validar manualmente foi rejeitado por expor uma falsa opcao de entrada.

## Decision: Tratar unicidade e checks em dominio e banco

**Rationale**: RN07, RN11 e RN12 existem como constraints no PostgreSQL (`uq_venue_name`, `ck_venue_name_not_empty`, `uq_section_venue_name`, checks positivos). A aplicacao tambem deve validar comandos antes da persistencia para retorno RFC 7807 previsivel. Corridas concorrentes continuam protegidas por constraints e mapeadas para HTTP 409/400.

**Alternatives considered**: Validar somente na aplicacao foi rejeitado por nao cobrir concorrencia. Validar somente no banco foi rejeitado por piorar mensagens de erro e testes de dominio.

## Decision: CRUD REST v1 com rotas administrativas e leitura publica

**Rationale**: A arquitetura define o `microservice-catalog` como API REST publica e administrativa. Rotas de escrita exigem `ROLE_ADMIN`; leituras de Venue/Section suportam o canal publico US-CAT-04/US-CAT-05.

**Alternatives considered**: Separar APIs admin/publicas em recursos completamente distintos foi rejeitado nesta fase porque adiciona duplicacao. Usar endpoints sem versionamento foi rejeitado pela constituicao e pela disciplina contract-first.

## Decision: Cache-aside Redis em chaves explicitas da feature

**Rationale**: A spec define `catalog:venue:{id}` e `catalog:venue:{id}:sections`. O `application.properties` ja possui `catalog.cache.key-prefix=${CATALOG_CACHE_KEY_PREFIX:catalog}` e Redis configurado. Escritas de Venue/Section invalidam sincronamente as chaves afetadas apos sucesso da transacao.

**Alternatives considered**: Cachear listagens completas de Venues foi adiado porque o contrato de chave da spec cobre detalhe e secoes; listagens podem ser otimizadas depois com contrato proprio.

## Decision: Nao validar capacidade contra vendas/inventario nesta feature

**Rationale**: A arquitetura declara `microservice-catalog` sem dependencias. Consultar `microservice-inventory` para bloquear reducao de capacidade violaria esse limite. A alteracao de `numberOfRows`/`rowCapacity` deve ser aceita pelo catalogo e reconciliada por contrato/evento futuro no inventario.

**Alternatives considered**: Chamada sincronica ao inventario foi rejeitada por contrariar o documento de arquitetura e aumentar acoplamento. Ler snapshots do inventario diretamente foi rejeitado por violar database-per-service.

## Decision: Eventos de propagacao ficam fora do escopo ate ADR/contrato

**Rationale**: `microservice-inventory.spec.md` documenta snapshots de `section_snapshot` alimentados por eventos, mas nao ha contrato para `SectionDeleted`, `VenueDeleted` nem para reducao de capacidade. A feature deve registrar essa dependencia e preparar porta de saida/messaging adapter apenas se houver contrato aprovado em tarefa posterior.

**Alternatives considered**: Publicar JSON livre em `catalog-events` foi rejeitado por violar contract-first. Bloquear delecao de Section ate evento existir foi rejeitado para o plano funcional local, mas deve ser decisao de release se a consistencia cross-service for obrigatoria em producao.

## Decision: Usar Liquibase existente e validar schema inicial

**Rationale**: `db.changelog-master.xml` ja aplica `V1__init.sql`, que contem `venue` e `section`. A implementacao pode usar a DDL existente sem migracao nova se nomes/constraints atenderem a spec; migrations adicionais so serao necessarias para indices auxiliares, auditoria ou outbox/eventos.

**Alternatives considered**: Recriar tabelas via Hibernate schema generation foi rejeitado porque o projeto usa Liquibase como ferramenta de versionamento.
