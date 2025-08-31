# Interação 1

## Serviço de Gerenciamento de Eventos (Event Service)

Este serviço será o "catálogo" de tudo o que pode ser reservado. Sua principal responsabilidade é fornecer informações sobre eventos e suas performances. A documentação original já descreve entidades como

`Event` e `EventCategory`  , que seriam centrais neste serviço.

- **Entidades de Negócio**: `Event`, `EventCategory`, `Venue`, `Show`, `Performance`, `MediaItem`, `MediaType` .

- **Funcionalidades Principais**:

    - Listar todos os eventos disponíveis.
    - Filtrar eventos por categoria .
    - Exibir detalhes de um evento específico, incluindo sua descrição e imagens .
    - Gerenciar informações sobre as performances de um evento em um local específico (`Show`).
    - Gerenciar dados de locais (`Venue`)
    - Expor APIs para consulta de eventos e locais.

- **Schema do Banco de Dados**: Um schema dedicado com tabelas para `Event`, `EventCategory`, `Venue`, `Show`, `Performance`, `MediaItem` e `MediaType`. Os dados de shows e performances são o elo de ligação entre eventos e locais7.

# Interação 2

### Requisitos Detalhados para o Event Service

Com a arquitetura de microserviços em mente, o **Event Service** é o ponto central para todas as informações sobre o que o Ticket Monster oferece. Sua missão é gerenciar o catálogo de eventos, locais e performances. A documentação original, embora monolítica, já oferece um excelente ponto de partida para a extração dos requisitos deste módulo.

A seguir, uma lista detalhada das funcionalidades deste serviço:

#### Gerenciamento de Eventos

- **Adicionar, atualizar e remover eventos:** Este serviço deve permitir que administradores gerenciem o ciclo de vida de um evento.
- **Listar eventos:** Oferecer uma API para buscar todos os eventos disponíveis.
- **Pesquisar por categoria:** Permitir que usuários e outros serviços filtrem eventos com base na sua categoria (ex: show de rock, comédia).
- **Obter detalhes de um evento:** Fornecer uma API para recuperar informações completas de um evento específico, incluindo nome, descrição e a imagem associada.

#### Gerenciamento de Locais (Venues)

- **Adicionar, atualizar e remover locais:** Administradores devem ser capazes de gerenciar os locais onde os eventos acontecem
- **Listar locais:** Expor uma API para buscar todos os locais
- **Obter detalhes de um local:** Permitir a recuperação de informações detalhadas de um local, como endereço e a capacidade de cada seção

#### Gerenciamento de Shows e Performances

- **Adicionar, atualizar e remover shows e performances:** O sistema deve permitir que administradores definam a relação entre um evento e um local (`Show`) e suas datas e horários de apresentação (`Performance`)
- **Listar shows por evento/local:** Oferecer uma API para buscar as performances de um evento em um local específico
- **Listar performances:** Fornecer as datas e horários de apresentação de um show

#### Gerenciamento de Mídias (Media Items)

- **Adicionar mídias a eventos e locais:** Permitir a associação de imagens ou vídeos (mídia items) a um evento ou local
- **Servir mídias:** Oferecer um endpoint para o front-end acessar diretamente as mídias (imagens, vídeos) associadas a eventos e locais.

O`Event Service` também deve ser responsável por toda a validação de dados relacionados às suas entidades, como garantir que o nome de um evento seja único e que a descrição tenha um tamanho mínimo e máximo.

# Interação 3

O CRUD (Create, Read, Update, Delete) de Gerenciamento de Eventos dentro do nosso novo **Event Service** será a espinha dorsal para a administração do catálogo de shows e apresentações. Com base na documentação original, podemos identificar as seguintes tabelas e suas respectivas colunas para suportar este módulo. A modelagem abaixo busca um alto nível de coesão, seguindo o princípio de ter um esquema de banco de dados por microserviço.

### Tabelas para o Event Service

A partir da análise da documentação, o CRUD de eventos exigirá as seguintes tabelas:

1. **`Event`**: A entidade central que representa um evento.

    - `id`: Chave primária.
    - `name`: Nome do evento.
    - `description`: Descrição detalhada do evento.
    - `major`: Flag booleana para indicar se é um evento principal (maior).
    - `picture`: URL da imagem principal do evento.
    - `version`: Campo para controle de concorrência.

2. **`EventCategory`**: Tabela de lookup para categorizar os eventos.

    - `id`: Chave primária.
    - `description`: Nome da categoria (ex: "Música", "Comédia", "Teatro").

3. **`Venue`**: Representa os locais onde os eventos acontecem.

    - `id`: Chave primária.
    - `name`: Nome do local.
    - `description`: Descrição do local.
    - `address`: Endereço completo do local.
    - `picture`: URL da imagem do local.
    - `capacity`: Capacidade total de público.
    - `version`: Campo para controle de concorrência.

4. **`Show`**: Tabela de ligação entre um evento e um local, indicando que um determinado evento irá acontecer em um determinado local.

    - `id`: Chave primária.
    - `event_id`: Chave estrangeira para a tabela `Event`.
    - `venue_id`: Chave estrangeira para a tabela `Venue`.
    - `version`: Campo para controle de concorrência.

5. **`Performance`**: Tabela que define a data e hora específicas em que um `Show` ocorrerá.

    - `id`: Chave primária.
    - `show_id`: Chave estrangeira para a tabela `Show`.
    - `date`: Data e hora da apresentação.
    - `version`: Campo para controle de concorrência.

6. **`MediaItem`**: Tabela para armazenar as URLs e os tipos de mídia associados a eventos ou locais.

    - `id`: Chave primária.
    - `media_url`: URL do arquivo de mídia.
    - `type`: Tipo de mídia (ex: "image/jpeg", "video/mp4").


A separação destas tabelas em um esquema único para o **Event Service** garante que todas as informações de catálogo estejam centralizadas e que este serviço seja o único responsável por gerenciar e expor esses dados. Futuramente, se necessário, a tabela `MediaItem` poderia ser extraída para um serviço dedicado, mas para a Fase 1, ela permanecerá aqui para simplificar a implementação.
