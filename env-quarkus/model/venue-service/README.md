# Venue Service

## Overview
The **Venue Service** is a microservice designed to manage venues where events can take place. It provides robust functionalities for handling various types of venues, their availability, capacity, and integration with other services.

## Features
- **Venue Management**: Create, read, update, and delete venues.
- **Venue Availability**: Check availability and manage reservations for events.
- **Venue Sector Management**: Define sectors within venues and manage their layouts.
- **Image Management**: Store and manage images associated with venues.
- **User Reviews**: Allow users to leave reviews and ratings for venues.

## Project Structure
```
venue-service
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── venueservice
│   │   │               ├── entity
│   │   │               ├── repository
│   │   │               ├── service
│   │   │               └── controller
│   │   ├── resources
│   │   └── test
├── pom.xml
└── README.md
```

## Technologies Used
- **Jakarta JPA**: For object-relational mapping and database interactions.
- **Spring Boot**: For building the RESTful API.
- **Maven**: For project management and dependency management.

## Getting Started
1. **Clone the repository**:
   ```
   git clone <repository-url>
   cd venue-service
   ```

2. **Build the project**:
   ```
   mvn clean install
   ```

3. **Run the application**:
   ```
   mvn spring-boot:run
   ```

4. **Access the API**: The API will be available at `http://localhost:8080`.

## API Endpoints
- **Venues**:
  - `POST /venues`: Create a new venue.
  - `GET /venues`: Retrieve all venues.
  - `PATCH /venues/{id}`: Update a venue.
  - `DELETE /venues/{id}`: Delete a venue.

- **Venue Sectors**:
  - `POST /venues/{id}/sectors`: Create a new sector for a venue.
  - `GET /venues/{id}/sectors`: Retrieve sectors for a venue.

- **Reservations**:
  - `POST /venues/{id}/reservations`: Reserve a venue for an event.
  - `GET /venues/{id}/reservations`: Retrieve reservations for a venue.

- **Images**:
  - `POST /venues/{id}/images`: Upload an image for a venue.
  - `GET /venues/{id}/images`: Retrieve images for a venue.

- **Reviews**:
  - `POST /venues/{id}/reviews`: Submit a review for a venue.
  - `GET /venues/{id}/reviews`: Retrieve reviews for a venue.

## Contributing
Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for details.