package com.ti5g.hotelbooking.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;
import com.ti5g.hotelbooking.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

	List<RoomEntity> findAllByOrderByRoomNumberAsc();

	@Query("""
			SELECT room
			FROM RoomEntity room
			WHERE room.hotel.id = :hotelId
			  AND room.capacity >= :guests
			  AND (:roomType IS NULL OR room.roomType = :roomType)
			  AND NOT EXISTS (
			      SELECT booking.id
			      FROM BookingEntity booking
			      WHERE booking.room.id = room.id
			        AND booking.checkInDate < :checkOutDate
			        AND :checkInDate < booking.checkOutDate
			  )
			""")
	List<RoomEntity> findAvailableRooms(
			@Param("hotelId") UUID hotelId,
			@Param("checkInDate") LocalDate checkInDate,
			@Param("checkOutDate") LocalDate checkOutDate,
			@Param("guests") int guests,
			@Param("roomType") RoomType roomType);
}
