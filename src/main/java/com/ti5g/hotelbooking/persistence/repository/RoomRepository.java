package com.ti5g.hotelbooking.persistence.repository;

import java.util.List;
import java.util.UUID;

import com.ti5g.hotelbooking.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

	List<RoomEntity> findAllByOrderByRoomNumberAsc();
}
