package com.ti5g.hotelbooking.service.availability;

import java.util.UUID;

public class HotelNotFoundException extends RuntimeException {

	public HotelNotFoundException(UUID hotelId) {
		super("Hotel '%s' was not found.".formatted(hotelId));
	}
}
