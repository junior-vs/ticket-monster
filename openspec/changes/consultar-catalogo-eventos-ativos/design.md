## Context

O microservice-catalog e responsavel pela exposicao publica de eventos em um cenario read-heavy com suporte a cache-aside em Redis. A US-CAT-01 exige que a consulta publica retorne apenas eventos ativos para venda. Na proposta desta change, a interpretacao de ativo sera baseada no ciclo de vida do evento, restringindo os resultados a eventos com status `PUBLISHED`.

## Goals / Non-Goals

**Goals:**
- Definir um contrato tecnico para consulta de eventos ativos/publicados no catalogo publico.
- Garantir consistencia entre regra de negocio, persistencia PostgreSQL e resposta HTTP.
- Preservar caracteristicas de desempenho de leitura com cache-aside.

**Non-Goals:**
- Alterar fluxo administrativo completo de criacao/edicao de eventos.
- Introduzir novo mecanismo de busca textual ou paginacao avancada.
- Alterar modelo de dados de Venue, Show ou Performance nesta change.

## Decisions

1. Filtro de ativo por status de dominio
- Decisao: considerar evento ativo quando `event.status = 'PUBLISHED'`.
- Racional: o modelo de dados da especificacao ja estabelece ciclo de vida explicito (`DRAFT`, `PUBLISHED`, `ARCHIVED`) e index parcial para status publicado.
- Alternativa considerada: usar janela temporal de show/performance para definir atividade. Rejeitada por conflitar com a semantica da US-CAT-01 e aumentar acoplamento com agregados de agenda.

2. Contrato de API publica estavel
- Decisao: manter endpoint de consulta publica de catalogo e explicitar comportamento de retorno para colecao vazia quando nao houver eventos ativos.
- Racional: evita breaking change para clientes existentes e torna o comportamento testavel.
- Alternativa considerada: retornar 404 sem itens. Rejeitada por pior ergonomia para clientes de listagem.

3. Cache-aside com chave segmentada de eventos ativos
- Decisao: cachear resultados da listagem publica com chave dedicada a eventos ativos e TTL curto.
- Racional: reduz leituras repetitivas ao banco sem comprometer atualizacoes administrativas.
- Alternativa considerada: desabilitar cache. Rejeitada por risco de regressao de performance.

4. Invalidacao orientada a alteracoes de status
- Decisao: invalidar cache de eventos ativos em operacoes administrativas que afetem publicacao/arquivamento de eventos.
- Racional: garante convergencia rapida entre estado persistido e catalogo publico.
- Alternativa considerada: invalidacao apenas por TTL. Rejeitada por permitir exibicao desatualizada apos mudancas criticas.

## Risks / Trade-offs

- [Risco] Divergencia entre regra de ativo no backend e entendimento de produto. -> Mitigacao: registrar semantica de ativo na spec e em testes de contrato.
- [Risco] Janela de inconsistencias por cache stale. -> Mitigacao: combinacao de invalidacao ativa e TTL curto.
- [Trade-off] Maior frequencia de invalidacoes pode reduzir hit ratio do cache. -> Mitigacao: invalidar apenas chaves de listagem afetadas por mudanca de status.
- [Trade-off] Restricao a `PUBLISHED` pode excluir eventos esperados por consumidores internos. -> Mitigacao: manter endpoints administrativos separados da API publica.
