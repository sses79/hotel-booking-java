# Hotel Booking API — Java Implementation Plan

## Goal

Rebuild the existing `HoltelBooking` backend challenge as an idiomatic Java
application while preserving its scope, REST API, business rules, SQL Server
data model, concurrency guarantees, test coverage, and optional Azure
deployment path.

The delivery principle remains the same: make booking correctness easy to
review, run, and discuss before adding platform features.

## Technology Baseline

Use a conventional Spring stack:

- Java 21 LTS.
- Spring Boot 4.0.7. This is the current stable line supported by the
  SpringDoc integration selected for the project.
- Maven with Maven Wrapper.
- Spring Web MVC for the REST API.
- Spring Data JPA with Hibernate.
- Jakarta Bean Validation.
- Microsoft JDBC Driver for SQL Server.
- Flyway, including `flyway-sqlserver`, for schema migrations.
- springdoc-openapi 3.x for OpenAPI and Swagger UI.
- Spring Boot Actuator for the health endpoint.
- JUnit 5, AssertJ, Spring Boot Test, MockMvc, and Testcontainers SQL Server.
- Docker Compose for local SQL Server.

Pin exact dependency and plugin versions when the project is generated, then
let the Spring Boot dependency management control compatible transitive
versions.

## Project Shape

Use one Spring Boot application rather than copying the .NET projects into
separate Maven modules. Package boundaries provide the same separation with
less build and dependency overhead for a challenge of this size.

```text
new-java/
  pom.xml
  mvnw
  mvnw.cmd
  .mvn/
  src/
    main/
      java/com/example/hotelbooking/
        HotelBookingApplication.java
        api/
          controller/
          dto/
          error/
        domain/
        persistence/
          entity/
          repository/
        service/
        config/
      resources/
        application.yml
        application-local.yml
        db/migration/
          V1__create_hotel_booking_schema.sql
    test/
      java/com/example/hotelbooking/
        unit/
        integration/
  infra/
    local/compose.yaml
    bicep/                  # optional final phase
  docs/
```

Responsibilities:

- `api`: REST controllers, request/response records, validation, OpenAPI
  annotations, and centralized error responses.
- `domain`: `RoomType` and pure booking rules that do not depend on Spring or
  JPA.
- `persistence`: JPA entities and Spring Data repositories.
- `service`: hotel search, availability, booking creation and lookup, and
  seed/reset use cases.
- `config`: transaction/retry, time, OpenAPI, and application configuration.
- `unit`: fast tests for date, capacity, overlap, and room-ordering rules.
- `integration`: real HTTP, JPA, Flyway, transaction, and SQL Server tests.

Do not add separate domain/application/infrastructure Maven modules, CQRS,
message queues, authentication, or a frontend.

## .NET-to-Java Mapping

| Existing implementation | Java implementation |
| --- | --- |
| ASP.NET Core controllers | Spring `@RestController` |
| Dependency injection | Spring constructor injection |
| Request/response DTOs | Java records plus Jakarta Validation |
| `ProblemDetails` | Spring `ProblemDetail` via `@RestControllerAdvice` |
| EF Core `DbContext` | Spring Data JPA repositories/Hibernate |
| EF Core model configuration | JPA mappings plus explicit Flyway SQL |
| EF Core migrations | Flyway versioned migrations |
| `DateOnly` | `LocalDate` |
| `DateTimeOffset` / `TimeProvider` | `Instant` / injected `Clock` |
| `Guid` | `UUID`, stored as SQL Server `uniqueidentifier` |
| Swagger/OpenAPI | springdoc-openapi Swagger UI |
| ASP.NET health check | Spring Boot Actuator health |
| xUnit | JUnit 5 |
| `WebApplicationFactory` | `@SpringBootTest` with MockMvc |
| Testcontainers.MsSql | Testcontainers `MSSQLServerContainer` |
| Serializable EF transaction | Spring `TransactionTemplate` at `SERIALIZABLE` |
| EF execution retry strategy | retry wrapper around the complete transaction |

## Domain Model

```text
Hotel
  id: UUID
  name: String

Room
  id: UUID
  hotelId: UUID
  roomNumber: String
  roomType: RoomType
  capacity: int

Booking
  id: UUID
  bookingReference: String
  hotelId: UUID
  roomId: UUID
  guestName: String
  guestCount: int
  checkInDate: LocalDate
  checkOutDate: LocalDate
  createdAtUtc: Instant

RoomType
  SINGLE
  DOUBLE
  DELUXE
```

