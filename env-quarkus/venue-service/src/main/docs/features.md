# Venue Service

Ótimo! O **Venue-Service** será responsável pelo gerenciamento de locais onde os eventos ocorrerão. Ele precisa fornecer funcionalidades robustas para lidar com diferentes tipos de locais, disponibilidade, capacidade e integração com o **Event-Service**.  

Aqui estão as principais funcionalidades que esse microsserviço poderá atender:  

---

## 🔹 **1. Gestão de Locais (CRUD Completo)**
- **Cadastro de locais** (`POST /venues`)  
- **Listagem de locais** (`GET /venues`)  
- **Busca avançada** por **capacidade, localização, tipo, status** (`GET /venues?city=São Paulo&capacity>100`)  
- **Atualização de informações dos locais** (`PATCH /venues/{id}`)  
- **Exclusão de locais** (`DELETE /venues/{id}`)  

> 📌 *Cada local pode conter informações como nome, endereço, capacidade, tipo (auditório, estádio, sala de conferência, etc.), descrição e imagem.*  

---

## 🔹 **2. Disponibilidade e Reserva de Locais**
- **Checagem de disponibilidade** (`GET /venues/{id}/availability?date=2025-03-15`)  
- **Reservar local para eventos** (`POST /venues/{id}/reservations`)  
- **Bloqueio de datas para manutenção** (`PATCH /venues/{id}/block-dates`)  
- **Política de cancelamento de reservas** (`DELETE /venues/{id}/reservations/{reservationId}`)  

---

## 🔹 **3. Gestão de Estrutura e Setores do Local**
- **Definição de setores do local** (`POST /venues/{id}/sectors`)  
- **Configuração de layout do local** (mapa de assentos, distribuição de espaços)  
- **Capacidade máxima por setor** (ex: `Auditório A - 200 lugares, Salão B - 100 lugares`)  
- **Suporte a múltiplos layouts (padrão, auditório, workshop, etc.)**  

> 📌 *Essa funcionalidade é útil para locais que têm diferentes arranjos de assentos conforme o tipo de evento.*  

---

## 🔹 **4. Integração com Event-Service**
- **Associação de eventos a locais** (`POST /events/{id}/venue/{venueId}`)  
- **Validação automática de disponibilidade antes da reserva**  
- **Sincronização de eventos agendados com os locais disponíveis**  

> 📌 *Isso evitará conflitos de agendamento e ajudará a manter a consistência entre os serviços.*  

---

## 🔹 **5. Suporte a Eventos Híbridos (Presencial e Online)**
- **Marcação de locais que oferecem suporte a eventos híbridos**  
- **Configuração de equipamentos disponíveis (câmeras, microfones, telas)**  
- **Links de streaming associados ao local**  

---

## 🔹 **6. Gestão de Infraestrutura e Serviços Extras**
- **Recursos disponíveis no local** (Wi-Fi, estacionamento, acessibilidade, segurança)  
- **Gerenciamento de fornecedores** (catering, som, iluminação)  
- **Lista de contatos de suporte para cada local**  

---

## 🔹 **7. Relatórios e Estatísticas**
- **Relatórios de uso do local** (quantidade de reservas por período)  
- **Medição da taxa de ocupação** (eventos x capacidade total)  
- **Histórico de eventos realizados no local**  

---

Esse conjunto de funcionalidades tornará o **Venue-Service** um serviço essencial para a gestão de locais de eventos, garantindo eficiência, organização e integração com o **Event-Service**. 🚀  

---
 Ótima sugestão! Vou refatorar os nomes das tabelas, removendo o prefixo **"venue_"** para deixá-los mais limpos e intuitivos, mantendo as boas práticas do PostgreSQL.  

---

# **📌 Schema de Dados do Venue-Service (PostgreSQL - Nomes Simplificados)**  

## **1️⃣ Tabela `venues` (Locais de Eventos)**
```sql
CREATE TABLE venues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'Brasil',
    postal_code VARCHAR(20) NOT NULL,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    capacity INT NOT NULL CHECK (capacity > 0),
    type VARCHAR(50) NOT NULL CHECK (type IN ('auditório', 'estádio', 'sala de conferência', 'teatro', 'outro')),
    amenities JSONB DEFAULT '{}'::JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ativo' CHECK (status IN ('ativo', 'inativo', 'manutenção')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_venues_city ON venues(city);
CREATE INDEX idx_venues_name ON venues(name);
CREATE INDEX idx_venues_coordinates ON venues(latitude, longitude);
```

---

## **2️⃣ Tabela `sectors` (Setores do Local)**
```sql
CREATE TABLE sectors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    layout JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_sectors_unique ON sectors(venue_id, name);
```

---

## **3️⃣ Tabela `reservations` (Reservas de Locais)**
```sql
CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    event_id UUID NULL,
    reserved_by UUID NOT NULL,
    start_datetime TIMESTAMPTZ NOT NULL,
    end_datetime TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pendente' CHECK (status IN ('pendente', 'confirmado', 'cancelado')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_reservations_datetime ON reservations(start_datetime, end_datetime);
```

---

## **4️⃣ Tabela `images` (Imagens do Local)**
```sql
CREATE TABLE images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    description TEXT,
    uploaded_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## **5️⃣ Tabela `reviews` (Avaliações do Local)**
```sql
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_reviews_venue ON reviews(venue_id);
CREATE INDEX idx_reviews_user ON reviews(user_id);
```

---



Agora o schema está mais limpo e intuitivo. Se precisar de mais refinamentos, só avisar! 🚀

## **📌 Próximos Passos**
🚀 **Implementar a API no Quarkus**:
1. Criar **DTOs** para requests/responses.  
2. Criar **endpoints REST (`GET`, `POST`, `PATCH`, `DELETE`)**.  
3. Implementar **persistência com Panache e Hibernate ORM**.  
4. Criar **validações para evitar conflitos de reserva**.  

Esse schema está bem otimizado para o PostgreSQL e pronto para uso no **Quarkus** com **Hibernate** e **Reactive**! 🎯
---

## **🔹 Resumo das Funcionalidades Atendidas**
| **Funcionalidade**       | **Tabelas Relacionadas** |
|------------------------|----------------------|
| **CRUD de Locais**        | `venues` |
| **Divisão em Setores**    | `venue_sectors` |
| **Reservas de Locais**    | `venue_reservations` |
| **Armazenamento de Imagens** | `venue_images` |
| **Avaliações de Usuários** | `venue_reviews` |

---

## **📌 Próximos Passos**
🚀 **Implementar a API REST no Quarkus para gerenciar essas entidades**:  
1. **Criar DTOs para requests/responses**.  
2. **Criar endpoints CRUD (`GET`, `POST`, `PATCH`, `DELETE`)**.  
3. **Implementar persistência com Panache**.  
4. **Criar validações para evitar conflitos de reserva**.  

Esse schema atende às suas expectativas? Alguma funcionalidade adicional que deseja incluir? 😊