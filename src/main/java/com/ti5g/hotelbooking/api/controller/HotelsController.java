package com.ti5g.hotelbooking.api.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.ti5g.hotelbooking.api.dto.hotel.HotelResponse;
import com.ti5g.hotelbooking.api.dto.room.AvailableRoomResponse;
import com.ti5g.hotelbooking.domain.RoomType;
import com.ti5g.hotelbooking.service.availability.RoomAvailabilityService;
import com.ti5g.hotelbooking.service.hotel.HotelSearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels")
public class HotelsController {

	private final HotelSearchService hotelSearchService;
	private final RoomAvailabilityService roomAvailabilityService;

	public HotelsController(
			HotelSearchService hotelSearchService,
			RoomAvailabilityService roomAvailabilityService) {
		this.hotelSearchService = hotelSearchService;
		this.roomAvailabilityService = roomAvailabilityService;
	}

	@GetMapping
	public List<HotelResponse> search(
			@RequestParam(required = false) String name) {
		return hotelSearchService.search(name).stream()
				.map(hotel -> new HotelResponse(hotel.id(), hotel.name()))
				.toList();
	}

	@GetMapping("/{hotelId}/rooms/available")
	public List<AvailableRoomResponse> findAvailableRooms(
			@PathVariable UUID hotelId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
			@RequestParam int guests,
			@RequestParam(required = false) RoomType roomType) {
		return roomAvailabilityService.findAvailableRooms(
						hotelId,
						checkIn,
						checkOut,
						guests,
						roomType)
				.stream()
				.map(room -> new AvailableRoomResponse(
						room.id(),
						room.hotelId(),
						room.roomNumber(),
						room.roomType(),
						room.capacity()))
				.toList();
	}
}
