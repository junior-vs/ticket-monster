## 1. Ajustes de Dominio e Consulta

- [ ] 1.1 Confirmar no modelo/consulta de eventos que a listagem publica filtra exclusivamente `status = PUBLISHED`
- [ ] 1.2 Garantir que eventos em `DRAFT` e `ARCHIVED` sejam excluidos da resposta publica
- [ ] 1.3 Revisar contrato de resposta da listagem para retornar colecao vazia com status de sucesso quando nao houver eventos ativos

## 2. Cache-Aside para Eventos Ativos

- [ ] 2.1 Implementar chave de cache dedicada para listagem publica de eventos ativos
- [ ] 2.2 Configurar leitura cache-aside para reutilizar resultado em requisicoes equivalentes dentro do TTL
- [ ] 2.3 Definir TTL curto e documentar racional de consistencia x performance para a listagem ativa

## 3. Invalidacao de Cache por Mudanca de Status

- [ ] 3.1 Invalidar cache de eventos ativos quando um evento transitar de `DRAFT` para `PUBLISHED`
- [ ] 3.2 Invalidar cache de eventos ativos quando um evento transitar de `PUBLISHED` para `ARCHIVED`
- [ ] 3.3 Cobrir transicoes sem impacto em eventos ativos para evitar invalidacoes desnecessarias

## 4. Testes e Validacao

- [ ] 4.1 Criar/atualizar testes de integracao para validar que a listagem publica retorna apenas eventos `PUBLISHED`
- [ ] 4.2 Criar teste para o cenario sem eventos ativos retornando colecao vazia com sucesso
- [ ] 4.3 Criar testes para hit de cache em requisicoes equivalentes no periodo de TTL
- [ ] 4.4 Criar testes para invalidacao de cache nas transicoes de status envolvendo `PUBLISHED`
- [ ] 4.5 Executar suite de testes do microservice-catalog e registrar evidencias de conformidade com a spec
