package com.codbid.telemetry.demo.service;

import com.codbid.telemetry.annotation.Telemetry;
import com.codbid.telemetry.demo.model.OrderResponse;
import com.codbid.telemetry.model.TelemetryKind;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final PaymentService paymentService;
    private final AuditService auditService;

    public OrderService(
            PaymentService paymentService,
            AuditService auditService
    ) {
        this.paymentService = paymentService;
        this.auditService = auditService;
    }

    @Telemetry(
            operation = "processOrder",
            component = "OrderService",
            kind = TelemetryKind.BUSINESS,
            tags = {"domain=orders", "stage=processing"}
    )
    public OrderResponse processOrder() {
        String orderId = UUID.randomUUID().toString();

        BigDecimal price = calculatePrice();
        paymentService.reservePayment(orderId, price);
        auditService.saveAuditLog(orderId);

        return new OrderResponse(
                orderId,
                "PROCESSED",
                price
        );
    }

    @Telemetry(
            operation = "processOrderById",
            component = "OrderService",
            kind = TelemetryKind.BUSINESS,
            tags = {"domain=orders", "stage=processing-by-id"}
    )
    public OrderResponse processOrderById(String orderId) {
        BigDecimal price = calculatePrice();
        auditService.saveAuditLog(orderId);

        return new OrderResponse(
                orderId,
                "PROCESSED_BY_ID",
                price
        );
    }

    @Telemetry(
            operation = "calculatePrice",
            component = "OrderService",
            kind = TelemetryKind.BUSINESS,
            tags = {"domain=orders", "stage=pricing"}
    )
    public BigDecimal calculatePrice() {
        sleep(ThreadLocalRandom.current().nextLong(40, 120));

        int amount = ThreadLocalRandom.current().nextInt(1000, 12000);
        return BigDecimal.valueOf(amount, 2);
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
