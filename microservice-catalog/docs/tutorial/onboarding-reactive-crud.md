# Tutorial: Desenvolvendo Operações de Escrita Reativas com Mutiny e Panache
Aprenda a construir fluxos de escrita e exclusão reativa com validações de regra de negócio sem bloqueio de threads.
## Exercício Prático
Implemente o endpoint de alteração de descrição de categoria.
1. **Validação de Unicidade Negativa**: Certifique-se de que a nova descrição não pertence a **outra** categoria existente (`id != targetId`).
2. **Operação Reativa de Merge**: Use `session.merge()` no Hibernate Reactive para atualizar a entidade.
3. **Validação com `UniAssertSubscriber`**: Crie um teste unitário simulando cenário de colisão de descrição e valide o retorno de `EventCategoryAlreadyExistsException`.