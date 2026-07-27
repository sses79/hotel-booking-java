package com.ti5g.hotelbooking.service.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;

public record BookingDetails(
		String bookingReference,
		UUID hotelId,
		String hotelName,
		UUID roomId,
		String roomNumber,
		RoomType roomType,
		int roomCapacity,
		String guestName,
		int guestCount,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		Instant createdAtUtc) {
}
