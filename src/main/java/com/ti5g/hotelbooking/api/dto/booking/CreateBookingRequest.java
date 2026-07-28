package com.ti5g.hotelbooking.api.dto.booking;

import java.time.LocalDate;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
		@NotNull
		@Schema(example = "00000000-0000-0000-0000-000000000001")
		UUID hotelId,
		@NotBlank
		@Size(max = 200)
		@Schema(example = "Ada Lovelace")
		String guestName,
		@Min(1)
		@Schema(example = "2", minimum = "1")
		int guestCount,
		@NotNull
		@Schema(example = "2030-08-01", type = "string", format = "date")
		LocalDate checkInDate,
		@NotNull
		@Schema(example = "2030-08-03", type = "string", format = "date")
		LocalDate checkOutDate,
		@Schema(
			description = "Optional preferred room type",
			example = "DOUBLE")
		RoomType roomType) {
}
