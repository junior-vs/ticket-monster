# Wiki: Reactive Cache-Aside Pagination com Mutiny e Panache

## Conceito Técnico
A paginação em serviços de leitura de alta concorrência exige desacoplar a carga do banco relacional através de camadas de cache volátil (Redis). No modelo assíncrono não-bloqueante com Quarkus e Mutiny, a composição entre o cliente Redis (`Uni<Optional<T>>`) e a consulta reativa no PostgreSQL (`PanacheRepositoryBase`) é construída como um pipeline reativo unificado.

## Architectural Decision Record (ADR-008)

### Status
Aprovado

### Contexto
O endpoint `GET /v1/events/published` do `microservice-catalog` atende picos de tráfego de leitura pública. A consulta precisa responder em p95 ≤ 250ms (PR-001) com suporte a filtros dinâmicos e paginação.

### Decisão
1. Adotar **Mutiny (`Uni<T>`)** para todo o pipeline de I/O de consulta e cache.
2. Implementar o padrão **Cache-Aside** no `RedisEventCacheAdapter` com chave contendo hash da combinação `(page, size, categoryId)`.
3. Configurar **degradação graciosa**: caso o servidor Redis fique indisponível ou estoure o timeout de leitura, o pipeline captura o erro via `.onFailure().recoverWithItem(Optional.empty())` e consulta o PostgreSQL sem interromper o cliente HTTP.

### Consequências
- **Positivas**: Throughput máximo na thread Event Loop; redução drástica de I/O no banco; alta resiliência contra instabilidade de cache.
- **Negativas**: Pequeno risco de eventual desatualização de dados no catálogo até a expiração do TTL (5 minutos) do cache Redis.