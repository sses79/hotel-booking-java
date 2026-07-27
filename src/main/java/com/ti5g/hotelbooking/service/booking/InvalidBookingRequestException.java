package com.ti5g.hotelbooking.service.booking;

public class InvalidBookingRequestException extends RuntimeException {

	public InvalidBookingRequestException(String message) {
		super(message);
	}
}
