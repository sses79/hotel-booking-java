package com.ti5g.hotelbooking.service.availability;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.BookingRules;
import com.ti5g.hotelbooking.domain.RoomCandidate;
import com.ti5g.hotelbooking.domain.RoomType;
import com.ti5g.hotelbooking.persistence.repository.HotelRepository;
import com.ti5g.hotelbooking.persistence.repository.RoomRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomAvailabilityService {

	private final HotelRepository hotelRepository;
	private final RoomRepository roomRepository;
	private final Clock clock;
	private final ObjectProvider<AvailabilityReadObserver> availabilityReadObservers;

	public RoomAvailabilityService(
			HotelRepository hotelRepository,
			RoomRepository roomRepository,
			Clock clock,
			ObjectProvider<AvailabilityReadObserver> availabilityReadObservers) {
		this.hotelRepository = hotelRepository;
		this.roomRepository = roomRepository;
		this.clock = clock;
		this.availabilityReadObservers = availabilityReadObservers;
	}

	@Transactional(readOnly = true)
	public List<AvailableRoomResult> findAvailableRooms(
			UUID hotelId,
			LocalDate checkInDate,
			LocalDate checkOutDate,
			int guests,
			RoomType roomType) {
		if (!BookingRules.hasNonPastCheckInDate(checkInDate, clock)) {
			throw new InvalidAvailabilityRequestException(
					"Check-in date cannot be in the past.");
		}
		if (!BookingRules.hasValidDateRange(checkInDate, checkOutDate)) {
			throw new InvalidAvailabilityRequestException(
					"Check-in date must be before check-out date.");
		}
		if (!BookingRules.hasValidGuestCount(guests)) {
			throw new InvalidAvailabilityRequestException(
					"Guest count must be at least 1.");
		}
		if (!hotelRepository.existsById(hotelId)) {
			throw new HotelNotFoundException(hotelId);
		}

		var rooms = roomRepository.findAvailableRooms(
				hotelId,
				checkInDate,
				checkOutDate,
				guests,
				roomType);

		availabilityReadObservers.orderedStream()
				.forEach(AvailabilityReadObserver::afterAvailabilityRead);

		var candidates = rooms
				.stream()
				.map(room -> new RoomCandidate(
						room.getId(),
						room.getRoomNumber(),
						room.getRoomType(),
						room.getCapacity()))
				.toList();

		return BookingRules.orderRoomsForBooking(candidates).stream()
				.map(room -> new AvailableRoomResult(
						room.id(),
						hotelId,
						room.roomNumber(),
						room.roomType(),
						room.capacity()))
				.toList();
	}
}
