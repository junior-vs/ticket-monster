## Why

A US-CAT-01 define a necessidade de expor apenas eventos ativos no catalogo publico, mas hoje essa intencao ainda nao esta formalizada como capacidade especifica no fluxo OpenSpec. Sem esse contrato explicito, implementacoes futuras podem retornar eventos inativos ou incompletos, gerando inconsistencias de negocio.

## What Changes

- Formalizar uma capacidade de consulta publica de eventos ativos para o microservice-catalog.
- Definir requisitos funcionais para listagem de eventos com filtro por status publicado/ativo.
- Definir comportamento de resposta para casos sem eventos ativos.
- Definir criterios de observabilidade e cache para leitura read-heavy.

## Capabilities

### New Capabilities
- `catalog-active-events-query`: Consulta publica de catalogo retornando apenas eventos ativos/publicados, com contrato de resposta e comportamento de filtragem.

### Modified Capabilities
- Nenhuma.

## Impact

- Especificacoes OpenSpec para o dominio de catalogo.
- API REST publica do microservice-catalog (endpoint de listagem de eventos).
- Camadas de consulta, cache Redis (cache-aside) e serializacao de resposta.
- Testes de contrato e integracao para garantir filtragem por status ativo.
