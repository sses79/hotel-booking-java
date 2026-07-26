package com.ti5g.hotelbooking.persistence.repository;

import java.util.UUID;

import com.ti5g.hotelbooking.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {
}
