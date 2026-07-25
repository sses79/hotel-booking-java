package com.ti5g.hotelbooking;

import org.springframework.boot.SpringApplication;

public class TestHotelBookingApplication {

	public static void main(String[] args) {
		SpringApplication.from(HotelBookingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
