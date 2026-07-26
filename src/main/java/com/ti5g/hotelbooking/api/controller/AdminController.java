package com.ti5g.hotelbooking.api.controller;

import com.ti5g.hotelbooking.api.dto.admin.SeedResponse;
import com.ti5g.hotelbooking.service.testdata.TestDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final TestDataService testDataService;

	public AdminController(TestDataService testDataService) {
		this.testDataService = testDataService;
	}

	@PostMapping("/seed")
	public SeedResponse seed() {
		var result = testDataService.seed();

		return new SeedResponse(
				result.hotelId(),
				result.hotelName(),
				result.roomsCreated());
	}

	@PostMapping("/reset")
	public ResponseEntity<Void> reset() {
		testDataService.reset();
		return ResponseEntity.noContent().build();
	}
}
