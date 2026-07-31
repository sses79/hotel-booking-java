# Hotel Booking API

Java and Spring Boot implementation of the hotel booking backend challenge.
Phases 1 through 6 provide the application scaffold, SQL Server development
environment, Flyway schema, JPA model, domain rules, deterministic test data,
hotel search, room availability, health endpoint, OpenAPI tooling, and SQL
Server-backed booking and concurrency tests. Phase 5 adds reviewer-focused API
documentation, build guardrails, HTTP examples, and an application container.
Phase 6 adds an optional GraalVM native image and Azure deployment path while
retaining the Temurin JVM workflow for normal development.

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

`verify` also checks Java formatting and dependency convergence. Apply safe
formatting fixes with:

```bash
./mvnw spotless:apply
```

## Continuous Integration

GitHub Actions validates Docker Compose, formatting, dependencies, and the
complete Maven suite with Temurin Java 21. It then builds the application image
and smoke-tests it with SQL Server. Integration tests create their own SQL
Server through Testcontainers, so CI does not need database secrets.

After changes reach `main`, the native-image workflow builds a Linux AMD64
GraalVM executable with Spring AOT and Paketo Buildpacks. It smoke-tests that
image before publishing `ghcr.io/sses79/hotel-booking-java:<commit-sha>`.

## GraalVM Native Container

The native release is intentionally built by
`.github/workflows/native-image.yml` on GitHub's Linux AMD64 runner. This keeps
the CPU- and memory-intensive compilation out of the normal Mac development
loop and produces the architecture Azure needs.

After the workflow exists on `main`, start a release build with:

```bash
gh workflow run native-image.yml --ref main
```

Docker can also build a host-architecture native container without a local
GraalVM installation. This is optional: native-image can overwhelm a
memory-constrained Mac. Only build locally when Docker can use about 10 GB
while macOS still has adequate headroom; otherwise use the GitHub workflow.

```bash
./mvnw \
  -Pnative \
  -DskipTests \
  spring-boot:build-image \
  -Dspring-boot.build-image.imageName=hotel-booking-java:native
```

Run it against local SQL Server on port `8081`, then execute the complete smoke
flow:

```bash
NATIVE_IMAGE_NAME=hotel-booking-java:native \
  docker compose \
  --env-file .env \
  -f infra/local/compose.yaml \
  --profile native \
  up -d sql sql-init app-native

BASE_URL=http://localhost:8081 ./scripts/smoke-test-api.sh
```

On Apple Silicon, this creates a Linux ARM64 image. Release images are built on
GitHub's Linux AMD64 runner because native executables are platform-specific.
The application continues to compile to Java 21 bytecode, while the Paketo
builder uses NIK/GraalVM 25 because Spring Boot 4 requires that native compiler
generation.

## Run Locally

Create local environment configuration:

```bash
cp .env.example .env
```

Start only SQL Server and create the `HotelBooking` database:

```bash
docker compose --env-file .env -f infra/local/compose.yaml up -d --wait sql sql-init
```

Run the API with the local profile:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Alternatively, build and run the API and SQL Server entirely in containers:

```bash
docker compose --env-file .env -f infra/local/compose.yaml up --build -d --wait
```

The containerized API is available at `http://localhost:8080` and reports ready
only after `/actuator/health` succeeds.

Available endpoints:

```text
GET http://localhost:8080/api/hotels?name=Grand
GET http://localhost:8080/api/hotels/{hotelId}/rooms/available?checkIn=2030-08-01&checkOut=2030-08-03&guests=2&roomType=DOUBLE
POST http://localhost:8080/api/bookings
GET http://localhost:8080/api/bookings/{reference}
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

## Manual Booking Flow

Set reusable values:

```bash
BASE_URL=http://localhost:8080
HOTEL_ID=00000000-0000-0000-0000-000000000001
```

Reset, seed, search, and check availability:

```bash
curl -i -X POST "$BASE_URL/api/admin/reset"
curl -i -X POST "$BASE_URL/api/admin/seed"
curl "$BASE_URL/api/hotels?name=Grand"
curl "$BASE_URL/api/hotels/$HOTEL_ID/rooms/available?checkIn=2030-08-01&checkOut=2030-08-03&guests=2&roomType=DOUBLE"
```

Create a booking:

```bash
curl -i -X POST "$BASE_URL/api/bookings" \
  -H 'Content-Type: application/json' \
  -d '{
    "hotelId": "00000000-0000-0000-0000-000000000001",
    "guestName": "Ada Lovelace",
    "guestCount": 2,
    "checkInDate": "2030-08-01",
    "checkOutDate": "2030-08-03",
    "roomType": "DOUBLE"
  }'
```

Use the returned reference to retrieve the booking:

```bash
curl "$BASE_URL/api/bookings/HB-123456"
```

The create response is `201 Created` and its `Location` header contains the
lookup URL. Booking conflicts return an RFC 9457 response with status `409`.
The executable [HTTP request collection](requests/hotel-booking.http) performs
the same flow and carries the generated reference into the lookup request.

Swagger UI provides another manual path:

```text
http://localhost:8080/swagger-ui/index.html
```

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

## Optional Azure Deployment

`infra/bicep` provisions an Azure Container Apps Consumption environment and
an Azure SQL Database serverless database. The application uses the `azure`
Spring profile, encrypted SQL connections, Flyway migrations, health probes,
scale-to-zero, and the immutable native GHCR image.

See [Azure deployment](docs/azure-deployment.md) for parameters, preview,
deployment, cost, registry visibility, and verification commands.
