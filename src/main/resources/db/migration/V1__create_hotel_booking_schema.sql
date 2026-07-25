CREATE TABLE hotels (
    id uniqueidentifier NOT NULL,
    name nvarchar(200) NOT NULL,
    CONSTRAINT pk_hotels PRIMARY KEY (id)
);

CREATE INDEX ix_hotels_name
    ON hotels (name);

CREATE TABLE rooms (
    id uniqueidentifier NOT NULL,
    hotel_id uniqueidentifier NOT NULL,
    room_number nvarchar(20) NOT NULL,
    room_type nvarchar(20) NOT NULL,
    capacity int NOT NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT fk_rooms_hotels
        FOREIGN KEY (hotel_id) REFERENCES hotels (id) ON DELETE CASCADE,
    CONSTRAINT ck_rooms_room_type
        CHECK (room_type IN ('SINGLE', 'DOUBLE', 'DELUXE')),
    CONSTRAINT ck_rooms_capacity
        CHECK (capacity >= 1),
    CONSTRAINT uq_rooms_hotel_room_number
        UNIQUE (hotel_id, room_number)
);

CREATE INDEX ix_rooms_hotel_type_capacity
    ON rooms (hotel_id, room_type, capacity);

CREATE TABLE bookings (
    id uniqueidentifier NOT NULL,
    booking_reference nvarchar(30) NOT NULL,
    hotel_id uniqueidentifier NOT NULL,
    room_id uniqueidentifier NOT NULL,
    guest_name nvarchar(200) NOT NULL,
    guest_count int NOT NULL,
    check_in_date date NOT NULL,
    check_out_date date NOT NULL,
    created_at_utc datetimeoffset(7) NOT NULL,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT fk_bookings_hotels
        FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT fk_bookings_rooms
        FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT uq_bookings_reference
        UNIQUE (booking_reference),
    CONSTRAINT ck_bookings_guest_count
        CHECK (guest_count >= 1),
    CONSTRAINT ck_bookings_date_range
        CHECK (check_in_date < check_out_date)
);

CREATE INDEX ix_bookings_hotel_room_dates
    ON bookings (hotel_id, room_id, check_in_date, check_out_date);
