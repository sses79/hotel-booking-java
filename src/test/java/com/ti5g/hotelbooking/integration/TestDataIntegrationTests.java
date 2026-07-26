package com.ti5g.hotelbooking.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ti5g.hotelbooking.TestcontainersConfiguration;
import com.ti5g.hotelbooking.domain.RoomType;
import com.ti5g.hotelbooking.persistence.entity.BookingEntity;
import com.ti5g.hotelbooking.persistence.repository.BookingRepository;
import com.ti5g.hotelbooking.persistence.repository.HotelRepository;
import com.ti5g.hotelbooking.persistence.repository.RoomRepository;
import com.ti5g.hotelbooking.service.testdata.SeedData;
import com.ti5g.hotelbooking.service.testdata.TestDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class TestDataIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TestDataService testDataService;

	@Autowired
	private HotelRepository hotelRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private BookingRepository bookingRepository;

	@BeforeEach
	void resetBeforeTest() {
		testDataService.reset();
	}

	@AfterEach
	void resetAfterTest() {
		testDataService.reset();
	}

	@Test
	void seedEndpointCreatesExpectedHotelAndSixRooms() throws Exception {
		mockMvc.perform(post("/api/admin/seed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hotelId").value(SeedData.GRAND_PLAZA_HOTEL_ID.toString()))
				.andExpect(jsonPath("$.hotelName").value(SeedData.GRAND_PLAZA_HOTEL_NAME))
				.andExpect(jsonPath("$.roomsCreated").value(6));

		assertThat(hotelRepository.count()).isEqualTo(1);
		assertThat(roomRepository.findAllByOrderByRoomNumberAsc())
				.extracting(
						room -> room.getId(),
						room -> room.getRoomNumber(),
						room -> room.getRoomType(),
						room -> room.getCapacity())
				.containsExactly(
						row(101, "101", RoomType.SINGLE, 1),
						row(102, "102", RoomType.SINGLE, 1),
						row(201, "201", RoomType.DOUBLE, 2),
						row(202, "202", RoomType.DOUBLE, 2),
						row(301, "301", RoomType.DELUXE, 4),
						row(302, "302", RoomType.DELUXE, 4));
	}

	@Test
	void seedIsRepeatableAndRemovesExistingBookings() {
		testDataService.seed();
		var hotel = hotelRepository.findById(SeedData.GRAND_PLAZA_HOTEL_ID).orElseThrow();
		var room = roomRepository.findById(SeedData.ROOMS.getFirst().id()).orElseThrow();

		bookingRepository.saveAndFlush(new BookingEntity(
				UUID.randomUUID(),
				"HB-TEST-001",
				hotel,
				room,
				"Ada Lovelace",
				1,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 3),
				Instant.parse("2026-07-09T12:00:00Z")));

		var result = testDataService.seed();

		assertThat(result.hotelId()).isEqualTo(SeedData.GRAND_PLAZA_HOTEL_ID);
		assertThat(result.roomsCreated()).isEqualTo(6);
		assertThat(hotelRepository.count()).isEqualTo(1);
		assertThat(roomRepository.count()).isEqualTo(6);
		assertThat(bookingRepository.count()).isZero();
	}

	@Test
	void resetEndpointRemovesAllData() throws Exception {
		testDataService.seed();

		mockMvc.perform(post("/api/admin/reset"))
				.andExpect(status().isNoContent());

		assertThat(bookingRepository.count()).isZero();
		assertThat(roomRepository.count()).isZero();
		assertThat(hotelRepository.count()).isZero();
	}

	private static org.assertj.core.groups.Tuple row(
			int idSuffix,
			String roomNumber,
			RoomType roomType,
			int capacity) {
		return org.assertj.core.groups.Tuple.tuple(
				UUID.fromString("00000000-0000-0000-0000-%012d".formatted(idSuffix)),
				roomNumber,
				roomType,
				capacity);
	}
}
