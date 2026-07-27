# Hotel Booking API

Java and Spring Boot implementation of the hotel booking backend challenge.
Phases 1 through 3 provide the application scaffold, SQL Server development
environment, Flyway schema, JPA model, domain rules, deterministic test data,
hotel search, room availability, health endpoint, OpenAPI tooling, and SQL
Server-backed integration tests.

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

## Continuous Integration

GitHub Actions validates the Docker Compose configuration and runs the complete
Maven verification suite with Temurin Java 21. CI runs for pull requests into
`main`, pushes to `dev` or `main`, and manual workflow dispatches. Integration
tests create their own SQL Server through Testcontainers, so CI does not need
database secrets.

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
GET http://localhost:8080/api/hotels?name=Grand
GET http://localhost:8080/api/hotels/{hotelId}/rooms/available?checkIn=2026-08-01&checkOut=2026-08-03&guests=2&roomType=DOUBLE
POST http://localhost:8080/api/admin/seed
POST http://localhost:8080/api/admin/reset
GET http://localhost:8080/actuator/health
GET http://localhost:8080/v3/api-docs
GET http://localhost:8080/swagger-ui/index.html
```

Seed predictable local data:

```bash
curl -i -X POST http://localhost:8080/api/admin/seed
```

The seed operation first resets all data, then creates `Grand Plaza Hotel`
with its fixed ID `00000000-0000-0000-0000-000000000001` and six rooms.

Reset all application data:

```bash
curl -i -X POST http://localhost:8080/api/admin/reset
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
