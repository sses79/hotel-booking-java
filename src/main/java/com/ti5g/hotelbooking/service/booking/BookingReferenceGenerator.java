package com.ti5g.hotelbooking.service.booking;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class BookingReferenceGenerator {

	private static final int MINIMUM_NUMBER = 100_000;
	private static final int MAXIMUM_NUMBER = 1_000_000;

	private final SecureRandom random = new SecureRandom();

	public String nextReference() {
		return "HB-%06d".formatted(random.nextInt(MINIMUM_NUMBER, MAXIMUM_NUMBER));
	}
}
