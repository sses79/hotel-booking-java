package com.ti5g.hotelbooking.api.controller;

import java.net.URI;

import com.ti5g.hotelbooking.api.dto.booking.BookingResponse;
import com.ti5g.hotelbooking.api.dto.booking.CreateBookingRequest;
import com.ti5g.hotelbooking.service.booking.BookingDetails;
import com.ti5g.hotelbooking.service.booking.BookingService;
import com.ti5g.hotelbooking.service.booking.CreateBookingCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Bookings")
public class BookingsController {

	private final BookingService bookingService;

	public BookingsController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	@Operation(
		summary = "Create a booking",
		description = """
				Selects the first suitable room inside a serializable transaction.
				Returns 409 when no matching room remains.
				""")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Booking created"),
		@ApiResponse(responseCode = "400", description = "Invalid booking request"),
		@ApiResponse(responseCode = "404", description = "Hotel not found"),
		@ApiResponse(responseCode = "409", description = "No matching room available")
	})
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
	@Operation(
		summary = "Find a booking",
		description = "Looks up a booking by its HB-###### reference.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Booking details"),
			@ApiResponse(responseCode = "404", description = "Booking not found")
		})
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
