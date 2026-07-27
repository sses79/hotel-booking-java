package com.ti5g.hotelbooking.persistence.repository;

import java.util.List;
import java.util.UUID;

import com.ti5g.hotelbooking.persistence.entity.HotelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<HotelEntity, UUID> {

	List<HotelEntity> findAllByOrderByNameAsc();

	List<HotelEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
