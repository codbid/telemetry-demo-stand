package com.codbid.telemetry.demo.service;

import com.codbid.telemetry.annotation.Telemetry;
import com.codbid.telemetry.model.TelemetryKind;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuditService {

    @Telemetry(
            operation = "saveAuditLog",
            component = "AuditService",
            kind = TelemetryKind.BUSINESS,
            tags = {"domain=audit", "stage=write"}
    )
    public void saveAuditLog(String entityId) {
        sleep(ThreadLocalRandom.current().nextLong(15, 45));
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
