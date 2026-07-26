package com.ti5g.hotelbooking.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRulesTests {

	private static final Clock FIXED_CLOCK =
			Clock.fixed(Instant.parse("2026-07-09T00:00:00Z"), ZoneOffset.UTC);

	@Test
	void checkInDateCannotBeBeforeToday() {
		var today = LocalDate.of(2026, 7, 9);

		assertThat(BookingRules.hasNonPastCheckInDate(today.plusDays(1), FIXED_CLOCK)).isTrue();
		assertThat(BookingRules.hasNonPastCheckInDate(today, FIXED_CLOCK)).isTrue();
		assertThat(BookingRules.hasNonPastCheckInDate(today.minusDays(1), FIXED_CLOCK)).isFalse();
	}

	@Test
	void checkOutDateMustBeAfterCheckInDate() {
		var checkIn = LocalDate.of(2026, 8, 1);

		assertThat(BookingRules.hasValidDateRange(checkIn, checkIn.plusDays(1))).isTrue();
		assertThat(BookingRules.hasValidDateRange(checkIn, checkIn)).isFalse();
		assertThat(BookingRules.hasValidDateRange(checkIn, checkIn.minusDays(1))).isFalse();
	}

	@Test
	void overlapReturnsTrueWhenDateRangesShareANight() {
		var overlaps = BookingRules.dateRangesOverlap(
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 3),
				LocalDate.of(2026, 8, 2),
				LocalDate.of(2026, 8, 4));

		assertThat(overlaps).isTrue();
	}

	@Test
	void overlapReturnsFalseForBackToBackBookings() {
		var overlaps = BookingRules.dateRangesOverlap(
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 3),
				LocalDate.of(2026, 8, 3),
				LocalDate.of(2026, 8, 5));

		assertThat(overlaps).isFalse();
	}

	@Test
	void roomCapacityMustCoverGuestCount() {
		assertThat(BookingRules.canRoomHoldGuests(2, 2)).isTrue();
		assertThat(BookingRules.canRoomHoldGuests(2, 3)).isFalse();
		assertThat(BookingRules.canRoomHoldGuests(2, 0)).isFalse();
	}

	@Test
	void guestNameMustContainVisibleText() {
		assertThat(BookingRules.hasValidGuestName("Ada Lovelace")).isTrue();
		assertThat(BookingRules.hasValidGuestName("  ")).isFalse();
		assertThat(BookingRules.hasValidGuestName(null)).isFalse();
	}

	@Test
	void roomOrderPrefersCapacityThenTypeThenStableRoomNumber() {
		var rooms = List.of(
				room("302", RoomType.DELUXE, 4),
				room("202", RoomType.DOUBLE, 2),
				room("201", RoomType.DOUBLE, 2),
				room("101", RoomType.SINGLE, 1));

		var orderedRoomNumbers = BookingRules.orderRoomsForBooking(rooms).stream()
				.map(RoomCandidate::roomNumber)
				.toList();

		assertThat(orderedRoomNumbers).containsExactly("101", "201", "202", "302");
	}

	private static RoomCandidate room(String roomNumber, RoomType roomType, int capacity) {
		return new RoomCandidate(UUID.randomUUID(), roomNumber, roomType, capacity);
	}
}
