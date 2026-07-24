## 1. `microservice-telemetry`
* **Responsabilidade:** Dashboards de monitoramento de vendas em tempo real e simulador de carga (Bot).
* **Banco de Dados:** PostgreSQL / TimescaleDB (`telemetry_db`) para histórico de métricas.
* **Redis:** Controle de estado do Bot (RUNNING, STOPPED) e buffer de logs rápidos.
* **APIs Expostas:** WebSockets / SSE para envio das métricas ao frontend.
* **Eventos Consumidos:** Todos os eventos de negócios publicados no Kafka.



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

### Sugestões de Alteração de Regras de Negócio e Histórias de Usuário para a Modernização


* **[ALTERA CA-TEL-02-BOT → RN]** O isolamento do Bot (já previsto como critério de aceite) passa a ser formalizado como regra de negócio: o Bot **não pode** compartilhar processo/threads com os serviços que atendem tráfego real de compradores.
* **[NOVO]** Retenção de trilha de auditoria: eventos de reserva (`BookingInitiatedEvent`, `BookingConfirmedEvent`, `BookingCancelledEvent`) publicados no Kafka devem ser retidos por período mínimo definido (ex.: 1 ano) para fins de auditoria/conciliação — o legado não mantém histórico de transições de estado, apenas o registro final no banco.
* **US-TEL-07 (nova):** Como auditor, quero consultar o histórico de eventos de uma reserva específica (tentativas, sucesso, falha, cancelamento) para fins de conciliação financeira.

> Itens marcados **[NOVO]** não têm equivalente no sistema legado e devem ser tratados no backlog como funcionalidades novas. Itens marcados **[ALTERA RNxx]** modificam o comportamento de uma regra existente e mapeada em `projeto.md` — a RN as-is correspondente permanece documentada nas seções 1–4 acima para rastreabilidade.



