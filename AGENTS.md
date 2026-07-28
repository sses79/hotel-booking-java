# Repository Guidelines

## Project Structure & Module Organization

Application code lives in `src/main/java/com/ti5g/hotelbooking`. Add new code
in feature-oriented packages beneath that root, keeping controllers, services,
repositories, and domain types close to their feature. Runtime configuration is
in `src/main/resources`; local overrides belong in `application-local.yml`.
Flyway owns the database schema through versioned files in
`src/main/resources/db/migration`. Tests mirror the main package under
`src/test/java`. Local SQL Server infrastructure is defined in
`infra/local/compose.yaml`, while design and delivery notes live in `docs/`.

## Build, Test, and Development Commands

- `./mvnw verify` compiles, runs all tests, and packages the application.
  Docker Desktop must be running because integration tests use Testcontainers.
- `docker compose --env-file .env -f infra/local/compose.yaml up -d --wait sql sql-init`
  starts SQL Server and creates the local `HotelBooking` database.
- Add `--build` and omit the service names to run the containerized API too.
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` runs the API after
  variables from `.env` have been exported.
- `docker compose --env-file .env -f infra/local/compose.yaml down` stops local
  services while preserving database data.

Use Java 21. Always use the Maven Wrapper rather than a globally installed
Maven version.

## Coding Style & Naming Conventions

Follow the existing Spring Java style: tabs for Java indentation, braces on the
same line, and one public top-level type per file. Use `PascalCase` for classes,
`lowerCamelCase` for methods and fields, and `UPPER_SNAKE_CASE` for constants.
Use two spaces in YAML. Prefer constructor injection for new components. Name
Flyway migrations `V<n>__<lowercase_description>.sql`; never edit an applied
migration—add a new version instead.

## Testing Guidelines

Tests use JUnit 5, AssertJ, Spring Boot Test, and Testcontainers SQL Server.
Name test classes `*Tests` and test methods after observable behavior, such as
`flywayCreatesTheInitialSchema`. Add focused unit tests for business rules and
integration tests for repositories, migrations, transactions, and concurrency.
No coverage threshold is configured; every behavior change should include
meaningful tests. Run `./mvnw verify` before pushing.

## Commit & Pull Request Guidelines

The history uses short, imperative commit subjects, for example
`Initialize Java hotel booking service`. Keep each commit scoped to one logical
change. Pull requests should explain what changed, why, API or schema impact,
and verification performed. Link relevant issues and include request/response
examples for endpoint changes. Target `main` from a development branch and
ensure CI is green before merge.

## Security & Configuration

Never commit `.env`, credentials, connection strings, or generated `target/`
content. Keep safe placeholders in `.env.example`. Recreating the Docker volume
with `down -v` deletes all local database data.
