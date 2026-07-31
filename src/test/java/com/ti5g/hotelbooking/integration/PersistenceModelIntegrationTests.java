package com.ti5g.hotelbooking.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ti5g.hotelbooking.TestcontainersConfiguration;
import com.ti5g.hotelbooking.domain.RoomType;
import com.ti5g.hotelbooking.persistence.entity.BookingEntity;
import com.ti5g.hotelbooking.persistence.entity.HotelEntity;
import com.ti5g.hotelbooking.persistence.entity.RoomEntity;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class PersistenceModelIntegrationTests {

	@Autowired
	private TestDataService testDataService;

	@Autowired
	private HotelRepository hotelRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private JdbcClient jdbcClient;

	@BeforeEach
	void resetBeforeTest() {
		testDataService.reset();
	}

	@AfterEach
	void resetAfterTest() {
		testDataService.reset();
	}

	@Test
	void roomTypeIsPersistedAsReadableText() {
		testDataService.seed();

		var storedRoomType = jdbcClient.sql("""
						SELECT room_type
						FROM rooms
						WHERE id = :roomId
						""")
				.param("roomId", SeedData.ROOMS.get(2).id())
				.query(String.class)
				.single();

		assertThat(storedRoomType).isEqualTo("DOUBLE");
	}

	@Test
	void roomTypeDatabaseConstraintRejectsUnsupportedText() {
		testDataService.seed();

		assertThatThrownBy(() -> jdbcClient.sql("""
						INSERT INTO rooms (
						    id,
						    hotel_id,
						    room_number,
						    room_type,
						    capacity
						)
						VALUES (
						    :id,
						    :hotelId,
						    :roomNumber,
						    :roomType,
						    :capacity
						)
						""")
				.param("id", UUID.randomUUID())
				.param("hotelId", SeedData.GRAND_PLAZA_HOTEL_ID)
				.param("roomNumber", "INVALID-TYPE")
				.param("roomType", "PENTHOUSE")
				.param("capacity", 2)
				.update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void roomRequiresAnExistingHotelRelationship() {
		assertThatThrownBy(() -> jdbcClient.sql("""
						INSERT INTO rooms (
						    id,
						    hotel_id,
						    room_number,
						    room_type,
						    capacity
						)
						VALUES (
						    :id,
						    :hotelId,
						    :roomNumber,
						    :roomType,
						    :capacity
						)
						""")
				.param("id", UUID.randomUUID())
				.param("hotelId", UUID.randomUUID())
				.param("roomNumber", "ORPHAN")
				.param("roomType", "SINGLE")
				.param("capacity", 1)
				.update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void duplicateRoomNumberWithinHotelIsRejected() {
		var hotel = SeedData.createGrandPlazaHotel();
		hotel.addRoom(new RoomEntity(
				UUID.randomUUID(),
				"101",
				RoomType.SINGLE,
				1));

		assertThatThrownBy(() -> hotelRepository.saveAndFlush(hotel))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void roomCapacityDatabaseConstraintRejectsZero() {
		var hotel = new HotelEntity(UUID.randomUUID(), "Constraint Test Hotel");
		hotel.addRoom(new RoomEntity(
				UUID.randomUUID(),
				"001",
				RoomType.SINGLE,
				0));

		assertThatThrownBy(() -> hotelRepository.saveAndFlush(hotel))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void duplicateBookingReferenceIsRejected() {
		testDataService.seed();
		var hotel = hotelRepository.findById(SeedData.GRAND_PLAZA_HOTEL_ID).orElseThrow();
		var rooms = roomRepository.findAllByOrderByRoomNumberAsc();
		bookingRepository.saveAndFlush(booking("HB-DUPLICATE", hotel, rooms.get(0), 1));

		assertThatThrownBy(() -> bookingRepository.saveAndFlush(
				booking("HB-DUPLICATE", hotel, rooms.get(1), 1)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bookingGuestCountDatabaseConstraintRejectsZero() {
		testDataService.seed();
		var hotel = hotelRepository.findById(SeedData.GRAND_PLAZA_HOTEL_ID).orElseThrow();
		var room = roomRepository.findAllByOrderByRoomNumberAsc().getFirst();

		assertThatThrownBy(() -> bookingRepository.saveAndFlush(
				booking("HB-ZERO-GUESTS", hotel, room, 0)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bookingDateRangeDatabaseConstraintRejectsSameDayCheckout() {
		testDataService.seed();
		var hotel = hotelRepository.findById(SeedData.GRAND_PLAZA_HOTEL_ID).orElseThrow();
		var room = roomRepository.findAllByOrderByRoomNumberAsc().getFirst();
		var booking = new BookingEntity(
				UUID.randomUUID(),
				"HB-BAD-DATES",
				hotel,
				room,
				"Ada Lovelace",
				1,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 1),
				Instant.parse("2026-07-09T12:00:00Z"));

		assertThatThrownBy(() -> bookingRepository.saveAndFlush(booking))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void roomReferencedByBookingCannotBeDeleted() {
		testDataService.seed();
		var hotel = hotelRepository.findById(SeedData.GRAND_PLAZA_HOTEL_ID).orElseThrow();
		var room = roomRepository.findAllByOrderByRoomNumberAsc().getFirst();
		bookingRepository.saveAndFlush(booking("HB-DELETE-GUARD", hotel, room, 1));

		assertThatThrownBy(() -> roomRepository.deleteAllInBatch())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static BookingEntity booking(
			String reference,
			HotelEntity hotel,
			RoomEntity room,
			int guestCount) {
		return new BookingEntity(
				UUID.randomUUID(),
				reference,
				hotel,
				room,
				"Ada Lovelace",
				guestCount,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 3),
				Instant.parse("2026-07-09T12:00:00Z"));
	}
}
