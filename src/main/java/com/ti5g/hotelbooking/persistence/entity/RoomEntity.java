package com.ti5g.hotelbooking.persistence.entity;

import java.util.Objects;
import java.util.UUID;

import com.ti5g.hotelbooking.domain.RoomType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "rooms",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_rooms_hotel_room_number",
				columnNames = {"hotel_id", "room_number"}),
		indexes = @Index(
				name = "ix_rooms_hotel_type_capacity",
				columnList = "hotel_id, room_type, capacity"))
public class RoomEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "hotel_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_rooms_hotels"))
	private HotelEntity hotel;

	@Column(name = "room_number", nullable = false, length = 20)
	private String roomNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "room_type", nullable = false, length = 20)
	private RoomType roomType;

	@Column(name = "capacity", nullable = false)
	private int capacity;

	protected RoomEntity() {
	}

	public RoomEntity(UUID id, String roomNumber, RoomType roomType, int capacity) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.roomNumber = Objects.requireNonNull(roomNumber, "roomNumber must not be null");
		this.roomType = Objects.requireNonNull(roomType, "roomType must not be null");
		this.capacity = capacity;
	}

	void assignTo(HotelEntity hotel) {
		this.hotel = Objects.requireNonNull(hotel, "hotel must not be null");
	}

	public UUID getId() {
		return id;
	}

	public HotelEntity getHotel() {
		return hotel;
	}

	public String getRoomNumber() {
		return roomNumber;
	}

	public RoomType getRoomType() {
		return roomType;
	}

	public int getCapacity() {
		return capacity;
	}
}
