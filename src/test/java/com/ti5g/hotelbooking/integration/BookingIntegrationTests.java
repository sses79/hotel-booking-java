package com.ti5g.hotelbooking.integration;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import com.ti5g.hotelbooking.TestcontainersConfiguration;
import com.ti5g.hotelbooking.persistence.repository.BookingRepository;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class BookingIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TestDataService testDataService;

	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private AvailabilityBarrier availabilityBarrier;

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
	void createsBookingAndFindsItByReference() throws Exception {
		testDataService.seed();
		var checkIn = futureDate();

		var result = mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingJson(
								SeedData.GRAND_PLAZA_HOTEL_ID,
								"  Ada Lovelace  ",
								2,
								checkIn,
								checkIn.plusDays(2),
								"double")))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(header().string(
						"Location",
						org.hamcrest.Matchers.matchesPattern(
								".*/api/bookings/HB-\\d{6}")))
				.andExpect(jsonPath("$.bookingReference").value(
						org.hamcrest.Matchers.matchesPattern("HB-\\d{6}")))
				.andExpect(jsonPath("$.hotelId")
						.value(SeedData.GRAND_PLAZA_HOTEL_ID.toString()))
				.andExpect(jsonPath("$.hotelName").value(SeedData.GRAND_PLAZA_HOTEL_NAME))
				.andExpect(jsonPath("$.roomNumber").value("201"))
				.andExpect(jsonPath("$.roomType").value("DOUBLE"))
				.andExpect(jsonPath("$.guestName").value("Ada Lovelace"))
				.andReturn();

		String reference = JsonPath.read(
				new String(
						result.getResponse().getContentAsByteArray(),
						StandardCharsets.UTF_8),
				"$.bookingReference");

		mockMvc.perform(get("/api/bookings/{reference}", reference))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bookingReference").value(reference))
				.andExpect(jsonPath("$.roomNumber").value("201"))
				.andExpect(jsonPath("$.guestName").value("Ada Lovelace"));
	}

	@Test
	void acceptsCheckInTodayAndAllowsBackToBackBookings() throws Exception {
		testDataService.seed();
		var today = LocalDate.now(clock);

		var firstBooking = createBooking(
				"First Guest", 1, today, today.plusDays(2), "SINGLE");

		assertThat(firstBooking.getResponse().getStatus()).isEqualTo(201);
		assertThat(roomNumber(firstBooking)).isEqualTo("101");

		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingJson(
								SeedData.GRAND_PLAZA_HOTEL_ID,
								"Second Guest",
								1,
								today.plusDays(2),
								today.plusDays(4),
								"SINGLE")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.roomNumber").value("101"));
	}

	@Test
	void rejectsInvalidBookingRequestWithProblemDetail() throws Exception {
		var today = LocalDate.now(clock);

		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingJson(
								SeedData.GRAND_PLAZA_HOTEL_ID,
								"Ada Lovelace",
								2,
								today.minusDays(1),
								today.plusDays(1),
								"DOUBLE")))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid booking request"))
				.andExpect(jsonPath("$.detail")
						.value("Check-in date cannot be in the past."));

		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingJson(
								SeedData.GRAND_PLAZA_HOTEL_ID,
								" ",
								0,
								today,
								today.plusDays(1),
								"DOUBLE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request body"));
	}

	@Test
	void returnsNotFoundForMissingHotelAndBooking() throws Exception {
		var checkIn = futureDate();
		var missingHotelId = UUID.fromString(
				"00000000-0000-0000-0000-000000000099");

		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingJson(
								missingHotelId,
								"Ada Lovelace",
								2,
								checkIn,
								checkIn.plusDays(2),
								"DOUBLE")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Hotel not found"));

		mockMvc.perform(get("/api/bookings/HB-999999"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Booking not found"));
	}

	@Test
	void returnsConflictWhenNoRequestedRoomIsAvailable() throws Exception {
		testDataService.seed();
		var checkIn = futureDate();
		var checkOut = checkIn.plusDays(2);

		assertThat(createBooking(
				"First Guest", 2, checkIn, checkOut, "DOUBLE")
				.getResponse().getStatus()).isEqualTo(201);
		assertThat(createBooking(
				"Second Guest", 2, checkIn, checkOut, "DOUBLE")
				.getResponse().getStatus()).isEqualTo(201);

		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingJson(
								SeedData.GRAND_PLAZA_HOTEL_ID,
								"Third Guest",
								2,
								checkIn,
								checkOut,
								"DOUBLE")))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Booking conflict"));
	}

	@Test
	void concurrentRequestsCannotDoubleBookTheLastMatchingRoom() throws Exception {
		testDataService.seed();
		var checkIn = futureDate();
		var checkOut = checkIn.plusDays(2);
		assertThat(createBooking(
				"Existing Guest", 2, checkIn, checkOut, "DOUBLE")
				.getResponse().getStatus()).isEqualTo(201);
		availabilityBarrier.arm();

		var results = createConcurrently(
				() -> createBooking(
						"Concurrent One", 2, checkIn, checkOut, "DOUBLE"),
				() -> createBooking(
						"Concurrent Two", 2, checkIn, checkOut, "DOUBLE"));

		assertThat(results)
				.extracting(result -> result.getResponse().getStatus())
				.containsExactlyInAnyOrder(201, 409);
		assertThat(bookingRepository.count()).isEqualTo(2);

		var overlappingPairCount = jdbcClient.sql("""
						SELECT COUNT(*)
						FROM bookings first_booking
						JOIN bookings second_booking
						  ON first_booking.room_id = second_booking.room_id
						 AND first_booking.id <> second_booking.id
						 AND first_booking.check_in_date < second_booking.check_out_date
						 AND second_booking.check_in_date < first_booking.check_out_date
						""")
				.query(Integer.class)
				.single();

		assertThat(overlappingPairCount).isZero();
	}

	@Test
	void concurrentBackToBackRequestsCanUseTheSameRoom() throws Exception {
		testDataService.seed();
		var firstCheckIn = futureDate();
		var boundary = firstCheckIn.plusDays(2);
		availabilityBarrier.arm();

		var results = createConcurrently(
				() -> createBooking(
						"Early Guest", 1, firstCheckIn, boundary, "SINGLE"),
				() -> createBooking(
						"Later Guest", 1, boundary, boundary.plusDays(2), "SINGLE"));

		assertThat(results)
				.extracting(result -> result.getResponse().getStatus())
				.containsOnly(201);
		assertThat(results)
				.extracting(BookingIntegrationTests::roomNumber)
				.containsOnly("101");
		assertThat(bookingRepository.count()).isEqualTo(2);
	}

	private MvcResult createBooking(
			String guestName,
			int guestCount,
			LocalDate checkIn,
			LocalDate checkOut,
			String roomType) throws Exception {
		return mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingJson(
								SeedData.GRAND_PLAZA_HOTEL_ID,
								guestName,
								guestCount,
								checkIn,
								checkOut,
								roomType)))
				.andReturn();
	}

	private List<MvcResult> createConcurrently(
			ThrowingRequest first,
			ThrowingRequest second) throws Exception {
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var firstResult = executor.submit(first::execute);
			var secondResult = executor.submit(second::execute);

			return List.of(
					firstResult.get(15, TimeUnit.SECONDS),
					secondResult.get(15, TimeUnit.SECONDS));
		}
	}

	private LocalDate futureDate() {
		return LocalDate.now(clock).plusDays(30);
	}

	private static String roomNumber(MvcResult result) {
		return JsonPath.read(
				new String(
						result.getResponse().getContentAsByteArray(),
						StandardCharsets.UTF_8),
				"$.roomNumber");
	}

	private static String bookingJson(
			UUID hotelId,
			String guestName,
			int guestCount,
			LocalDate checkIn,
			LocalDate checkOut,
			String roomType) {
		return """
				{
				  "hotelId": "%s",
				  "guestName": "%s",
				  "guestCount": %d,
				  "checkInDate": "%s",
				  "checkOutDate": "%s",
				  "roomType": "%s"
				}
				""".formatted(
				hotelId,
				guestName,
				guestCount,
				checkIn,
				checkOut,
				roomType);
	}

	@FunctionalInterface
	private interface ThrowingRequest {

		MvcResult execute() throws Exception;
	}
}
