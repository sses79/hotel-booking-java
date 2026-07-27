package com.ti5g.hotelbooking.service.booking;

public class NoRoomAvailableException extends RuntimeException {

	public NoRoomAvailableException() {
		super("No room is available for the requested stay and guest count.");
	}
}
