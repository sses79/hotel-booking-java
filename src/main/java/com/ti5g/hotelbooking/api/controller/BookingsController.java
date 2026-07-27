package com.ti5g.hotelbooking.api.controller;

import java.net.URI;

import com.ti5g.hotelbooking.api.dto.booking.BookingResponse;
import com.ti5g.hotelbooking.api.dto.booking.CreateBookingRequest;
import com.ti5g.hotelbooking.service.booking.BookingDetails;
import com.ti5g.hotelbooking.service.booking.BookingService;
import com.ti5g.hotelbooking.service.booking.CreateBookingCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/bookings")
public class BookingsController {

	private final BookingService bookingService;

	public BookingsController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	public ResponseEntity<BookingResponse> create(
			@Valid @RequestBody CreateBookingRequest request) {
		var booking = bookingService.create(new CreateBookingCommand(
				request.hotelId(),
				request.guestName(),
				request.guestCount(),
				request.checkInDate(),
				request.checkOutDate(),
				request.roomType()));
		var response = toResponse(booking);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{reference}")
				.buildAndExpand(response.bookingReference())
				.toUri();

		return ResponseEntity.created(location).body(response);
	}

	@GetMapping("/{reference}")
	public BookingResponse getByReference(@PathVariable String reference) {
		return toResponse(bookingService.getByReference(reference));
	}

	private static BookingResponse toResponse(BookingDetails booking) {
		return new BookingResponse(
				booking.bookingReference(),
				booking.hotelId(),
				booking.hotelName(),
				booking.roomId(),
				booking.roomNumber(),
				booking.roomType(),
				booking.roomCapacity(),
				booking.guestName(),
				booking.guestCount(),
				booking.checkInDate(),
				booking.checkOutDate(),
				booking.createdAtUtc());
	}
}
