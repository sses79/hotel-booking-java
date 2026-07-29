# Java and .NET Implementation Comparison

This document compares the Java implementation in this repository with the
.NET implementation in `/Users/tim/Yun/HoltelBooking`. The applications share
the same hotel-booking domain, but they are not currently interchangeable at
the API, schema, or operational level.

## Architecture and Dependency Injection

The .NET solution separates API, services, repositories, and models into
multiple projects. Its dependency graph is registered explicitly in
`HotelBooking.Api/Program.cs` and service collection extensions. These project
boundaries provide compile-time separation and make the complete application
wiring visible in one place.

The Java application is a single Maven module organized into packages beneath
`com.ti5g.hotelbooking`. Spring discovers `@Service`, `@Repository`, and
`@RestController` components and injects constructor dependencies. This reduces
manual wiring, although architectural boundaries rely more on package
conventions.

## API Contract Differences

- Operational endpoints differ: .NET uses `/health`, `/openapi/v1.json`, and
  `/swagger`; Java uses `/actuator/health`, `/v3/api-docs`, and
  `/swagger-ui/index.html`.
- Room types are serialized differently. .NET produces values such as
  `"Single"`, while Java produces `"SINGLE"`.
- Java returns `404` when availability is requested for a nonexistent hotel.
  The .NET query can return an empty list instead.
- Java centralizes errors in `ApiExceptionHandler`; .NET primarily maps service
  results inside individual controllers.
- Java uses `@NotBlank` for the guest name. The .NET combination of
  `[Required]` and `[MinLength(1)]` can accept whitespace, which is subsequently
  trimmed to an empty value.

These differences should be resolved through an explicit shared API contract
before clients are expected to switch between implementations.

## Persistence and Schema Management

The .NET implementation uses EF Core code-first migrations, with the
`HotelBookingDbContext` model as the schema source. The Java implementation
uses SQL-first Flyway migrations in `src/main/resources/db/migration`;
Hibernate validates rather than creates the schema.

Java local startup is more self-contained: the Compose `sql-init` service
creates the database and Flyway applies migrations when the application starts.
The .NET Compose environment does not automatically perform the equivalent
fresh-database migration.

Although the logical models are similar, their physical schemas differ.
.NET uses names such as `HotelId`, while Java uses `hotel_id`. The two
applications therefore cannot safely share their existing schemas.

The Java room-type column has an explicit database `CHECK` constraint. The
.NET enum conversion does not currently create the equivalent constraint.

## Queries, Transactions, and Concurrency

Both implementations use serializable transactions and retry transient booking
conflicts. .NET delegates retry classification to the EF Core SQL Server
execution strategy. Java explicitly recognizes selected SQL Server error codes
in `TransactionRetryExecutor`.

.NET propagates `CancellationToken` from HTTP requests into EF Core operations.
Java currently has no equivalent request-cancellation path. Conversely, Java
limits booking-reference generation to 20 attempts; the .NET loop is unbounded.

The .NET hotel search uses a no-tracking SQL projection. Java loads entities and
maps them afterward, although its case-insensitive search semantics are more
explicit.

The Java availability query would benefit from an index matching its booking
conflict lookup:

```sql
CREATE INDEX ix_bookings_room_dates
    ON bookings (room_id, check_in_date, check_out_date);
```

This should be introduced as a new Flyway migration, never by editing an
already-applied migration.

## Testing, CI, and Operations

Both implementations have API integration tests, SQL Server Testcontainers,
and concurrent-booking tests. The Java suite additionally covers migrations,
retry classification, converters, and the Hibernate model. Its CI builds and
smoke-tests JVM and GraalVM native containers.

The .NET CI checks EF migration drift and runs a scheduled NuGet vulnerability
scan. Java does not currently have an equivalent scheduled dependency-security
check.

Java exposes Actuator liveness, readiness, and datasource health information.
The .NET `/health` endpoint currently indicates application process health but
does not register a SQL Server health check.

Java resets test data using bulk deletes. The .NET reset implementation loads
rows before removing them, which consumes more memory as data volume grows.
Both reset endpoints should be disabled or protected in production.

## Recommended Parity Priorities

1. Define matching status codes, enum representations, validation rules, and
   operational endpoint expectations.
2. Reject whitespace-only guest names in .NET.
3. Add the Java room-and-date availability index.
4. Define a reliable .NET migration-at-deployment or startup process.
5. Disable or authorize test-data reset endpoints in production.
6. Add scheduled Java dependency vulnerability scanning.
7. Load-test both applications with the same database, data set, pool sizes,
   and workload before making throughput or cost decisions.

For framework-specific context, see the official
[EF Core asynchronous programming guidance](https://learn.microsoft.com/en-us/ef/core/miscellaneous/async).
