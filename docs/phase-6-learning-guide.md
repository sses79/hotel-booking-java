# Phase 6 Learning Guide

Phase 6 adds a production-shaped native and Azure path without replacing the
fast JVM development loop. The 80/20 lesson is that native deployment is a
separate compilation target whose platform, runtime reachability, configuration,
and delivery boundary must all be proved explicitly.

## 1. AOT Moves Framework Decisions to Build Time

The `native-maven-plugin` opts the application into GraalVM Native Build Tools.
Spring Boot's inherited `native` Maven profile runs Spring AOT processing before
Paketo invokes native-image. Normal compilation targets Java 21; the isolated
Buildpacks environment uses NIK/GraalVM 25 because Spring Boot 4 requires that
native compiler generation.

```text
Java source → Spring AOT model → reachability metadata
    → GraalVM native compilation → machine executable
```

The closed-world compiler must know which classes, resources, reflection, and
proxies can be reached. Spring contributes most hints automatically; the smoke
test reveals gaps in third-party libraries such as JDBC, Flyway, or Springdoc.
The real build also found a JDK console object retained during analysis.
`src/main/resources/META-INF/native-image/com.ti5g/hotel-booking-runtime/native-image.properties`
keeps `Password$ConsoleHolder` initialized at runtime, preserving GraalVM's JNI
rule for `java.io.Console` without freezing a console object into the
executable.

**Lesson:** a successful Java compilation does not prove native reachability.

## 2. Keep JVM and Native Delivery Paths Separate

The existing `Dockerfile` still builds a Temurin JRE image. The native path uses
`-Pnative spring-boot:build-image` and the pinned Paketo Noble Java Tiny builder.
Its final image launches a native executable and does not contain a JVM.

JVM builds remain the default because they compile quickly and support the full
JUnit/Testcontainers suite. Native compilation runs after changes reach `main`,
where its longer feedback time buys deployment confidence.

**Lesson:** optimize the delivery artifact without making every development
cycle pay the optimization cost.

## 3. Native Images Belong to One Platform

GraalVM does not cross-compile. The Docker Buildpacks command on Apple Silicon
produces Linux ARM64; Azure's target is Linux AMD64. The native workflow
therefore runs on `ubuntu-latest`, asserts the image architecture, verifies
that the final image has no Java executable, and publishes only an immutable
commit-SHA tag.

```text
main commit SHA → Linux AMD64 build → smoke test → GHCR same SHA
```

**Lesson:** a native executable includes an operating-system and CPU contract,
not just an application version.

## 4. Smoke Tests Prove the Native Boundary

`scripts/smoke-test-api.sh` exercises health, OpenAPI, Swagger UI, Flyway-backed
seed data, search, availability, booking, and lookup. Both the JVM container CI
and native workflow use the same observable flow.

The 44 JVM tests prove detailed rules, transactions, and concurrency. The
native smoke test answers a narrower question: can the compiled delivery
artifact start and perform its critical paths with real SQL Server?

**Lesson:** use broad JVM coverage and a small, high-value native test rather
than duplicating the whole suite in a costly native build.

## 5. Infrastructure Translates Configuration, Not Architecture

The source project's useful Azure shape remains: Container Apps Consumption,
Azure SQL serverless, an immutable GHCR image, health probes, and scale 0–2.
The Bicep template replaces .NET settings with Spring's `azure` profile and
`DB_HOST`, `DB_NAME`, `DB_USER`, and secret-backed `DB_PASSWORD`.

Flyway still owns schema creation in Azure. The platform changes where the
application runs, not who owns persistence behavior.

**Lesson:** reuse stable infrastructure decisions while translating only the
application-specific contract.

## Try It Safely

Use the GitHub workflow for the resource-intensive build:

```bash
gh workflow run native-image.yml --ref main
gh run list --workflow native-image.yml --limit 1
```

The runner builds Linux AMD64, starts SQL Server, runs
`scripts/smoke-test-api.sh`, and publishes the commit-SHA image only after the
smoke test passes. A local build is optional and needs roughly 10 GB assigned
to Docker; keep using Temurin and `./mvnw verify` for routine feedback.

## Continuous-Learning Loop

1. Name the deployment behavior being optimized.
2. Identify what moves from runtime discovery to build-time knowledge.
3. Build for the exact target platform.
4. Prove the smallest critical path with real dependencies.
5. Treat a missing resource or reflection error as reachability evidence.
6. Carry the resulting hint or configuration lesson into the next library.
