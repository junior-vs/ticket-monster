# Especificação dos Microsserviços Propostos — TicketMonster

Este documento detalha as especificações funcionais e técnicas para cada um dos microsserviços propostos na nova arquitetura do **TicketMonster**. As regras de negócio foram extraídas do mapeamento de engenharia reversa do monolito original ([projeto.md](file:///e:/develop/repos/java-projects/ticket-monster/docs/projeto.md)) e redistribuídas sob os princípios de Domain-Driven Design (DDD).

> **Nota de rastreabilidade:** a numeração das RNs (RN01–RN41) é a mesma de `projeto.md`, para permitir conferência direta com o legado. Toda RN abaixo descreve o comportamento **atual (as-is)** do sistema, exatamente como implementado hoje. Regras cujo comportamento **muda** na modernização (novo mecanismo de lock, novo processo de cancelamento, etc.) são apresentadas em duas partes dentro do mesmo microsserviço: a RN as-is (idêntica a `projeto.md`) seguida de um item **"Melhoria Proposta (To-Be)"**, explicitamente marcado como tal, para não haver ambiguidade sobre o que já existe hoje e o que é novo.

---

## 1. Microsserviço: `microservice-catalog`

### Responsabilidade
Gerenciamento das entidades de divulgação artística, locais físicos, shows cadastrados e a agenda temporal de apresentações (performances). É um serviço otimizado para leitura intensiva (*read-heavy*), cujos dados são expostos ao canal público de vendas e painel de administração.

### Regras de Negócio (RNs) Mapeadas
* **RN01 (Unicidade do Evento):** O nome do evento deve ser único em toda a base de dados.
* **RN02 (Tamanho do Nome do Evento):** O nome de um evento deve ter obrigatoriamente entre 5 e 50 caracteres.
* **RN03 (Tamanho da Descrição do Evento):** A descrição de um evento deve ter entre 20 e 1000 caracteres.
* **RN04 (Categoria Obrigatória):** Todo evento cadastrado deve estar associado a uma categoria de evento ativa (`EventCategory`).
* **RN05 (Mídia Opcional):** A imagem promocional de um evento é um campo opcional.
* **RN06 (Categoria Única):** A descrição da categoria de evento deve ser única e não nula.
* **RN07 (Unicidade de Venue):** O nome do local físico (Venue) deve ser único e não vazio.
* **RN08 (Associação de Show):** Um Show representa uma associação única entre um `Event` e um `Venue`. Não é permitida a duplicação dessa associação.
* **RN09 (Performance Obrigatória):** Toda performance de show deve conter obrigatoriamente data/hora e estar vinculada a um `Show` válido.
* **RN10 (Unicidade de Sessão por Show):** Não é permitido agendar duas performances do mesmo show exatamente na mesma data e hora.
* **RN11 (Unicidade de Seção por Venue):** O nome de uma seção física (ex.: "Camarote") deve ser único dentro de um mesmo `Venue`.
* **RN12 (Cálculo de Capacidade de Seção):** A capacidade de assentos de uma seção física é calculada pela multiplicação de `fileiras × capacidade por fileira` (`Section.getCapacity()`).
* **RN34 (Restrição de Tipo de Mídia):** O tipo de item de mídia promocional aceito é restrito à categoria `IMAGE`.
* **RN35 (Fallback de Imagem):** Em caso de falha de carregamento ou download de uma imagem promocional remota, o microsserviço deve injetar síncronamente o caminho para uma imagem de fallback local padrão (`not_available.jpg`).
* **RN37 (URL de Mídia Válida):** A URL de um item de mídia (`MediaItem`) deve ter uma estrutura válida (`http` ou `https`) e ser única na base.

### Histórias de Usuário (US)
* **US-CAT-01:** Consultar catálogo de eventos ativos.
* **US-CAT-02:** Filtrar eventos catalogados por categoria de interesse.
* **US-CAT-03:** Visualizar detalhes artísticos e imagem promocional de um evento específico por ID.
* **US-CAT-04:** Consultar lista de locais de espetáculo (Venues) disponíveis para venda.
* **US-CAT-05:** Visualizar detalhes de um local de espetáculo (capacidade total, seções físicas e endereço).
* **US-CAT-06:** Listar a agenda de shows vinculados a um determinado evento ou local.
* **US-CAT-07:** Consultar sessões (performances) ativas de um show para compra.
* **US-CAT-08:** Gerenciar cadastro de eventos (Inclusão, Alteração, Exclusão) (Admin).
* **US-CAT-09:** Gerenciar catálogo de categorias de eventos (Admin).
* **US-CAT-10:** Gerenciar locais físicos de espetáculos e suas seções estruturais (Admin).
* **US-CAT-11:** Criar e alterar agendamentos de shows e suas performances temporais (Admin).
* **US-CAT-12:** Cadastrar novos itens de mídia com validação síncrona de URL (Admin).

### Critérios de Aceite (CAs)
* **CA-CAT-01-VAL:** Toda alteração de evento deve validar as constraints de tamanho (5-50 caracteres para nome, 20-1000 para descrição) retornando erro 400 (Problem Details) sob falha.
* **CA-CAT-02-UNI:** A criação de um agendamento de Show ou Performance deve validar unicidades relacionais no banco de dados e retornar HTTP 409 (Conflict) em caso de duplicidade detectada.
* **CA-CAT-03-MED:** A falha na resolução HTTP da imagem do `MediaItem` na persistência administrativa não deve travar o cadastro; o microsserviço deve persistir a URL original e marcar a imagem para fallback de leitura local.

---

## 2. Microsserviço: `microservice-inventory`

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

## 4. Microsserviço: `microservice-telemetry`

### Responsabilidade
Consumo de eventos de vendas para atualização de painéis de ocupação em tempo real e orquestração do robô simulador de carga (Bot) para demonstração de estresse de compras.

### Regras de Negócio (RNs) Mapeadas
* **RN31 (Métricas de Shows Ativos):** O painel analítico de métricas deve exibir informações consolidadas exclusivamente de shows que possuam performances em datas futuras (ignorar espetáculos passados).
* **RN32 (Métricas de Ocupação Futura):** O cálculo e contagem de poltronas vendidas para os gráficos de ocupação em tempo real devem desconsiderar dados de exibições passadas.
* **RN33 (Acesso de Leitura):** Os endpoints expostos pelo painel de monitoramento de métricas são estritamente de leitura (apenas métodos `@GET` HTTP).
* **RN38 (Limites do Simulador):** O robô de simulação de carga (Bot) é limitado por restrições operacionais rígidas: máximo de 100 ticket requests por simulação e no máximo 100 tickets individuais por request.
* **RN39 (Frequência do Simulador):** O Bot deve rodar de forma contínua em background disparando ordens de compra fictícias em períodos configuráveis (padrão de 3 segundos).
* **RN40 (Capacidade do Buffer de Logs):** O log de histórico de compras simuladas deve manter apenas as últimas 50 mensagens registradas na memória RAM rápida da aplicação.
* **RN41 (Limpeza em Lotes - Bot Reset):** O comando de reset de dados do ambiente de demonstração deve limpar o banco de dados transacional deletando as reservas de forma segmentada e escalonada (em lotes de 10 por transação) para evitar lock em tabelas e estouro de memória de log transacional.

### Histórias de Usuário (US)
* **US-TEL-01:** Consultar painel analítico consolidado contendo estatísticas de ocupação de assentos para apresentações futuras.
* **US-TEL-02:** Iniciar a execução do simulador de carga (Bot) via console administrativo.
* **US-TEL-03:** Interromper a simulação de compras do Bot a qualquer momento.
* **US-TEL-04:** Consultar o log rotativo contendo as últimas 50 atividades executadas pelo robô em execução.
* **US-TEL-05:** Solicitar limpeza total de transações de venda do banco de dados (Bot Reset em lotes).
* **US-TEL-06:** Receber notificações em tempo real de novos ingressos vendidos via canal de mensageria WebSockets/SSE.

### Critérios de Aceite (CAs)
* **CA-TEL-01-MET:** A contagem de ocupação de assentos em shows deve ser atualizada em tempo real (latência menor que 2 segundos) a partir do consumo de eventos `BookingConfirmedEvent` no Kafka, atualizando os clientes ativos via WebSocket.
* **CA-TEL-02-BOT:** O acionamento do Bot de compras simuladas deve ser isolado em container ou worker próprio para evitar consumo excessivo de CPU das threads principais de atendimento de clientes reais.
* **CA-TEL-03-RST:** A funcionalidade de RESET deve enviar requisições de deleção assíncronas ao `microservice-booking` em lotes controlados de 10 registros. O tempo total de execução não pode impactar o tempo de resposta da API do Gateway.

---

## 5. Sugestões de Alteração de Regras de Negócio e Histórias de Usuário para a Modernização

Visão consolidada em `modernization_architecture.md`, seção 21. Abaixo, o detalhamento específico por microsserviço — cada item indica se **altera** uma RN as-is existente ou é **novo** (sem equivalente no legado).

### 5.1 `microservice-catalog`
* **[ALTERA RN34]** O tipo de mídia deixa de ser um enum fechado (`IMAGE` apenas) e passa a ser um catálogo extensível (`IMAGE`, `VIDEO`, `AUDIO`), configurável sem redeploy do serviço.
* **[NOVO]** `Event` passa a ter ciclo de vida explícito (`DRAFT` → `PUBLISHED` → `ARCHIVED`), em vez de existir implicitamente a partir do cadastro. Hoje qualquer `Event` criado no admin já aparece imediatamente no catálogo público.
* **[NOVO]** `EventCategory` não pode ser excluída se houver `Event` associado (hoje o legado não impõe essa proteção explicitamente a nível de regra de negócio, apenas via eventual erro de integridade referencial do banco).
* **US-CAT-13 (nova):** Como administrador, quero cadastrar um evento em rascunho e publicá-lo apenas quando estiver pronto, para evitar exibir eventos incompletos no catálogo público.
* **US-CAT-14 (nova):** Como administrador, quero cadastrar mídia em vídeo além de imagem, sem depender de alteração de código do serviço.

### 5.2 `microservice-inventory`
* **[ALTERA RN22]** Granularidade do lock passa de seção inteira (lock pessimista JPA) para assento individual (Redis NX) — ver `modernization_architecture.md` seção 21 e RN22 (as-is/to-be) acima.
* **[NOVO]** Locks órfãos (ex.: instância do serviço derruba antes de confirmar ou cancelar) devem ser recuperados automaticamente pelo TTL do Redis, sem exigir intervenção manual — capacidade equivalente ao `EXPIRATION_TIME` do legado, porém agora resiliente a falhas de processo (no legado, se a JVM cair no meio de uma alocação, o timestamp gravado na matriz ainda expira normalmente, então o comportamento é preservado, não é uma regra nova de fato — mantido aqui apenas como critério de aceite de paridade).
* **US-INV-09 (nova):** Como plataforma, quero que uma falha do serviço de inventário durante o checkout não deixe assentos bloqueados permanentemente (o TTL do Redis garante a liberação).

### 5.3 `microservice-booking`
* **[ALTERA RN29 / RN30]** Código de cancelamento passa a ser gerado por UUID e validado no cancelamento — ver seção 3, RN29/RN30 (as-is/to-be) acima. Esta é a mudança de regra de negócio mais crítica identificada na modernização, pois corrige uma falha de controle de acesso presente no legado.
* **[NOVO]** Posse da reserva: consulta (`GET`) e cancelamento (`DELETE`) exigem ownership (token OIDC do comprador ou e-mail + código de cancelamento). Hoje `GET /rest/bookings/{id}` e a listagem completa (`GET /rest/bookings`) são publicamente acessíveis a qualquer requisitante, sem filtro por comprador.
* **[NOVO]** Idempotência via `Idempotency-Key` na criação de reserva, para tolerar retries de rede no fluxo distribuído (Saga) — risco que não existia na transação local única do legado.
* **US-BOOK-08 (nova):** Como comprador, quero cancelar minha reserva informando o código de cancelamento recebido na confirmação, e ser barrado (HTTP 403) se o código não corresponder.
* **US-BOOK-09 (nova):** Como comprador, quero reenviar uma requisição de compra sem risco de duplicar a reserva em caso de timeout de rede.
* **US-BOOK-10 (nova):** Como comprador, quero consultar apenas as reservas associadas à minha conta/e-mail, e não a listagem completa de reservas de todos os clientes.

### 5.4 `microservice-telemetry`
* **[ALTERA CA-TEL-02-BOT → RN]** O isolamento do Bot (já previsto como critério de aceite) passa a ser formalizado como regra de negócio: o Bot **não pode** compartilhar processo/threads com os serviços que atendem tráfego real de compradores.
* **[NOVO]** Retenção de trilha de auditoria: eventos de reserva (`BookingInitiatedEvent`, `BookingConfirmedEvent`, `BookingCancelledEvent`) publicados no Kafka devem ser retidos por período mínimo definido (ex.: 1 ano) para fins de auditoria/conciliação — o legado não mantém histórico de transições de estado, apenas o registro final no banco.
* **US-TEL-07 (nova):** Como auditor, quero consultar o histórico de eventos de uma reserva específica (tentativas, sucesso, falha, cancelamento) para fins de conciliação financeira.

> Itens marcados **[NOVO]** não têm equivalente no sistema legado e devem ser tratados no backlog como funcionalidades novas. Itens marcados **[ALTERA RNxx]** modificam o comportamento de uma regra existente e mapeada em `projeto.md` — a RN as-is correspondente permanece documentada nas seções 1–4 acima para rastreabilidade.