package com.ti5g.hotelbooking.api.dto.admin;

import java.util.UUID;

public record SeedResponse(
		UUID hotelId,
		String hotelName,
		int roomsCreated) {
}
