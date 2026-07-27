package com.ti5g.hotelbooking.api.dto.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;

public record BookingResponse(
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
