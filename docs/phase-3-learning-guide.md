# Phase 3 Learning Guide

Phase 3 adds hotel search and room availability. The 80/20 lesson is that most
read APIs become understandable when four responsibilities stay separate.

## 1. Controllers Translate HTTP

`HotelsController` accepts path and query parameters and returns response DTOs.
`RoomTypeConverter` makes values such as `double`, `DOUBLE`, and ` Double `
equivalent at the API boundary.

Controllers do not expose JPA entities. `HotelResponse` and
`AvailableRoomResponse` keep the public contract independent from database
mapping decisions.

## 2. Services Describe the Use Case

`HotelSearchService` normalizes optional search text. `RoomAvailabilityService`
checks dates, guest count, and hotel existence before asking the repository for
candidates.

```text
HTTP parameters
    ↓
business validation
    ↓
database candidate query
    ↓
deterministic domain ordering
    ↓
response DTOs
```

The injected UTC `Clock` makes “today” a controlled dependency rather than
hidden global state.

## 3. SQL Removes Unavailable Candidates

The availability repository query asks SQL Server for rooms that:

- belong to the requested hotel;
- have sufficient capacity;
- match the optional room type; and
- have no overlapping booking.

The overlap condition uses half-open stays:

```text
existing check-in < requested checkout
and requested check-in < existing checkout
```

Therefore a checkout on August 3 and a new check-in on August 3 do not overlap.
Filtering in SQL avoids loading every booking into Java.

## 4. Deterministic Ordering Is a Business Rule

SQL finds eligible rooms, but `BookingRules.orderRoomsForBooking` defines their
stable order: capacity, room type (`SINGLE`, `DOUBLE`, `DELUXE`), then room
number.

The same ordering will let Phase 4 select the first available room predictably.
Keeping it in domain code prevents search and booking from drifting apart.

## Error Boundary

Expected client mistakes become RFC 9457 `ProblemDetail` responses:

- `400` for invalid dates, guest count, or parameter formats;
- `404` when the hotel does not exist.

Unexpected programming or infrastructure failures are not disguised as client
errors.

## What the Tests Prove

Fast unit tests cover date, capacity, overlap, ordering, and room-type parsing.
MockMvc plus Testcontainers proves the HTTP contract and the actual SQL Server
query, including case-insensitive search, filtering, overlaps, and back-to-back
stays.
