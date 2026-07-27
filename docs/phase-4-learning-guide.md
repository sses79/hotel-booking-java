# Phase 4 Learning Guide

Phase 4 adds booking creation and lookup. The 80/20 lesson is that booking
correctness depends less on controller code and more on where the transaction
and retry boundaries sit.

## 1. The Transaction Owns the Decision

A booking is one indivisible decision:

```text
find an available room → select it → insert booking → commit
```

`BookingService` runs that complete sequence in a new `SERIALIZABLE`
transaction. SQL Server then protects the availability read from a conflicting
write. A JVM lock would protect only one application process and would fail
after the API scales to multiple replicas.

## 2. Retry the Whole Transaction

Two correct serializable transactions can deadlock when they race. SQL Server
chooses one as a victim so the other can finish.

`TransactionRetryExecutor` sits outside `TransactionTemplate`. On a recognized
transient SQL Server error, it starts the entire operation again in a fresh
transaction:

```text
attempt → rollback → short backoff → new transaction → re-read availability
```

Never retry inside the failed transaction. Its state is no longer trustworthy.
Validation errors and ordinary “no room” results are not transient, so they are
not retried. A rare booking-reference uniqueness collision also restarts the
complete transaction so a fresh reference can be generated safely.

## 3. Availability Has One Meaning

Booking creation reuses `RoomAvailabilityService`, including capacity, optional
room type, half-open date overlap, and deterministic ordering. This prevents
the search endpoint and booking command from developing different definitions
of “available.”

The first ordered candidate is selected. Back-to-back stays can share a room
because `[check-in, checkout)` excludes the checkout date.

## 4. HTTP Is an Explicit Boundary

`BookingsController` translates JSON into a command and returns a response DTO.
A successful create returns `201 Created` plus a `Location` header. Lookup by
the generated `HB-######` reference returns `200`.

Expected failures use RFC 9457 `ProblemDetail`:

- `400` for invalid input;
- `404` for a missing hotel or booking;
- `409` when no room remains or concurrent retries are exhausted.

## What the Tests Prove

Fast unit tests cover reference format and retry policy. MockMvc and a real
Testcontainers SQL Server cover normal creation, lookup, validation,
availability conflicts, and back-to-back stays.

The concurrency test uses a test-only barrier to pause two requests after both
availability reads. Releasing them together forces the risky race and proves
that exactly one request books the final matching room. A second race proves
that concurrent back-to-back bookings both succeed on the same room.