Use half-open booking ranges:

```text
[checkInDate, checkOutDate)
```

Two ranges overlap when:

```text
existingCheckIn < requestedCheckOut
    AND requestedCheckIn < existingCheckOut
```

This permits a new booking to start on the previous booking's checkout date.

Database rules in the first Flyway migration:

- Required columns and foreign keys.
- `capacity >= 1`.
- `guest_count >= 1`.
- `check_in_date < check_out_date`.
- Unique booking reference.
- Unique room number within a hotel.
- Room type stored as a readable string.
- Index hotel names for search.
- Index room filtering by hotel, type, and capacity.
- Index booking overlap queries by hotel, room, check-in, and checkout.

Keep business validation in Java as well as database constraints. The database
constraints are the final integrity boundary, not the API's primary error
handling mechanism.

## REST API

Preserve the existing surface:

```text
GET  /api/hotels?name=...
GET  /api/hotels/{hotelId}/rooms/available
POST /api/bookings
GET  /api/bookings/{reference}
POST /api/admin/seed
POST /api/admin/reset
GET  /actuator/health
GET  /v3/api-docs
GET  /swagger-ui/index.html
```

Availability query parameters:

```text
checkIn=yyyy-MM-dd
checkOut=yyyy-MM-dd
guests=2
roomType=DOUBLE       # optional, case-insensitive at the API boundary
```

Example booking request:

```json
{
  "hotelId": "00000000-0000-0000-0000-000000000001",
  "guestName": "Ada Lovelace",
  "guestCount": 2,
  "checkInDate": "2026-08-01",
  "checkOutDate": "2026-08-03",
  "roomType": "DOUBLE"
}
```

Response behavior:

- `200 OK` for successful searches and booking lookup.
- `201 Created` with a `Location` header for a new booking.
- `204 No Content` for reset.
- `400 Bad Request` for validation and invalid date ranges.
- `404 Not Found` for a missing hotel or booking.
- `409 Conflict` when no suitable room remains.
- RFC 9457-style `ProblemDetail` JSON for errors.

Define the API contract with DTOs. Do not serialize JPA entities directly.

## Business Rules

- Check-in may be today or later, based on an injected UTC `Clock`.
- Check-in must be before checkout.
- Guest count must be at least one.
- Guest name must not be blank.
- A selected room's capacity must be at least the guest count.
- One room must cover the whole stay.
- A room cannot have an overlapping booking.
- Booking references must be unique.
- Optional room type restricts candidates to that type.
- Without a requested type, select deterministically by capacity, room type
  order (`SINGLE`, `DOUBLE`, `DELUXE`), then room number.

Put date overlap and deterministic ordering in pure Java functions so the
highest-risk rules can be unit tested without Spring.

## Seed and Reset

Keep the deterministic source behavior:

```text
POST /api/admin/seed
  1. Reset existing data.
  2. Insert the fixed hotel and six rooms.

POST /api/admin/reset
  Delete Bookings -> Rooms -> Hotels.
```

Seed:

```text
Grand Plaza Hotel
ID: 00000000-0000-0000-0000-000000000001

101 SINGLE capacity 1
102 SINGLE capacity 1
201 DOUBLE capacity 2
202 DOUBLE capacity 2
301 DELUXE capacity 4
302 DELUXE capacity 4
```

Use fixed UUIDs for the hotel and rooms. Do not seed bookings. Return the hotel
ID, name, and room count from the seed endpoint.

## Booking and Concurrency Design

The booking transaction is the most important part of the implementation.

1. Validate the command before opening a transaction.
2. Verify the hotel exists.
3. Enter a bounded retry wrapper.
4. Start a new SQL Server transaction at `SERIALIZABLE`.
5. Query suitable rooms and exclude overlapping bookings.
6. Select the first room in deterministic order.
7. Generate a booking reference.
8. Insert and flush the booking.
9. Commit.
10. Load and return the booking details.

Use a retry wrapper outside a `TransactionTemplate`, so every retry creates and
replays a complete new transaction. Do not place retry inside one transaction.
Retry only recognized transient SQL Server failures such as deadlock victims;
do not retry validation errors or normal no-availability conflicts.

The initial implementation should retain SQL Server and serializable range
protection because that behavior has already been proven in the .NET version.
Do not replace it with a JVM `synchronized` block or in-memory lock, which
would fail with multiple API replicas.

