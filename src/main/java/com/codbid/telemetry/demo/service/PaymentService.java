package com.codbid.telemetry.demo.service;

import com.codbid.telemetry.annotation.Telemetry;
import com.codbid.telemetry.demo.model.PaymentResponse;
import com.codbid.telemetry.model.TelemetryKind;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    @Telemetry(
            operation = "reservePayment",
            component = "PaymentService",
            kind = TelemetryKind.BUSINESS,
            tags = {"domain=payments", "stage=reserve"}
    )
    public void reservePayment(String orderId, BigDecimal amount) {
        sleep(ThreadLocalRandom.current().nextLong(80, 260));

        int random = ThreadLocalRandom.current().nextInt(100);
        if (random < 5) {
            throw new IllegalStateException("Payment reservation failed for order " + orderId);
        }
    }

    @Telemetry(
            operation = "confirmPayment",
            component = "PaymentService",
            kind = TelemetryKind.BUSINESS,
            tags = {"domain=payments", "stage=confirm"}
    )
    public PaymentResponse confirmPayment() {
        sleep(ThreadLocalRandom.current().nextLong(60, 180));

        return new PaymentResponse(
                UUID.randomUUID().toString(),
                "CONFIRMED"
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
