package com.ti5g.hotelbooking.service.booking;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

@Component
public class TransactionRetryExecutor {

	private static final int DEFAULT_MAX_ATTEMPTS = 5;
	private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(25);
	private static final Set<Integer> RETRYABLE_SQL_SERVER_ERROR_CODES = Set.of(
			2601,
			2627,
			1205,
			1222,
			3960,
			41302,
			41305,
			41325);

	private final int maxAttempts;
	private final Duration initialBackoff;

	public TransactionRetryExecutor() {
		this(DEFAULT_MAX_ATTEMPTS, DEFAULT_INITIAL_BACKOFF);
	}

	TransactionRetryExecutor(int maxAttempts, Duration initialBackoff) {
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("maxAttempts must be at least 1");
		}
		this.maxAttempts = maxAttempts;
		this.initialBackoff = initialBackoff;
	}

	public <T> T execute(Supplier<T> operation) {
		for (int attempt = 1; ; attempt++) {
			try {
				return operation.get();
			}
			catch (RuntimeException exception) {
				if (!isRetryable(exception)) {
					throw exception;
				}
				if (attempt == maxAttempts) {
					throw new BookingConflictException(
							"Booking could not be completed because of concurrent changes. "
									+ "Please retry.",
							exception);
				}
				backoff(attempt);
			}
		}
	}

	private void backoff(int attempt) {
		try {
			Thread.sleep(initialBackoff.multipliedBy(attempt).toMillis());
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new BookingConflictException(
					"Booking was interrupted while retrying a concurrent change.",
					exception);
		}
	}

	private static boolean isRetryable(Throwable failure) {
		var current = failure;

		while (current != null) {
			if (current instanceof TransientDataAccessException) {
				return true;
			}
			if (current instanceof SQLException sqlException
					&& RETRYABLE_SQL_SERVER_ERROR_CODES.contains(sqlException.getErrorCode())) {
				return true;
			}
			current = current.getCause();
		}

		return false;
	}
}
