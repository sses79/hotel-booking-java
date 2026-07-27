package com.ti5g.hotelbooking.service.hotel;

import java.util.List;

import com.ti5g.hotelbooking.persistence.entity.HotelEntity;
import com.ti5g.hotelbooking.persistence.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelSearchService {

	private final HotelRepository hotelRepository;

	public HotelSearchService(HotelRepository hotelRepository) {
		this.hotelRepository = hotelRepository;
	}

	@Transactional(readOnly = true)
	public List<HotelSearchResult> search(String name) {
		List<HotelEntity> hotels;

		if (name == null || name.isBlank()) {
			hotels = hotelRepository.findAllByOrderByNameAsc();
		}
		else {
			hotels = hotelRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim());
		}

		return hotels.stream()
				.map(hotel -> new HotelSearchResult(hotel.getId(), hotel.getName()))
				.toList();
	}
}
