package com.ti5g.hotelbooking.service.testdata;

import java.util.UUID;

public record SeedResult(
		UUID hotelId,
		String hotelName,
		int roomsCreated) {
}
