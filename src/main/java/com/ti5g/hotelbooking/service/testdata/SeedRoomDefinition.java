package com.ti5g.hotelbooking.service.testdata;

import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;

public record SeedRoomDefinition(
		UUID id,
		String roomNumber,
		RoomType roomType,
		int capacity) {
}
