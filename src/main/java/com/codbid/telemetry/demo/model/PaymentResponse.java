package com.codbid.telemetry.demo.model;

public record PaymentResponse(
        String paymentId,
        String status
) {
}
