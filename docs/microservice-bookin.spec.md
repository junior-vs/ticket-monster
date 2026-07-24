## 1. `microservice-booking`
* **Responsabilidade:** Gestão do ciclo de vida das reservas, validação do e-mail do cliente, emissão de tickets e orquestração do checkout.
* **Banco de Dados:** PostgreSQL (`booking_db`).
* **APIs Expostas:** REST endpoints para criação e cancelamento de bookings.
* **Eventos Publicados:** `BookingInitiatedEvent`, `BookingConfirmedEvent`, `BookingCancelledEvent`.
* **Outbox Pattern:** Utiliza a tabela de outbox na base de dados para garantir entrega de eventos ao Kafka com garantia de *at-least-once*.


## 3. Microsserviço: `microservice-booking`

### Responsabilidade
Orquestração transacional do checkout de compras. É o ponto de entrada para pedidos de reserva, validação de e-mails de compradores, geração de bilhetes, controle do código de cancelamento e processamento de exclusões (cancelamento da venda).

### Regras de Negócio (RNs) Mapeadas
* **RN15 (Itens Mínimos):** Uma transação de reserva (`Booking`) deve conter obrigatoriamente no mínimo 1 ingresso (`Ticket`).
* **RN16 (Validação de E-mail):** O e-mail de contato do comprador deve ser válido sintaticamente e não nulo.
* **RN17 (Integridade de Preço):** O preço cobrado no ticket individual deve corresponder exatamente ao valor definido em `TicketPrice` para a combinação Show + Seção + Categoria de Ingresso no momento da criação da reserva. *(No legado o preço é copiado diretamente de `TicketPrice.getPrice()` para o `Ticket` no instante da compra; não existe histórico/versionamento de preço por data — `TicketPrice` é um valor único e atual por combinação. Se a modernização exigir preço "vigente na data", isso é uma capacidade nova, não herdada do legado.)*
* **RN18 (Sem Categorias Duplicadas na Linha):** Não é permitida a inclusão de múltiplas solicitações para a mesma categoria de preço (`TicketPrice.id`) na mesma requisição de checkout (as quantidades de tickets da mesma modalidade devem ser agrupadas em um único item de requisição).
* **RN19 (Cálculo do Total da Reserva):** O valor total cobrado em uma reserva é a soma exata de todos os ingressos individuais emitidos na transação.
* **RN21 (Transação Tudo-ou-Nada):** Se a alocação de poltronas falhar para qualquer uma das seções solicitadas na requisição do cliente, a reserva inteira deve ser cancelada e estornada (Rollback da Saga).
* **RN27 (Limpeza Transacional de Cancelamento):** A exclusão de uma reserva ativa implica obrigatoriamente no cancelamento em cascata de todos os seus ingressos (`Tickets`) e no disparo da desalocação das poltronas no estoque.
* **RN29 (Código de Cancelamento) — As-Is:** O legado gera um código de cancelamento **fixo e estático** `"abc"` para toda reserva (`booking.setCancellationCode("abc")` em `BookingService.createBooking()`). O campo existe no modelo (`Booking.cancellationCode`), mas nunca é gerado de forma única ou aleatória.
* **RN30 (Autenticação do Cancelamento) — As-Is:** O método de exclusão de reserva do legado (`BookingService.deleteBooking(Long id)`) **não recebe nem valida** nenhum código de cancelamento — a operação é executada apenas com base no ID da reserva, sem qualquer verificação de posse. Isto é uma falha de controle de acesso do sistema atual (qualquer requisição `DELETE /rest/bookings/{id}` remove a reserva de terceiros), não uma proteção existente.
* **Melhoria Proposta (To-Be):** gerar o código de cancelamento com UUID ou algoritmo criptográfico único por reserva, e passar a exigir e validar esse código na exclusão — corrigindo a falha de controle de acesso identificada em RN30 as-is. Esta é uma capacidade **nova**, não uma regra herdada do legado.

### Histórias de Usuário (US)
* **US-BOOK-01:** Criar um pedido de compra contendo e-mail de contato, ID de performance e lista de ingressos desejados por preço.
* **US-BOOK-02:** Consultar o status atual e detalhes de faturamento de uma compra por ID.
* **US-BOOK-03:** Confirmar a criação definitiva da reserva e emitir os tickets após retorno positivo do inventário.
* **US-BOOK-04:** Rejeitar a compra e notificar o usuário caso o estoque de assentos contíguos não esteja mais disponível no checkout.
* **US-BOOK-05:** Cancelar uma reserva ativa informando o ID da compra e o respectivo código de cancelamento válido.
* **US-BOOK-06:** Visualizar listagem de compras paginadas no painel corporativo (Admin).
* **US-BOOK-07:** Garantir a publicação de eventos de vendas (`BookingInitiatedEvent`, `BookingConfirmedEvent`, `BookingCancelledEvent`) no broker Kafka através de escrita transacional com Outbox Pattern.

### Critérios de Aceite (CAs)
* **CA-BOOK-01-VAL:** A chamada de criação de booking (`POST /api/v1/bookings`) deve validar a estrutura de e-mail (anotação `@Email`) e rejeitar payloads que não contenham itens de ingresso, retornando HTTP 400.
* **CA-BOOK-02-SAG:** Caso o Kafka ou o `microservice-inventory` sinalize falha de alocação de poltrona, a reserva correspondente no banco de dados do `microservice-booking` deve ser imediatamente alterada para o status `FAILED`, liberando quaisquer recursos.
* **CA-BOOK-03-SEC:** O método de cancelamento (`DELETE /api/v1/bookings/{id}`) deve conter o header `X-Cancellation-Code`. Se o código fornecido não bater com o UUID gerado no momento do cadastro do Booking, o serviço deve negar a operação retornando HTTP 403 (Forbidden). *(Este controle não existe no legado — ver RN30 as-is — e é um requisito novo desta modernização.)*

---
### Sugestões de Alteração de Regras de Negócio e Histórias de Usuário para a Modernização

* **[ALTERA RN29 / RN30]** Código de cancelamento passa a ser gerado por UUID e validado no cancelamento — ver seção 3, RN29/RN30 (as-is/to-be) acima. Esta é a mudança de regra de negócio mais crítica identificada na modernização, pois corrige uma falha de controle de acesso presente no legado.
* **[NOVO]** Posse da reserva: consulta (`GET`) e cancelamento (`DELETE`) exigem ownership (token OIDC do comprador ou e-mail + código de cancelamento). Hoje `GET /rest/bookings/{id}` e a listagem completa (`GET /rest/bookings`) são publicamente acessíveis a qualquer requisitante, sem filtro por comprador.
* **[NOVO]** Idempotência via `Idempotency-Key` na criação de reserva, para tolerar retries de rede no fluxo distribuído (Saga) — risco que não existia na transação local única do legado.
* **US-BOOK-08 (nova):** Como comprador, quero cancelar minha reserva informando o código de cancelamento recebido na confirmação, e ser barrado (HTTP 403) se o código não corresponder.
* **US-BOOK-09 (nova):** Como comprador, quero reenviar uma requisição de compra sem risco de duplicar a reserva em caso de timeout de rede.
* **US-BOOK-10 (nova):** Como comprador, quero consultar apenas as reservas associadas à minha conta/e-mail, e não a listagem completa de reservas de todos os clientes.
