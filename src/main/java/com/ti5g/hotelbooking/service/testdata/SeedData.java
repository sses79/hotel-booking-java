package com.ti5g.hotelbooking.service.testdata;

import java.util.List;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;
import com.ti5g.hotelbooking.persistence.entity.HotelEntity;
import com.ti5g.hotelbooking.persistence.entity.RoomEntity;

public final class SeedData {

	public static final UUID GRAND_PLAZA_HOTEL_ID =
			UUID.fromString("00000000-0000-0000-0000-000000000001");
	public static final String GRAND_PLAZA_HOTEL_NAME = "Grand Plaza Hotel";

	public static final List<SeedRoomDefinition> ROOMS = List.of(
			room("00000000-0000-0000-0000-000000000101", "101", RoomType.SINGLE, 1),
			room("00000000-0000-0000-0000-000000000102", "102", RoomType.SINGLE, 1),
			room("00000000-0000-0000-0000-000000000201", "201", RoomType.DOUBLE, 2),
			room("00000000-0000-0000-0000-000000000202", "202", RoomType.DOUBLE, 2),
			room("00000000-0000-0000-0000-000000000301", "301", RoomType.DELUXE, 4),
			room("00000000-0000-0000-0000-000000000302", "302", RoomType.DELUXE, 4));

	private SeedData() {
	}

	public static HotelEntity createGrandPlazaHotel() {
		var hotel = new HotelEntity(GRAND_PLAZA_HOTEL_ID, GRAND_PLAZA_HOTEL_NAME);

		ROOMS.forEach(room -> hotel.addRoom(new RoomEntity(
				room.id(),
				room.roomNumber(),
				room.roomType(),
				room.capacity())));

		return hotel;
	}

	private static SeedRoomDefinition room(
			String id,
			String roomNumber,
			RoomType roomType,
			int capacity) {
		return new SeedRoomDefinition(UUID.fromString(id), roomNumber, roomType, capacity);
	}
}
