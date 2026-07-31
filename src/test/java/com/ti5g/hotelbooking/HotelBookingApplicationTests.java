package com.ti5g.hotelbooking;

import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class HotelBookingApplicationTests {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private Clock clock;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
		assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
	}

	@Test
	void flywayCreatesTheInitialSchema() {
		var tableCount = jdbcClient.sql("""
						SELECT COUNT(*)
						FROM INFORMATION_SCHEMA.TABLES
						WHERE TABLE_SCHEMA = 'dbo'
						  AND TABLE_NAME IN ('hotels', 'rooms', 'bookings')
						""")
				.query(Integer.class)
				.single();

		assertThat(tableCount).isEqualTo(3);
	}

	@Test
	void openApiDescribesTheCompleteReviewerFlow() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Hotel Booking API"))
				.andExpect(jsonPath("$.paths['/api/admin/seed'].post").exists())
				.andExpect(jsonPath("$.paths['/api/hotels'].get").exists())
				.andExpect(jsonPath(
						"$.paths['/api/hotels/{hotelId}/rooms/available'].get")
						.exists())
				.andExpect(jsonPath("$.paths['/api/bookings'].post").exists())
				.andExpect(jsonPath("$.paths['/api/bookings'].post.responses['201']")
						.exists())
				.andExpect(jsonPath("$.paths['/api/bookings'].post.responses['409']")
						.exists())
				.andExpect(jsonPath("$.paths['/api/bookings/{reference}'].get")
						.exists());
	}

	@Test
	void swaggerUiIsAvailable() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/html"));
	}

	@Test
	void healthEndpointIsAvailable() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

}
