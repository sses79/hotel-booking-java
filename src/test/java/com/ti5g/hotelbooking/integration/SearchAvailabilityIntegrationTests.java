package com.ti5g.hotelbooking.integration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ti5g.hotelbooking.TestcontainersConfiguration;
import com.ti5g.hotelbooking.persistence.entity.BookingEntity;
import com.ti5g.hotelbooking.persistence.entity.HotelEntity;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class SearchAvailabilityIntegrationTests {

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

	@Autowired
	private Clock clock;

	@BeforeEach
	void resetBeforeTest() {
		testDataService.reset();
	}

	@AfterEach
	void resetAfterTest() {
		testDataService.reset();
	}

	@Test
	void hotelSearchTrimsTextIgnoresCaseAndOrdersByName() throws Exception {
		testDataService.seed();
		hotelRepository.saveAndFlush(new HotelEntity(
				UUID.fromString("00000000-0000-0000-0000-000000000002"),
				"Grand Airport Lodge"));

		mockMvc.perform(get("/api/hotels").param("name", " gRaNd "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Grand Airport Lodge"))
				.andExpect(jsonPath("$[1].id").value(SeedData.GRAND_PLAZA_HOTEL_ID.toString()))
				.andExpect(jsonPath("$[1].name").value(SeedData.GRAND_PLAZA_HOTEL_NAME))
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void emptyHotelSearchReturnsEveryHotelInNameOrder() throws Exception {
		testDataService.seed();
		hotelRepository.saveAndFlush(new HotelEntity(
				UUID.fromString("00000000-0000-0000-0000-000000000002"),
				"Airport Lodge"));

		mockMvc.perform(get("/api/hotels"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Airport Lodge"))
				.andExpect(jsonPath("$[1].name").value(SeedData.GRAND_PLAZA_HOTEL_NAME));
	}

	@Test
	void availabilityFiltersCapacityAndUsesDeterministicOrdering() throws Exception {
		testDataService.seed();
		var checkIn = futureDate();

		mockMvc.perform(get(availabilityUrl())
						.param("checkIn", checkIn.toString())
						.param("checkOut", checkIn.plusDays(2).toString())
						.param("guests", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].roomNumber").value("201"))
				.andExpect(jsonPath("$[1].roomNumber").value("202"))
				.andExpect(jsonPath("$[2].roomNumber").value("301"))
				.andExpect(jsonPath("$[3].roomNumber").value("302"))
				.andExpect(jsonPath("$.length()").value(4));
	}

	@Test
	void availabilityAcceptsCaseInsensitiveRoomType() throws Exception {
		testDataService.seed();
		var checkIn = futureDate();

		mockMvc.perform(get(availabilityUrl())
						.param("checkIn", checkIn.toString())
						.param("checkOut", checkIn.plusDays(2).toString())
						.param("guests", "1")
						.param("roomType", "double"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].roomNumber").value("201"))
				.andExpect(jsonPath("$[1].roomNumber").value("202"))
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void availabilityExcludesOverlapsButAllowsBackToBackStay() throws Exception {
		testDataService.seed();
		var bookedCheckIn = futureDate();
		var bookedCheckOut = bookedCheckIn.plusDays(2);
		var hotel = hotelRepository.findById(SeedData.GRAND_PLAZA_HOTEL_ID).orElseThrow();
		var room = roomRepository.findById(SeedData.ROOMS.get(2).id()).orElseThrow();

		bookingRepository.saveAndFlush(new BookingEntity(
				UUID.randomUUID(),
				"HB-PHASE3-001",
				hotel,
				room,
				"Ada Lovelace",
				2,
				bookedCheckIn,
				bookedCheckOut,
				Instant.now(clock)));

		mockMvc.perform(get(availabilityUrl())
						.param("checkIn", bookedCheckIn.plusDays(1).toString())
						.param("checkOut", bookedCheckOut.plusDays(1).toString())
						.param("guests", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].roomNumber").value("202"))
				.andExpect(jsonPath("$.length()").value(3));

		mockMvc.perform(get(availabilityUrl())
						.param("checkIn", bookedCheckOut.toString())
						.param("checkOut", bookedCheckOut.plusDays(1).toString())
						.param("guests", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].roomNumber").value("201"))
				.andExpect(jsonPath("$.length()").value(4));
	}

	@Test
	void invalidAvailabilityRulesReturnProblemDetails() throws Exception {
		var today = LocalDate.now(clock);

		mockMvc.perform(get(availabilityUrl())
						.param("checkIn", today.minusDays(1).toString())
						.param("checkOut", today.plusDays(1).toString())
						.param("guests", "2"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid availability request"))
				.andExpect(jsonPath("$.detail").value("Check-in date cannot be in the past."));

		mockMvc.perform(get(availabilityUrl())
						.param("checkIn", today.toString())
						.param("checkOut", today.toString())
						.param("guests", "2"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail")
						.value("Check-in date must be before check-out date."));

		mockMvc.perform(get(availabilityUrl())
						.param("checkIn", today.toString())
						.param("checkOut", today.plusDays(1).toString())
						.param("guests", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Guest count must be at least 1."));
	}

	@Test
	void invalidRoomTypeReturnsProblemDetail() throws Exception {
		var checkIn = futureDate();

		mockMvc.perform(get(availabilityUrl())
						.param("checkIn", checkIn.toString())
						.param("checkOut", checkIn.plusDays(1).toString())
						.param("guests", "1")
						.param("roomType", "penthouse"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid request parameter"))
				.andExpect(jsonPath("$.detail").value("Invalid value for parameter 'roomType'."));
	}

	@Test
	void missingHotelReturnsNotFoundProblemDetail() throws Exception {
		var checkIn = futureDate();
		var missingHotelId = UUID.fromString("00000000-0000-0000-0000-000000000099");

		mockMvc.perform(get("/api/hotels/{hotelId}/rooms/available", missingHotelId)
						.param("checkIn", checkIn.toString())
						.param("checkOut", checkIn.plusDays(1).toString())
						.param("guests", "1"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Hotel not found"))
				.andExpect(jsonPath("$.detail").value(
						"Hotel '%s' was not found.".formatted(missingHotelId)));
	}

	private LocalDate futureDate() {
		return LocalDate.now(clock).plusDays(30);
	}

	private static String availabilityUrl() {
		return "/api/hotels/%s/rooms/available".formatted(SeedData.GRAND_PLAZA_HOTEL_ID);
	}
}