Generate references in a stable `HB-...` format and enforce uniqueness in SQL
Server. Handle the rare unique-reference collision inside the bounded retry
boundary.

## Data and Local Development

Use SQL Server in every relational path:

```text
Local:       SQL Server 2022 via Docker Compose
Integration: disposable SQL Server via Testcontainers
Azure:       Azure SQL Database serverless, optional
```

Do not use H2 as a substitute for relational integration tests. Its SQL,
locking, date, and transaction behavior is not the behavior being shipped.

Local configuration comes from environment variables, with non-secret
defaults documented in `.env.example`. Never commit a real password.

Flyway owns schema creation and changes. Configure Hibernate with schema
validation rather than automatic DDL creation:

```text
spring.jpa.hibernate.ddl-auto=validate
```

## Test Plan

### Unit tests

- Valid and invalid date ranges.
- Past, today, and future check-in.
- Half-open overlap behavior.
- Back-to-back bookings do not overlap.
- Guest-count and capacity rules.
- Deterministic room ordering.

### Repository and model tests

- Unique booking reference.
- Unique room number per hotel.
- Database check constraints.
- Enum persistence.
- Required relationships and delete behavior.
- Flyway can create an empty database and Hibernate validates it.

### API integration tests

- OpenAPI JSON, Swagger UI, and health are available.
- Seed is repeatable and returns predictable IDs.
- Reset removes all data.
- Hotel name search works.
- Availability filters by date, guests, and optional room type.
- Invalid availability requests return `400`.
- Booking returns `201`, a location, and lookup details.
- Capacity and overlap violations are rejected.
- Missing hotel and booking return `404`.
- Exhausted availability returns `409`.
- Booking today is accepted; a past check-in is rejected.

### Deterministic concurrency tests

Run against Testcontainers SQL Server:

1. Seed two suitable rooms.
2. Pre-book one room.
3. Pause two requests after both availability reads using a test-only latch or
   repository test hook.
4. Release them together.
5. Assert exactly one returns `201` and one returns `409`.
6. Assert the database has no overlapping bookings for the same room.

Also verify two concurrent back-to-back stays can use the same room.

## Delivery Phases

### Phase 1 — Bootstrap

- Generate the Spring Boot Maven project and wrapper.
- Add dependencies, configuration profiles, and package structure.
- Add Docker Compose for SQL Server.
- Add the initial Flyway migration.
- Verify build, migration, and application startup.

### Phase 2 — Domain and test data

- Implement entities, repositories, domain rules, and injected `Clock`.
- Implement deterministic seed/reset services and endpoints.
- Add Flyway/model and seed/reset integration tests.

### Phase 3 — Search and availability

- Implement hotel name search.
- Implement availability filtering and deterministic ordering.
- Add validation, DTO mapping, and unit/integration tests.

### Phase 4 — Booking

- Implement serializable booking transaction and full-transaction retry.
- Implement booking-reference generation and lookup.
- Map errors to `ProblemDetail`.
- Add normal booking, overlap, back-to-back, and deterministic concurrency
  tests.

### Phase 5 — Reviewer experience

- Add springdoc OpenAPI, Swagger UI, Actuator health, and example HTTP calls.
- Add a README covering prerequisites, build, test, run, seed, reset, and a
  complete manual booking flow.
- Add dependency and formatting checks.
- Build and exercise the application container with local SQL Server.

### Phase 6 — Optional Azure parity

- Add a multi-stage Java container image.
- Reuse the existing Azure shape: GHCR commit-SHA image to Azure Container Apps
  Consumption and Azure SQL Database serverless.
- Translate only application-specific environment variables and container
  settings; retain the useful Bicep architecture from the source project.

## Definition of Done

- `./mvnw verify` passes with Docker running.
- The API and SQL Server start through Docker Compose.
- Flyway builds a database from empty state.
- Swagger supports the full manual test flow.
- Seed/reset is deterministic.
- All business-rule and API tests pass.
- The forced concurrency test proves a room cannot be double booked.
- No secrets are committed.
- README instructions work from a clean checkout.

## Deliberately Deferred

- Authentication and authorization.
- Frontend.
- Cancellation, modification, payment, pricing, or inventory management.
- Idempotency keys.
- Email or messaging.
- Caching.
- Event-driven architecture.
- Observability infrastructure beyond logs and health.
- Azure deployment until the local implementation and concurrency tests pass.
