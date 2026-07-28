# Phase 5 Learning Guide

Phase 5 does not add business behavior. It makes the existing behavior easy to
understand, run, and trust. The 80/20 lesson is that a reviewer needs four
things: a clear entry point, repeatable examples, automated guardrails, and one
production-shaped execution path.

## 1. Documentation Is Part of the API

Springdoc discovers controllers automatically, while
`OpenApiConfiguration` supplies the API purpose and tag structure. Controller
operations explain intent and the booking request carries realistic examples.
Swagger UI turns the resulting OpenAPI document into an executable review
surface.

The OpenAPI and Swagger tests protect this surface from accidental removal.
They verify the documented paths rather than only checking that a JSON document
exists.

## 2. One Flow Should Prove the System

`requests/hotel-booking.http` follows the shortest useful journey:

```text
health → reset → seed → search → availability → book → lookup
```

Fixed hotel IDs and future dates remove guesswork. The booking lookup reads the
reference from the create response, so the flow can be replayed without editing
generated data.

## 3. Guardrails Belong in the Build

Spotless rejects trailing whitespace, missing final newlines, unused imports,
and wildcard imports. Run `./mvnw spotless:apply` to make safe mechanical
corrections without rewriting intentional code layout or text-block content.

Maven Enforcer checks Java and Maven versions, duplicate declarations, and
dependency convergence. Both tools run during `./mvnw verify`, so local builds
and CI enforce the same contract.

## 4. Containers Test the Delivery Boundary

The multi-stage `Dockerfile` compiles with Java 21 and copies only the runnable
JAR into a smaller JRE image. The runtime uses a non-root user and exposes an
Actuator health check.

Docker Compose starts SQL Server, creates the database, then starts the API.
Flyway still owns schema creation inside that database. CI builds this same
image and smoke-tests health, OpenAPI, seed, and search. This catches failures
that unit tests cannot, such as an incorrect image, port, profile, or database
hostname.
