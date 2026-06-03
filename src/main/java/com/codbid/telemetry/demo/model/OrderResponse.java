package com.codbid.telemetry.demo.model;

import java.math.BigDecimal;

public record OrderResponse(
        String orderId,
        String status,
        BigDecimal amount
) {
}
