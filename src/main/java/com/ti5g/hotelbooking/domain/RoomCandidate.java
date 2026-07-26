package com.ti5g.hotelbooking.domain;

import java.util.Objects;
import java.util.UUID;

public record RoomCandidate(
		UUID id,
		String roomNumber,
		RoomType roomType,
		int capacity) {

	public RoomCandidate {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(roomNumber, "roomNumber must not be null");
		Objects.requireNonNull(roomType, "roomType must not be null");
	}
}
