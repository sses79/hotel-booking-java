package com.ti5g.hotelbooking;

import com.ti5g.hotelbooking.integration.AvailabilityBarrier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean(destroyMethod = "stop")
	@ServiceConnection
	MSSQLServerContainer sqlServerContainer() {
		MSSQLServerContainer container = new MSSQLServerContainer(
				DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"));
		container.acceptLicense();
		return container;
	}

	@Bean
	AvailabilityBarrier availabilityBarrier() {
		return new AvailabilityBarrier();
	}

}
