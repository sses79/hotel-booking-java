package com.ti5g.hotelbooking.service.availability;

import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;

public record AvailableRoomResult(
		UUID id,
		UUID hotelId,
		String roomNumber,
		RoomType roomType,
		int capacity) {
}
