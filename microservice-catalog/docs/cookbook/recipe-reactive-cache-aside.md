# Cookbook: Como Implementar Paginação e Cache-Aside Reativo no Quarkus

## Pré-requisitos
- Extension `quarkus-hibernate-reactive-panache`
- Extension `quarkus-redis-client`
- Mutiny (`io.smallrye.mutiny.Uni`)

## Passo a Passo

1. **Construa um Value Object de Paginação Imutável**
   Utilize Java Records garantindo os limites máximos de `size` no construtor compacto para barrar *Out Of Memory* por queries desproporcionais.

2. **Crie a Assinatura do Repositório Reativo**
   Retorne `Uni<PageResult<T>>` combinando o `.list()` e o `.count()` do Panache Query usando `Uni.combine().all().unis(...)`.

3. **Componha o Pipeline Cache-Aside no Use Case**
   ```java
   return cachePort.get(key)
       .onFailure().recoverWithItem(Optional.empty()) // Degradação graciosa
       .chain(cached -> cached.isPresent() 
           ? Uni.createFrom().item(cached.get())
           : repositoryPort.find(filter, page).call(result -> cachePort.put(key, result))
       );