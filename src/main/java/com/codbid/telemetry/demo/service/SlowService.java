package com.codbid.telemetry.demo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class SlowService {

    public long simulateSlowHttpOperation() {
        long durationMs = ThreadLocalRandom.current().nextLong(900, 1600);
        sleep(durationMs);
        return durationMs;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", exception);
        }
    }
}
