# IAM Service

This is the backend service for the Identity and Access Management (IAM) system. It leverages Keycloak for user authentication and authorization, and uses Kafka for asynchronous user event processing. It also tracks user login history in a PostgreSQL database.

## Features

- **User Registration:** Registers new users in Keycloak and publishes a user creation event to Kafka.
- **User Login:** Authenticates users against Keycloak and records login attempts (success/failure) in the database. Returns Keycloak access and refresh tokens upon successful login.
- **Logout:** Initiates user logout in Keycloak and validates token invalidation.
- **Login History:** Retrieves the last 5 login attempts for a specific user from the database.
- **Role-Based Access Control (RBAC):** Utilizes Keycloak's roles and groups for managing user permissions.
- **Asynchronous User Event Handling:** Publishes user creation events to a Kafka topic for other services to consume.
- **Database Integration:** Stores login history in a PostgreSQL database using Spring Data JPA.
- **API Documentation:** Provides API documentation using SpringDoc (Swagger UI).
- **Actuator Endpoints:** Exposes Spring Boot Actuator endpoints for monitoring and management.

## Technologies Used

- **Java:** The primary programming language.
- **Spring Boot:** A framework for building standalone, production-grade Spring-based Applications.
- **Spring Web:** For building RESTful APIs.
- **Spring Security:** For securing the application and integrating with Keycloak.
- **Spring Security OAuth2 Resource Server:** For JWT-based authentication.
- **Spring Kafka:** For interacting with the Kafka message broker.
- **Spring Data JPA:** For database interaction with PostgreSQL.
- **PostgreSQL:** The relational database used for storing login history.
- **Keycloak:** An open-source Identity and Access Management solution.
- **Liquibase:** (Currently disabled) For database schema migrations.
- **Maven:** For project management and dependency management.
- **Slf4j & Logback:** For logging.
- **SpringDoc (Swagger UI):** For generating API documentation.
- **Spring Boot Actuator:** For providing operational information about the application.

The IAM Service interacts with the following components:

1.  **Keycloak:** For user registration, authentication, and authorization.
2.  **Kafka:** For publishing asynchronous `UserEvent` messages.
3.  **PostgreSQL:** For storing user login history.
4.  **Clients (e.g., Frontend):** Consume the API endpoints for user registration and login.

## Setup and Installation

1.  **Prerequisites:**
    - Java Development Kit (JDK) 17 or higher
    - Maven
    - Docker (for running Keycloak, Kafka, and PostgreSQL - optional but recommended for local development)
    - Docker Compose (optional for orchestrating Docker containers)

2.  **Clone the repository:**
    ```bash
    git clone <your-repository-url>
    cd <your-repository-name>
    ```

3.  **Configure Environment:**
    - Create a `src/main/resources/application.properties` file (or `application.yml`) and configure the following:
        - **Keycloak Settings:** `keycloak.auth-server-url`, `keycloak.realm`, `keycloak.client-id`, `keycloak.client-secret`
        - **Kafka Settings:** `spring.kafka.bootstrap-servers`, `kafka.user-topic`
        - **PostgreSQL Settings:** `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
        - **Server Port:** `server.port`
        - Other application-specific configurations.

    Example `application.properties`:
    ```properties
    server.port=8090

    spring.datasource.url=jdbc:postgresql://localhost:5432/loginTrackDB
    spring.datasource.username=postgres
    spring.datasource.password=postgres
    spring.datasource.driver-class-name=org.postgresql.Driver
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true

    spring.kafka.bootstrap-servers=localhost:9092
    spring.kafka.consumer.group-id=user-sync-group
    spring.kafka.consumer.auto-offset-reset=earliest
    spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
    spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
    kafka.user-topic=user-topic

    keycloak.auth-server-url=http://localhost:8080
    keycloak.realm=contentnexus
    keycloak.client-id=iam-service
    keycloak.client-secret=7FTdbO8lU0pSFqmKqxLOPGFZ71hFD2pR
    keycloak.public-client=true
    keycloak.bearer-only=true
    ```

4.  **Set up Keycloak:**
    - Ensure you have a Keycloak instance running. You can use Docker for this:
      ```bash
      docker run -p 8080:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin quay.io/keycloak/keycloak:latest start --optimized
      ```
    - Access the Keycloak admin console (usually at `http://localhost:8080/admin`) and create the necessary realm (`contentnexus` in the example), client (`iam-service` with appropriate settings), and user groups/roles.

5.  **Set up Kafka:**
    - Ensure you have a Kafka instance running. You can use Docker Compose:
      ```yaml
      version: '3.7'
      services:
        zookeeper:
          image: confluentinc/cp-zookeeper:latest
          environment:
            ZOOKEEPER_CLIENT_PORT: 2181
            ZOOKEEPER_TICK_TIME: 2000
          ports:
            - "2181:2181"

        kafka:
          image: confluentinc/cp-kafka:latest
          depends_on:
            - zookeeper
          ports:
            - "9092:9092"
          environment:
            KAFKA_BROKER_ID: 1
            KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
            KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
            KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      ```
      Run with: `docker-compose up -d`

6.  **Set up PostgreSQL:**
    - Ensure you have a PostgreSQL instance running. You can use Docker:
      ```bash
      docker run -d --name login-db -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=loginTrackDB postgres:latest
      ```

7.  **Build the application:**
    ```bash
    mvn clean install
    ```

8.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```

    The service will be accessible at `http://localhost:8090` (or the port configured in your `application.properties`).

## API Endpoints

You can access the API documentation using Swagger UI at `http://localhost:8090/swagger-ui.html`.

The main API endpoints are:

-   **`POST /auth/register`**: Registers a new user. Accepts a JSON body with user details (username, email, firstName, lastName, password).
-   **`POST /auth/login`**: Authenticates a user. Accepts username and password as request parameters. Returns Keycloak access and refresh tokens.
-   **`GET /auth/login-history/{username}`**: Retrieves the last 5 login attempts for the given username.
-   **`POST /auth/logout`**: Logs out a user. Requires `refreshToken`, `accessToken`, and `userId` as request parameters.

## Kafka Topic

The service publishes `UserEvent` messages to the `user-topic` upon successful user registration. Other services can subscribe to this topic to react to new user creations.

The `UserEvent` JSON structure includes:

```json
{
  "userId": "...",
  "username": "...",
  "email": "...",
  "firstName": "...",
  "lastName": "...",
  "role": "..."
}
