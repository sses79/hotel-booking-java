package com.ti5g.hotelbooking.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class BookingRules {

	private static final Comparator<RoomCandidate> ROOM_SELECTION_ORDER =
			Comparator.comparingInt(RoomCandidate::capacity)
					.thenComparingInt(room -> roomTypeOrder(room.roomType()))
					.thenComparing(RoomCandidate::roomNumber, String.CASE_INSENSITIVE_ORDER);

	private BookingRules() {
	}

	public static boolean hasNonPastCheckInDate(LocalDate checkInDate, Clock clock) {
		Objects.requireNonNull(checkInDate, "checkInDate must not be null");
		Objects.requireNonNull(clock, "clock must not be null");

		return !checkInDate.isBefore(LocalDate.now(clock));
	}

	public static boolean hasValidDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
		Objects.requireNonNull(checkInDate, "checkInDate must not be null");
		Objects.requireNonNull(checkOutDate, "checkOutDate must not be null");

		return checkInDate.isBefore(checkOutDate);
	}

	public static boolean hasValidGuestCount(int guestCount) {
		return guestCount >= 1;
	}

	public static boolean hasValidGuestName(String guestName) {
		return guestName != null && !guestName.isBlank();
	}

	public static boolean canRoomHoldGuests(int roomCapacity, int guestCount) {
		return hasValidGuestCount(guestCount) && roomCapacity >= guestCount;
	}

	public static boolean dateRangesOverlap(
			LocalDate existingCheckInDate,
			LocalDate existingCheckOutDate,
			LocalDate requestedCheckInDate,
			LocalDate requestedCheckOutDate) {
		Objects.requireNonNull(existingCheckInDate, "existingCheckInDate must not be null");
		Objects.requireNonNull(existingCheckOutDate, "existingCheckOutDate must not be null");
		Objects.requireNonNull(requestedCheckInDate, "requestedCheckInDate must not be null");
		Objects.requireNonNull(requestedCheckOutDate, "requestedCheckOutDate must not be null");

		return existingCheckInDate.isBefore(requestedCheckOutDate)
				&& requestedCheckInDate.isBefore(existingCheckOutDate);
	}

	public static List<RoomCandidate> orderRoomsForBooking(Collection<RoomCandidate> rooms) {
		Objects.requireNonNull(rooms, "rooms must not be null");

		return rooms.stream()
				.sorted(ROOM_SELECTION_ORDER)
				.toList();
	}

	public static int roomTypeOrder(RoomType roomType) {
		Objects.requireNonNull(roomType, "roomType must not be null");

		return switch (roomType) {
			case SINGLE -> 1;
			case DOUBLE -> 2;
			case DELUXE -> 3;
		};
	}
}
