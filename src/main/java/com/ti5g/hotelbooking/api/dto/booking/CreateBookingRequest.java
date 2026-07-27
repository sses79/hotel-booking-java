package com.ti5g.hotelbooking.api.dto.booking;

import java.time.LocalDate;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
		@NotNull UUID hotelId,
		@NotBlank @Size(max = 200) String guestName,
		@Min(1) int guestCount,
		@NotNull LocalDate checkInDate,
		@NotNull LocalDate checkOutDate,
		RoomType roomType) {
}
