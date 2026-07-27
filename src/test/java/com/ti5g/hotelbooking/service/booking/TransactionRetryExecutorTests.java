package com.ti5g.hotelbooking.service.booking;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRetryExecutorTests {

	@Test
	void retriesTheCompleteOperationForRetryableSqlServerFailures() {
		var attempts = new AtomicInteger();
		var executor = new TransactionRetryExecutor(3, Duration.ZERO);

		var result = executor.execute(() -> {
			if (attempts.incrementAndGet() < 3) {
				throw deadlock();
			}
			return "created";
		});

		assertThat(result).isEqualTo("created");
		assertThat(attempts).hasValue(3);
	}

	@Test
	void retriesTheCompleteOperationForAReferenceCollision() {
		var attempts = new AtomicInteger();
		var executor = new TransactionRetryExecutor(2, Duration.ZERO);

		var result = executor.execute(() -> {
			if (attempts.incrementAndGet() == 1) {
				throw new RuntimeException(
						new SQLException(
								"unique booking reference",
								"23000",
								2627));
			}
			return "created";
		});

		assertThat(result).isEqualTo("created");
		assertThat(attempts).hasValue(2);
	}

	@Test
	void doesNotRetryNonTransientFailures() {
		var attempts = new AtomicInteger();
		var executor = new TransactionRetryExecutor(3, Duration.ZERO);

		assertThatThrownBy(() -> executor.execute(() -> {
			attempts.incrementAndGet();
			throw new IllegalArgumentException("invalid");
		}))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("invalid");
		assertThat(attempts).hasValue(1);
	}

	@Test
	void returnsConflictWhenEveryRetryableAttemptFails() {
		var attempts = new AtomicInteger();
		var executor = new TransactionRetryExecutor(3, Duration.ZERO);

		assertThatThrownBy(() -> executor.execute(() -> {
			attempts.incrementAndGet();
			throw deadlock();
		}))
				.isInstanceOf(BookingConflictException.class)
				.hasMessageContaining("concurrent changes");
		assertThat(attempts).hasValue(3);
	}

	private static RuntimeException deadlock() {
		return new RuntimeException(
				new SQLException("deadlock victim", "40001", 1205));
	}
}
