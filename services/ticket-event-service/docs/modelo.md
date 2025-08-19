 

### **Modelo de Dados do Event-Service**

O `event-service` é o proprietário de três entidades principais, que serão armazenadas em seu próprio banco de dados, seguindo o padrão **Database per Service**.

#### **Entidade: `Event`**
Esta entidade representa um evento cultural ou esportivo.

* `id` (Long): Identificador único do evento. **(Chave Primária)**
* `uuid` (UUID) - Identificador unico para o evento
* `name` (String): Nome do evento (ex: "Rock in Rio").
* `description` (String): Descrição detalhada do evento.
* `startDate` (DateTime): Data e hora de início do evento.
* `endDate` (DateTime): Data e hora de término do evento.
* `location` (String): Localização do evento (cidade/país).
* `category_id` (Long): Chave estrangeira para a entidade `EventCategory`.
* `createdAt` (DateTime): Carimbo de data/hora da criação do registro.
* `updatedAt` (DateTime): Carimbo de data/hora da última atualização.

#### **Entidade: `EventCategory`**
Esta entidade é uma tabela de lookup que classifica os eventos.

* `id` (Long): Identificador único da categoria. **(Chave Primária)**
* `description` (String): Descrição da categoria (ex: "Música", "Esportes", "Teatro").
* `createdAt` (DateTime): Carimbo de data/hora da criação do registro.

#### **Entidade: `MediaItem`**
Esta entidade armazena referências a imagens ou vídeos relacionados a um evento.

* `id` (UUID): Identificador único da mídia. **(Chave Primária)**
* `uuid` (UUID) - Identificador unico para o MediaItem
* `url` (String): URL de onde a mídia pode ser acessada.
* `mediaType` (Enum/String): Tipo de mídia (ex: "IMAGE", "VIDEO").
* `event_id` (UUID ou Long): Chave estrangeira que aponta para o evento ao qual a mídia pertence.
* `createdAt` (DateTime): Carimbo de data/hora da criação do registro.

---

### **Relacionamentos**

* **`Event` para `EventCategory` (Cardinalidade: Muitos para Um)**
    * Um `Event` pertence a uma única `EventCategory`.
    * Uma `EventCategory` pode estar associada a muitos `Eventos`.
    * Este relacionamento será implementado através da chave estrangeira `category_id` na tabela `Event`.

* **`MediaItem` para `Event` (Cardinalidade: Muitos para Um)**
    * Cada `MediaItem` pertence a um único `Event`.
    * Um `Event` pode ter muitos `MediaItem`s.
    * Este relacionamento será implementado através da chave estrangeira `event_id` na tabela `MediaItem`.

### **Considerações Adicionais para a Implementação**

* **Tipos de Dados:** A escolha por `UUID` ou `Long` para os IDs depende da sua preferência e do framework utilizado. O `UUID` é útil em ambientes distribuídos.
* **Versionamento do Schema:** Recomendo o uso de ferramentas como `Flyway` ou `Liquibase` para gerenciar as versões do schema do banco de dados, garantindo que as alterações no modelo de dados sejam aplicadas de forma controlada em todos os ambientes.
* **Performance:** Para buscas, como a listagem de eventos por categoria, é fundamental criar índices na coluna `category_id` e outras que forem utilizadas para busca.
* **Integração:** O `event-service` não precisará de informações dos outros serviços para funcionar, mas a arquitetura `Event-Driven` exige que ele seja capaz de publicar eventos assíncronos que informem outros serviços sobre as mudanças em seus dados.