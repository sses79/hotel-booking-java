package com.ti5g.hotelbooking.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "bookings",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_bookings_reference",
				columnNames = "booking_reference"),
		indexes = @Index(
				name = "ix_bookings_hotel_room_dates",
				columnList = "hotel_id, room_id, check_in_date, check_out_date"))
public class BookingEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "booking_reference", nullable = false, length = 30)
	private String bookingReference;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "hotel_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_bookings_hotels"))
	private HotelEntity hotel;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "room_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_bookings_rooms"))
	private RoomEntity room;

	@Column(name = "guest_name", nullable = false, length = 200)
	private String guestName;

	@Column(name = "guest_count", nullable = false)
	private int guestCount;

	@Column(name = "check_in_date", nullable = false)
	private LocalDate checkInDate;

	@Column(name = "check_out_date", nullable = false)
	private LocalDate checkOutDate;

	@Column(name = "created_at_utc", nullable = false, columnDefinition = "datetimeoffset(7)")
	private Instant createdAtUtc;

	protected BookingEntity() {
	}

	public BookingEntity(
			UUID id,
			String bookingReference,
			HotelEntity hotel,
			RoomEntity room,
			String guestName,
			int guestCount,
			LocalDate checkInDate,
			LocalDate checkOutDate,
			Instant createdAtUtc) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.bookingReference = Objects.requireNonNull(
				bookingReference, "bookingReference must not be null");
		this.hotel = Objects.requireNonNull(hotel, "hotel must not be null");
		this.room = Objects.requireNonNull(room, "room must not be null");
		this.guestName = Objects.requireNonNull(guestName, "guestName must not be null");
		this.guestCount = guestCount;
		this.checkInDate = Objects.requireNonNull(checkInDate, "checkInDate must not be null");
		this.checkOutDate = Objects.requireNonNull(checkOutDate, "checkOutDate must not be null");
		this.createdAtUtc = Objects.requireNonNull(createdAtUtc, "createdAtUtc must not be null");
	}

	public UUID getId() {
		return id;
	}

	public String getBookingReference() {
		return bookingReference;
	}

	public HotelEntity getHotel() {
		return hotel;
	}

	public RoomEntity getRoom() {
		return room;
	}

	public String getGuestName() {
		return guestName;
	}

	public int getGuestCount() {
		return guestCount;
	}

	public LocalDate getCheckInDate() {
		return checkInDate;
	}

	public LocalDate getCheckOutDate() {
		return checkOutDate;
	}

	public Instant getCreatedAtUtc() {
		return createdAtUtc;
	}
}
