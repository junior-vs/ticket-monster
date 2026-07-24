
## 1. `microservice-inventory`
* **Responsabilidade:** Alocação de assentos físicos, controle de capacidade por performance e expiração de reservas temporárias.
* **Banco de Dados:** PostgreSQL (`inventory_db`) para persistência do estado permanente + Redis para controle de locks de assentos em tempo real.
* **APIs Expostas:** Reactive REST para verificar disponibilidade e solicitar bloqueio de assentos.
* **Eventos Consumidos:** `BookingInitiatedEvent` (para manter os assentos travados), `BookingConfirmedEvent` (para persistir a ocupação definitiva), `BookingCancelledEvent` / `BookingFailedEvent` (para liberar os assentos).
* **Dependências:** `microservice-catalog` (apenas para leitura de estrutura de seções via cache).


### Responsabilidade
Controle em tempo real da disponibilidade de poltronas livres, precificação por show/seção/categoria de ingresso, gerenciamento dos bloqueios temporários (locks de checkout) e confirmação final dos assentos ocupados.

### Regras de Negócio (RNs) Mapeadas
* **RN13 (Definição de Tarifa):** O preço do ingresso deve ser associado unicamente à combinação de `Show + Seção Física + Categoria de Ingresso` (ex.: Show 1, Seção "Pista", Categoria "Meia-Entrada").
* **RN14 (Unicidade de Categoria Tarifária):** A descrição da categoria tarifária de ingresso (ex.: "Estudante", "VIP") deve ser única na base.
* **RN20 (Alocação Contígua):** O sistema deve buscar prioritariamente uma sequência linear e contígua de assentos livres na mesma fileira para atender à quantidade de ingressos solicitados pelo comprador.
* **RN22 (Concorrência por Poltrona) — As-Is:** A reserva de assentos não pode permitir reservas duplicadas da mesma poltrona física. No legado, isso é garantido por um lock pessimista de escrita (`LockModeType.PESSIMISTIC_WRITE`) sobre a linha de `SectionAllocation`, aplicado por **seção inteira** (não por assento individual) — `SeatAllocationService.retrieveSectionAllocationExclusively()`. Isso serializa toda a alocação de uma seção, mesmo entre compradores disputando assentos diferentes dentro dela.
* **Melhoria Proposta (To-Be):** substituir o lock pessimista por seção por operações atômicas NX (Not Exists) no Redis, com granularidade por assento individual (`lock:seat:{perfId}:{secId}:{row}:{num}`), eliminando a serialização de toda a seção e permitindo concorrência real entre compradores de assentos distintos.
* **RN23 (Expiração do Bloqueio):** Reservas temporárias de assentos (geradas durante a navegação do checkout) devem expirar automaticamente após 60 segundos, retornando as posições ao estoque disponível.
* **RN25 (Quantidade Positiva):** A quantidade de assentos solicitada em uma requisição de alocação deve ser estritamente maior que zero.
* **RN26 (Consistência de Desalocação):** Não é permitida a desalocação ou liberação de uma poltrona que não esteja marcada como ocupada ou reservada.
* **RN28 (Agrupamento de Desalocação):** O processo de liberação em lote de poltronas exige que todos os assentos informados na lista pertençam à mesma seção física da Performance.

### Histórias de Usuário (US)
* **US-INV-01:** Consultar mapa dinâmico de assentos livres e ocupados por performance e seção específica.
* **US-INV-02:** Solicitar reserva temporária de assentos contíguos para início de checkout.
* **US-INV-03:** Solicitar reserva temporária de assentos não contíguos caso a opção contígua falhe e o usuário aceite posições dispersas.
* **US-INV-04:** Liberar assentos reservados temporariamente após estouro do tempo de checkout (60 segundos).
* **US-INV-05:** Confirmar marcação de ocupação permanente de assentos vinculados a uma reserva paga com sucesso (Saga OK).
* **US-INV-06:** Desalocar assentos em lote para devolução ao estoque de vendas após cancelamento de compra.
* **US-INV-07:** Gerenciar categorias de tarifa de ingressos (CRUD) (Admin).
* **US-INV-08:** Configurar a tabela de valores de preços de ingressos (TicketPrice) por espetáculo, seção e tarifa (Admin).

### Critérios de Aceite (CAs)
* **CA-INV-01-LOK:** O lock temporário de poltronas no Redis deve expirar em exatos 60.000 milissegundos usando TTL nativo. O banco de dados relacional só deve ser atualizado para status `OCCUPIED` se a Saga de compra for confirmada antes da expiração deste TTL.
* **CA-INV-02-CON:** O algoritmo de busca contígua de assentos deve avaliar o gap livre a partir da matriz de status ativa no Redis. Se não houver posições contíguas suficientes em nenhuma fileira da seção, o serviço deve rejeitar a chamada retornando uma lista vazia de assentos e a flag de falha.
* **CA-INV-03-ERR:** Qualquer tentativa de liberar uma poltrona livre ou inexistente deve retornar erro com código de negócio e HTTP Status 422 (Unprocessable Entity).

---
### Sugestões de Alteração de Regras de Negócio e Histórias de Usuário para a Modernização

* **[ALTERA RN22]** Granularidade do lock passa de seção inteira (lock pessimista JPA) para assento individual (Redis NX) — ver `modernization_architecture.md` seção 21 e RN22 (as-is/to-be) acima.
* **[NOVO]** Locks órfãos (ex.: instância do serviço derruba antes de confirmar ou cancelar) devem ser recuperados automaticamente pelo TTL do Redis, sem exigir intervenção manual — capacidade equivalente ao `EXPIRATION_TIME` do legado, porém agora resiliente a falhas de processo (no legado, se a JVM cair no meio de uma alocação, o timestamp gravado na matriz ainda expira normalmente, então o comportamento é preservado, não é uma regra nova de fato — mantido aqui apenas como critério de aceite de paridade).
* **US-INV-09 (nova):** Como plataforma, quero que uma falha do serviço de inventário durante o checkout não deixe assentos bloqueados permanentemente (o TTL do Redis garante a liberação).
