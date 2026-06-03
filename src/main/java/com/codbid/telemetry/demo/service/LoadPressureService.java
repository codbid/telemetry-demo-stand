package com.codbid.telemetry.demo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LoadPressureService {

    private static final long WINDOW_MS = 1_000;

    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger windowRequests = new AtomicInteger();
    private final AtomicLong windowStartedAt = new AtomicLong(System.currentTimeMillis());

    public void requestStarted() {
        rotateWindowIfNeeded();
        inFlight.incrementAndGet();
        windowRequests.incrementAndGet();
    }

    public void requestFinished() {
        inFlight.updateAndGet(current -> Math.max(0, current - 1));
    }

    public int inFlight() {
        return inFlight.get();
    }

    public double recentRps() {
        rotateWindowIfNeeded();

        long elapsed = Math.max(1, System.currentTimeMillis() - windowStartedAt.get());
        return windowRequests.get() * 1_000.0 / elapsed;
    }

    public int pressureScore() {
        double score = inFlight() * 1.8 + recentRps() * 1.2;
        return (int) Math.min(100, Math.max(0, Math.round(score)));
    }

    public long latencyPenaltyMs(long normalMaxMs, long overloadedMaxMs) {
        int pressure = pressureScore();

        if (pressure < 10) {
            return randomBetween(0, normalMaxMs);
        }

        double ratio = pressure / 100.0;
        long maxPenalty = Math.round(normalMaxMs + (overloadedMaxMs - normalMaxMs) * ratio * ratio);
        return randomBetween(normalMaxMs / 2, Math.max(normalMaxMs, maxPenalty));
    }

    public boolean shouldFail(int normalPercent, int overloadedPercent) {
        int pressure = pressureScore();
        int probability = normalPercent + (overloadedPercent - normalPercent) * pressure / 100;
        return ThreadLocalRandom.current().nextInt(100) < probability;
    }

    public Snapshot snapshot() {
        return new Snapshot(inFlight(), recentRps(), pressureScore());
    }

    public record Snapshot(int inFlight, double recentRps, int pressureScore) {
    }

    private long randomBetween(long minInclusive, long maxExclusive) {
        if (maxExclusive <= minInclusive) {
            return minInclusive;
        }

        return ThreadLocalRandom.current().nextLong(minInclusive, maxExclusive);
    }

    private void rotateWindowIfNeeded() {
        long now = System.currentTimeMillis();
        long startedAt = windowStartedAt.get();

        if (now - startedAt < WINDOW_MS) {
            return;
        }

        if (windowStartedAt.compareAndSet(startedAt, now)) {
            windowRequests.set(0);
        }
    }
}
