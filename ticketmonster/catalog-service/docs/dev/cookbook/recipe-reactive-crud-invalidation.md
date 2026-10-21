# Cookbook: Padrão Reativo para CRUD Administrativo com Invalidação de Cache

## Objetivo
Receita replicável para implementar CRUD reativo com verificação de unicidade, integridade de exclusão e invalidação de cache Redis.

## Passo a Passo

1. **Implemente a Invalidação Não-Bloqueante com `.call()`**
   ```java
   public Uni<MyEntity> update(MyEntity entity) {
       return repository.save(entity)
           .call(() -> cache.invalidate()
               .onFailure().invoke(err -> LOG.warn("Falha ao limpar cache", err))
               .replaceWithVoid()
               .onFailure().recoverWithItem((Void) null)
           );
   }