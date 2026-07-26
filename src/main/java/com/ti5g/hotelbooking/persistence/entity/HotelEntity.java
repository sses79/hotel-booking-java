package com.ti5g.hotelbooking.persistence.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "hotels")
public class HotelEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("roomNumber ASC")
	private List<RoomEntity> rooms = new ArrayList<>();

	protected HotelEntity() {
	}

	public HotelEntity(UUID id, String name) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.name = Objects.requireNonNull(name, "name must not be null");
	}

	public void addRoom(RoomEntity room) {
		Objects.requireNonNull(room, "room must not be null");
		room.assignTo(this);
		rooms.add(room);
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<RoomEntity> getRooms() {
		return Collections.unmodifiableList(rooms);
	}
}
