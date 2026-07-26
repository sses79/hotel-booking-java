package com.ti5g.hotelbooking;

import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class HotelBookingApplicationTests {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private Clock clock;

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

}
