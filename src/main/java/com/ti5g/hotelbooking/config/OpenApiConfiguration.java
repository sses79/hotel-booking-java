package com.ti5g.hotelbooking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Hotel Booking API",
				version = "1.0",
				description = """
						Search hotels, check room availability, and create bookings.
						Seed the deterministic Grand Plaza Hotel before following the
						manual booking flow.
						"""),
		tags = {
			@Tag(name = "Hotels", description = "Hotel search and room availability"),
			@Tag(name = "Bookings", description = "Booking creation and lookup"),
			@Tag(name = "Test data", description = "Deterministic reviewer data")
		})
public class OpenApiConfiguration {
}
