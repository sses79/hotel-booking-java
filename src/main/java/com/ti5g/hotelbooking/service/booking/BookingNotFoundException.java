package com.ti5g.hotelbooking.service.booking;

public class BookingNotFoundException extends RuntimeException {

	public BookingNotFoundException(String bookingReference) {
		super("Booking '%s' was not found.".formatted(bookingReference));
	}
}
