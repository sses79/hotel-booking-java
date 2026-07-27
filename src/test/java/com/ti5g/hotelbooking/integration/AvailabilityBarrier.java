package com.ti5g.hotelbooking.integration;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ti5g.hotelbooking.service.availability.AvailabilityReadObserver;

public class AvailabilityBarrier implements AvailabilityReadObserver {

	private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

	private volatile Gate gate = Gate.createDisarmed();

	public void arm() {
		gate = Gate.createArmed();
	}

	@Override
	public void afterAvailabilityRead() {
		var currentGate = gate;
		var participant = currentGate.participants().incrementAndGet();

		if (!currentGate.armed() || participant > 2) {
			return;
		}

		currentGate.arrivals().countDown();

		try {
			if (!currentGate.arrivals().await(
					WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException(
						"Concurrent availability reads did not reach the barrier.");
			}
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(
					"Interrupted while waiting for concurrent availability reads.",
					exception);
		}
	}

	private record Gate(
			boolean armed,
			AtomicInteger participants,
			CountDownLatch arrivals) {

		private static Gate createArmed() {
			return new Gate(true, new AtomicInteger(), new CountDownLatch(2));
		}

		private static Gate createDisarmed() {
			return new Gate(false, new AtomicInteger(), new CountDownLatch(0));
		}
	}
}
