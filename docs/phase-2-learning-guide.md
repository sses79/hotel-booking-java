# Phase 2 Learning Guide

Phase 2 turns the database foundation into a usable application model and adds
repeatable test data. The 80/20 lesson is that five boundaries explain most of
the implementation.

## 1. Domain Rules Express Decisions

`domain/BookingRules.java` contains decisions that do not require Spring or a
database: valid dates, guest names, capacity, overlap, and deterministic room
ordering.

```text
input values → business rule → true, false, or ordered result
```

The rules accept a `Clock` instead of reading the system date directly. Tests
can therefore freeze time and prove that today is accepted while yesterday is
rejected.

**Lesson:** Keep important decisions in ordinary Java so they are fast and easy
to test.

## 2. JPA Entities Map Java to Flyway Tables

`HotelEntity`, `RoomEntity`, and `BookingEntity` describe how Java fields map
to the tables created by Flyway:

```text
HotelEntity  1 ─── * RoomEntity
HotelEntity  1 ─── * BookingEntity
RoomEntity   1 ─── * BookingEntity
```

`HotelEntity.addRoom` maintains both sides of the hotel-room relationship.
Room types use `EnumType.STRING`, so SQL Server stores readable values such as
`DOUBLE` rather than fragile numeric positions.

**Lesson:** Flyway owns the schema; entities explain how Java uses it. Hibernate
`validate` stops startup when the two disagree.

## 3. Repositories Provide Persistence Operations

Spring Data turns interfaces such as `HotelRepository` into working database
adapters. The application receives them through constructor injection and uses
operations including `findById`, `count`, and `deleteAllInBatch`.

Repositories remove repetitive SQL for simple persistence. They do not replace
business rules or transaction design.

## 4. Transactions Protect Complete Operations

`TestDataService.seed` is one transaction:

```text
delete bookings
    ↓
delete rooms
    ↓
delete hotels
    ↓
insert fixed hotel and rooms
    ↓
commit everything
```

Deleting in dependency order respects foreign keys. Fixed UUIDs and a
reset-before-seed policy make every run produce the same result.

The seed uses `EntityManager.persist` because fixed, non-null UUIDs can make
Spring Data assume an entity already exists and call `merge`, causing avoidable
lookup queries.

**Lesson:** A transaction defines the all-or-nothing boundary; deterministic
data makes debugging and API demonstrations repeatable.

## 5. Tests Prove Each Boundary at the Right Cost

- Unit tests prove pure booking rules without starting Spring.
- Integration tests start real SQL Server through Testcontainers.
- Model tests prove enum storage, uniqueness, check constraints, and restrictive
  foreign keys.
- MockMvc tests prove HTTP status codes and JSON without opening a network port.

The shared `TestcontainersConfiguration` had to be public because Java
subpackage names do not grant access to package-private classes.

**Lesson:** Compile early to catch Java structure errors quickly, then use the
real database for behavior that depends on SQL Server.

## Request Flow

```text
POST /api/admin/seed
        ↓
AdminController
        ↓
TestDataService transaction
        ↓
Spring Data repositories / EntityManager
        ↓
Hibernate SQL
        ↓
Flyway-created SQL Server tables
```

The controller returns a `SeedResponse` DTO rather than exposing JPA entities.
This keeps the HTTP contract independent from persistence details.

## Reusable Development Loop

For the next feature:

1. Define the observable behavior.
2. Put decisions in pure domain code.
3. Map persistence explicitly.
4. Choose the transaction boundary.
5. Expose a DTO-based API.
6. Prove rules quickly and SQL behavior against SQL Server.
