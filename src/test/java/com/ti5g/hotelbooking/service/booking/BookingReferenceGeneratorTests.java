package com.ti5g.hotelbooking.service.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingReferenceGeneratorTests {

	private final BookingReferenceGenerator generator = new BookingReferenceGenerator();

	@Test
	void referenceUsesHumanFriendlyFixedWidthFormat() {
		assertThat(generator.nextReference()).matches("HB-\\d{6}");
	}
}
