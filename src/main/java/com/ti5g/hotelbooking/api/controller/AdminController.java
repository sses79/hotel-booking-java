package com.ti5g.hotelbooking.api.controller;

import com.ti5g.hotelbooking.api.dto.admin.SeedResponse;
import com.ti5g.hotelbooking.service.testdata.TestDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Test data")
public class AdminController {

	private final TestDataService testDataService;

	public AdminController(TestDataService testDataService) {
		this.testDataService = testDataService;
	}

	@PostMapping("/seed")
	@Operation(
		summary = "Seed deterministic test data",
		description = "Resets all data, then creates Grand Plaza Hotel and six rooms.",
		responses = @ApiResponse(responseCode = "200", description = "Test data seeded"))
	public SeedResponse seed() {
		var result = testDataService.seed();

		return new SeedResponse(
				result.hotelId(),
				result.hotelName(),
				result.roomsCreated());
	}

	@PostMapping("/reset")
	@Operation(
		summary = "Reset application data",
		description = "Deletes bookings, rooms, and hotels in dependency order.",
		responses = @ApiResponse(responseCode = "204", description = "Data reset"))
	public ResponseEntity<Void> reset() {
		testDataService.reset();
		return ResponseEntity.noContent().build();
	}
}
