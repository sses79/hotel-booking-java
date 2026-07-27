package com.ti5g.hotelbooking.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import com.ti5g.hotelbooking.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

	boolean existsByBookingReference(String bookingReference);

	@Query("""
			SELECT booking
			FROM BookingEntity booking
			JOIN FETCH booking.hotel
			JOIN FETCH booking.room
			WHERE booking.bookingReference = :bookingReference
			""")
	Optional<BookingEntity> findDetailsByBookingReference(
			@Param("bookingReference") String bookingReference);
}
