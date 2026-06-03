package com.codbid.telemetry.demo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class SlowService {

    private final LoadPressureService loadPressureService;

    public SlowService(LoadPressureService loadPressureService) {
        this.loadPressureService = loadPressureService;
    }

    public long simulateSlowHttpOperation() {
        long durationMs = ThreadLocalRandom.current().nextLong(350, 800)
                + loadPressureService.latencyPenaltyMs(200, 4_500);
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
