# Hotel Booking API

Java and Spring Boot implementation of the hotel booking backend challenge.
Phase 1 provides the application scaffold, SQL Server development environment,
Flyway schema, health endpoint, OpenAPI tooling, and SQL Server-backed
integration test foundation.

## Prerequisites

- Java 21
- Docker Desktop

The Maven Wrapper downloads the pinned Maven version automatically.

## Build and Test

Docker Desktop must be running because integration tests use a disposable SQL
Server container.

```bash
./mvnw verify
```

## Run Locally

Create local environment configuration:

```bash
cp .env.example .env
```

Start SQL Server and create the `HotelBooking` database:

```bash
docker compose --env-file .env -f infra/local/compose.yaml up -d --wait
```

Run the API with the local profile:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Available endpoints:

```text
GET http://localhost:8080/actuator/health
GET http://localhost:8080/v3/api-docs
GET http://localhost:8080/swagger-ui/index.html
```

Stop SQL Server while preserving its data:

```bash
docker compose --env-file .env -f infra/local/compose.yaml down
```

SQL Server stores the `sa` password when its data volume is first initialized.
Changing `MSSQL_SA_PASSWORD` in `.env` does not update an existing volume. For
a disposable local database, recreate it with the new password (this deletes
all local database data):

```bash
docker compose --env-file .env -f infra/local/compose.yaml down -v
docker compose --env-file .env -f infra/local/compose.yaml up -d --wait
```

The database schema is owned by Flyway. Hibernate validates mappings against
that schema and does not create or update tables.
