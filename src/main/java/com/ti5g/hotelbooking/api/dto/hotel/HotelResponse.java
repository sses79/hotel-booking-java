package com.ti5g.hotelbooking.api.dto.hotel;

import java.util.UUID;

public record HotelResponse(
		UUID id,
		String name) {
}
