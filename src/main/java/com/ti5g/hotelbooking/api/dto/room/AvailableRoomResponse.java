package com.ti5g.hotelbooking.api.dto.room;

import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;

public record AvailableRoomResponse(
		UUID id,
		UUID hotelId,
		String roomNumber,
		RoomType roomType,
		int capacity) {
}
