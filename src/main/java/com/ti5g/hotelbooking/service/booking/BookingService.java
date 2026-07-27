package com.ti5g.hotelbooking.service.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.BookingRules;
import com.ti5g.hotelbooking.persistence.entity.BookingEntity;
import com.ti5g.hotelbooking.persistence.repository.BookingRepository;
import com.ti5g.hotelbooking.persistence.repository.HotelRepository;
import com.ti5g.hotelbooking.persistence.repository.RoomRepository;
import com.ti5g.hotelbooking.service.availability.RoomAvailabilityService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BookingService {

	private static final int MAX_REFERENCE_ATTEMPTS = 20;

	private final BookingRepository bookingRepository;
	private final HotelRepository hotelRepository;
	private final RoomRepository roomRepository;
	private final RoomAvailabilityService roomAvailabilityService;
	private final BookingReferenceGenerator bookingReferenceGenerator;
	private final TransactionRetryExecutor transactionRetryExecutor;
	private final EntityManager entityManager;
	private final Clock clock;
	private final TransactionTemplate bookingTransaction;

	public BookingService(
			BookingRepository bookingRepository,
			HotelRepository hotelRepository,
			RoomRepository roomRepository,
			RoomAvailabilityService roomAvailabilityService,
			BookingReferenceGenerator bookingReferenceGenerator,
			TransactionRetryExecutor transactionRetryExecutor,
			EntityManager entityManager,
			Clock clock,
			PlatformTransactionManager transactionManager) {
		this.bookingRepository = bookingRepository;
		this.hotelRepository = hotelRepository;
		this.roomRepository = roomRepository;
		this.roomAvailabilityService = roomAvailabilityService;
		this.bookingReferenceGenerator = bookingReferenceGenerator;
		this.transactionRetryExecutor = transactionRetryExecutor;
		this.entityManager = entityManager;
		this.clock = clock;
		this.bookingTransaction = new TransactionTemplate(transactionManager);
		this.bookingTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		this.bookingTransaction.setPropagationBehavior(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public BookingDetails create(CreateBookingCommand command) {
		validate(command);

		return transactionRetryExecutor.execute(() -> Objects.requireNonNull(
				bookingTransaction.execute(status -> createInTransaction(command))));
	}

	@Transactional(readOnly = true)
	public BookingDetails getByReference(String bookingReference) {
		var normalizedReference = normalizeReference(bookingReference);

		return bookingRepository.findDetailsByBookingReference(normalizedReference)
				.map(BookingService::toDetails)
				.orElseThrow(() -> new BookingNotFoundException(normalizedReference));
	}

	private BookingDetails createInTransaction(CreateBookingCommand command) {
		entityManager.clear();

		var availableRooms = roomAvailabilityService.findAvailableRooms(
				command.hotelId(),
				command.checkInDate(),
				command.checkOutDate(),
				command.guestCount(),
				command.roomType());

		if (availableRooms.isEmpty()) {
			throw new NoRoomAvailableException();
		}

		var room = roomRepository.findById(availableRooms.getFirst().id())
				.orElseThrow(NoRoomAvailableException::new);
		var hotel = hotelRepository.getReferenceById(command.hotelId());
		var booking = new BookingEntity(
				UUID.randomUUID(),
				nextUniqueReference(),
				hotel,
				room,
				command.guestName().trim(),
				command.guestCount(),
				command.checkInDate(),
				command.checkOutDate(),
				Instant.now(clock));

		bookingRepository.saveAndFlush(booking);

		return toDetails(booking);
	}

	private String nextUniqueReference() {
		for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
			var reference = bookingReferenceGenerator.nextReference();

			if (!bookingRepository.existsByBookingReference(reference)) {
				return reference;
			}
		}

		throw new BookingConflictException(
				"Could not generate a unique booking reference. Please retry.");
	}

	private void validate(CreateBookingCommand command) {
		if (command == null) {
			throw new InvalidBookingRequestException("Booking request is required.");
		}
		if (command.hotelId() == null) {
			throw new InvalidBookingRequestException("Hotel ID is required.");
		}
		if (!BookingRules.hasValidGuestName(command.guestName())) {
			throw new InvalidBookingRequestException("Guest name is required.");
		}
		if (command.guestName().trim().length() > 200) {
			throw new InvalidBookingRequestException(
					"Guest name must contain at most 200 characters.");
		}
		if (command.checkInDate() == null || command.checkOutDate() == null) {
			throw new InvalidBookingRequestException(
					"Check-in and check-out dates are required.");
		}
		if (!BookingRules.hasNonPastCheckInDate(command.checkInDate(), clock)) {
			throw new InvalidBookingRequestException(
					"Check-in date cannot be in the past.");
		}
		if (!BookingRules.hasValidDateRange(
				command.checkInDate(),
				command.checkOutDate())) {
			throw new InvalidBookingRequestException(
					"Check-in date must be before check-out date.");
		}
		if (!BookingRules.hasValidGuestCount(command.guestCount())) {
			throw new InvalidBookingRequestException(
					"Guest count must be at least 1.");
		}
	}

	private static String normalizeReference(String bookingReference) {
		if (bookingReference == null || bookingReference.isBlank()) {
			throw new BookingNotFoundException("");
		}
		return bookingReference.trim();
	}

	private static BookingDetails toDetails(BookingEntity booking) {
		return new BookingDetails(
				booking.getBookingReference(),
				booking.getHotel().getId(),
				booking.getHotel().getName(),
				booking.getRoom().getId(),
				booking.getRoom().getRoomNumber(),
				booking.getRoom().getRoomType(),
				booking.getRoom().getCapacity(),
				booking.getGuestName(),
				booking.getGuestCount(),
				booking.getCheckInDate(),
				booking.getCheckOutDate(),
				booking.getCreatedAtUtc());
	}
}
