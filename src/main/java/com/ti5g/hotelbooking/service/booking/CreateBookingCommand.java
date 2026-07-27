package com.ti5g.hotelbooking.service.booking;

import java.time.LocalDate;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;

public record CreateBookingCommand(
		UUID hotelId,
		String guestName,
		int guestCount,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		RoomType roomType) {
}
