package com.codbid.telemetry.demo.controller;

import com.codbid.telemetry.manual.TelemetryManual;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class ManualDemoController {

    private final TelemetryManual telemetryManual;

    public ManualDemoController(TelemetryManual telemetryManual) {
        this.telemetryManual = telemetryManual;
    }

    @GetMapping("/manual/success")
    public Map<String, Object> manualSuccess() {
        telemetryManual.success(
                "manualPaymentConfirmed",
                Map.of(
                        "domain", "payments",
                        "source", "manual-api",
                        "scenario", "success"
                )
        );

        return Map.of(
                "status", "ok",
                "manualEvent", "manualPaymentConfirmed"
        );
    }

    @GetMapping("/manual/error")
    public Map<String, Object> manualError() {
        RuntimeException exception = new RuntimeException("Manual payment failure");

        telemetryManual.error(
                "manualPaymentFailed",
                exception,
                Map.of(
                        "domain", "payments",
                        "source", "manual-api",
                        "scenario", "error"
                )
        );

        return Map.of(
                "status", "error-event-sent",
                "manualEvent", "manualPaymentFailed"
        );
    }

    @GetMapping("/manual/track")
    public Map<String, Object> manualTrack() {
        String result = telemetryManual.track(
                "manualOrderProcessing",
                Map.of(
                        "domain", "orders",
                        "source", "manual-track",
                        "scenario", "track-with-result"
                ),
                () -> {
                    sleep(ThreadLocalRandom.current().nextLong(120, 520));

                    int random = ThreadLocalRandom.current().nextInt(100);
                    if (random < 15) {
                        throw new IllegalStateException("Manual tracked order failed");
                    }

                    return "processed";
                }
        );

        return Map.of(
                "status", result,
                "manualEvent", "manualOrderProcessing"
        );
    }

    @GetMapping("/manual/track-void")
    public Map<String, Object> manualTrackVoid() {
        telemetryManual.track(
                "manualAuditLogWriting",
                Map.of(
                        "domain", "audit",
                        "source", "manual-track",
                        "scenario", "track-void"
                ),
                () -> sleep(ThreadLocalRandom.current().nextLong(30, 90))
        );

        return Map.of(
                "status", "ok",
                "manualEvent", "manualAuditLogWriting"
        );
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
