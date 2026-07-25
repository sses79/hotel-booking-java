# Phase 1 Learning Guide

Phase 1 is the bootstrap foundation that makes later feature development
predictable.

## The 80/20 View

Five ideas explain most of the current codebase.

### 1. Spring Boot Assembles the Application

`HotelBookingApplication.java` is deliberately small:

```java
@SpringBootApplication
public class HotelBookingApplication {
	public static void main(String[] args) {
		SpringApplication.run(HotelBookingApplication.class, args);
	}
}
```

`@SpringBootApplication` starts the application, discovers components beneath
`com.ti5g.hotelbooking`, and configures features based on the dependencies
in `pom.xml`.

**Lesson:** Put future packages beneath the root application package so Spring
can discover their components automatically.

### 2. Dependencies Describe Capabilities

`pom.xml` provides the application’s capabilities:

- Web MVC: HTTP APIs
- JPA and Hibernate: database mapping
- Flyway: schema migrations
- Jakarta Validation: input validation
- Actuator: health checks
- SpringDoc: Swagger and OpenAPI
- Testcontainers: tests against real SQL Server

**Lesson:** A dependency makes a capability available; it does not design the
feature for us.

### 3. Flyway Owns the Schema

`V1__create_hotel_booking_schema.sql` creates the tables. Hibernate uses
`ddl-auto: validate`, so Flyway changes the database while Hibernate checks that
Java mappings agree with it.

**Lesson:** Database history should be explicit and reviewable. Never edit an
applied migration; add the next version, such as `V2__add_guest_email.sql`.

### 4. Configuration Changes by Environment

- `application.yml` contains shared behavior.
- `application-local.yml` contains local database settings.
- `.env` contains uncommitted local secrets.
- `.env.example` documents required variables safely.

**Lesson:** Keep application code consistent across environments and supply
environment-specific differences through configuration.

### 5. Test the Real Boundary

`TestcontainersConfiguration.java` starts an actual SQL Server container. The
integration test proves that Spring starts, SQL Server is reachable, Flyway
runs, and the expected tables exist.

**Lesson:** Prefer tests that prove observable system behavior over tests that
only prove individual methods execute.

## Phase 1 Execution Loop

```text
Maven Wrapper
    ↓
Compile application
    ↓
Start disposable SQL Server
    ↓
Run Flyway migration
    ↓
Start Spring context
    ↓
Validate schema
    ↓
Run tests and package
```

Run the complete loop with:

```bash
./mvnw verify
```

## Lessons Already Discovered

- SQL Server passwords persist in Docker volumes. Changing `.env` does not
  change an existing volume.
- SQL Server uses AMD64 emulation on an Apple Silicon Mac, so startup is slower.
- A healthy database container is insufficient; the application must also
  connect and migrate successfully.
- `contextLoads()` is a basic smoke test.
  `flywayCreatesTheInitialSchema()` proves observable database behavior.

## Continuous-Learning Workflow

Use this cycle for every future feature:

1. **Goal:** What user behavior are we adding?
2. **Core principle:** Which Java or Spring concept makes it work?
3. **Smallest implementation:** What is the minimum useful change?
4. **Proof:** Which test demonstrates the behavior?
5. **Failure lesson:** What did errors reveal?
6. **Takeaway:** What knowledge transfers to the next feature?

For example, adding entities should teach how a domain concept becomes a Java
type, a JPA mapping, a database constraint, and a repeatable integration test.
