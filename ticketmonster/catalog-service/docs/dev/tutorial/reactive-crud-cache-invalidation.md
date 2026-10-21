# Wiki: CRUD Reativo e Invalidação de Cache com Mutiny e Redis

## Conceito Técnico
Em arquiteturas reativas com Quarkus e Mutiny, operações de alteração de estado (`POST`, `PUT`, `DELETE`) em entidades master/lookup (ex.: `EventCategory`) devem invalidar de forma reativa a camada de cache Redis sem introduzir bloqueios I/O nem interromper o pipeline principal em caso de instabilidade pontual no Redis.

## Architectural Decision Record (ADR-009)

### Status
Aprovado

### Contexto
A listagem de categorias (`GET /v1/event-categories`) é consultada com altíssima frequência no catálogo público. As alterações cadastrais executadas no painel administrativo (`ROLE_ADMIN`) ocorrem com menor frequência, mas exigem que a visão pública reflita a alteração imediatamente.

### Decisão
1. **Composição em Cadeia via `.call()`**: Utilizar o operador `Uni.call()` no Mutiny para disparar a invalidação do Redis imediatamente após a confirmação da transação relacional no PostgreSQL.
2. **Resiliência Graciosa de Cache**: Falhas na invalidação do Redis são capturadas com `.onFailure().recoverWithItem((Void) null)`, registrando log de aviso sem cancelar a resposta HTTP 201/200/204 do banco relacional.

### Consequências
- Garantia de consistência eventual imediata sem impactar a latência do PostgreSQL.
- Isolamento de falha do Redis durante operações administrativas.