package com.ti5g.hotelbooking.service.testdata;

import com.ti5g.hotelbooking.persistence.repository.BookingRepository;
import com.ti5g.hotelbooking.persistence.repository.HotelRepository;
import com.ti5g.hotelbooking.persistence.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestDataService {

	private final BookingRepository bookingRepository;
	private final RoomRepository roomRepository;
	private final HotelRepository hotelRepository;
	private final EntityManager entityManager;

	public TestDataService(
			BookingRepository bookingRepository,
			RoomRepository roomRepository,
			HotelRepository hotelRepository,
			EntityManager entityManager) {
		this.bookingRepository = bookingRepository;
		this.roomRepository = roomRepository;
		this.hotelRepository = hotelRepository;
		this.entityManager = entityManager;
	}

	@Transactional
	public SeedResult seed() {
		resetData();

		var hotel = SeedData.createGrandPlazaHotel();
		entityManager.persist(hotel);
		entityManager.flush();

		return new SeedResult(hotel.getId(), hotel.getName(), hotel.getRooms().size());
	}

	@Transactional
	public void reset() {
		resetData();
	}

	private void resetData() {
		bookingRepository.deleteAllInBatch();
		roomRepository.deleteAllInBatch();
		hotelRepository.deleteAllInBatch();
		entityManager.clear();
	}
}
