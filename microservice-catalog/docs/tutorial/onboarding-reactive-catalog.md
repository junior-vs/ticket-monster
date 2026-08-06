# Tutorial: Construindo seu Primeiro Pipeline Reativo de Consulta no Quarkus
Este tutorial guiará novos desenvolvedores na implementação de pipelines de leitura reativa usando Mutiny e Panache.
## Exercício Prático
Suponha que você precise adicionar um filtro por tag de evento na consulta pública.
1. **Passo 1: Crie o Value Object `TagFilter`**
   Crie um Record em `domain/vo` que valide se a tag possui entre 2 e 30 caracteres.
2. **Passo 2: Estenda a interface `EventRepositoryPort`**
   Adicione o parâmetro `TagFilter` no método de busca reativa.
3. **Passo 3: Escreva a consulta HQL Reativa no `EventPanacheRepository`**
   Utilize `PanacheRepositoryBase` para encadear a condição `status = :status AND :tag MEMBER OF e.tags`.
4. **Passo 4: Valide sem subir banco usando Testes Unitários**
   Crie um teste unitário no Use Case mockando as portas com `Uni.createFrom().item(...)`.