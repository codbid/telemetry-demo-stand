package com.codbid.telemetry.demo.controller;

import com.codbid.telemetry.demo.model.OrderResponse;
import com.codbid.telemetry.demo.model.PaymentResponse;
import com.codbid.telemetry.demo.service.OrderService;
import com.codbid.telemetry.demo.service.PaymentService;
import com.codbid.telemetry.demo.service.SlowService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class DemoController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final SlowService slowService;

    public DemoController(
            OrderService orderService,
            PaymentService paymentService,
            SlowService slowService
    ) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.slowService = slowService;
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        long start = System.nanoTime();

        double result = 0.0;

        for (int i = 1; i <= 300_000; i++) {
            result += Math.sqrt(i) * Math.sin(i) / Math.cos(i + 1);
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000;

        return Map.of(
                "status", "ok",
                "message", "CPU test request completed",
                "result", result,
                "durationMs", durationMs
        );
    }

    @GetMapping("/business")
    public OrderResponse business() {
        return orderService.processOrder();
    }

    @GetMapping("/payment")
    public PaymentResponse payment() {
        return paymentService.confirmPayment();
    }

    @GetMapping("/slow")
    public Map<String, Object> slow() {
        long durationMs = slowService.simulateSlowHttpOperation();

        return Map.of(
                "status", "ok",
                "operation", "slow",
                "durationMs", durationMs
        );
    }

    @GetMapping("/flaky")
    public Map<String, Object> flaky() {
        int value = ThreadLocalRandom.current().nextInt(100);

        if (value < 35) {
            throw new IllegalStateException("Random flaky failure");
        }

        return Map.of(
                "status", "ok",
                "operation", "flaky",
                "randomValue", value
        );
    }

    @GetMapping("/fail")
    public Map<String, Object> fail() {
        throw new IllegalStateException("Forced failure for telemetry demo");
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse orderById(@PathVariable String orderId) {
        return orderService.processOrderById(orderId);
    }
}
