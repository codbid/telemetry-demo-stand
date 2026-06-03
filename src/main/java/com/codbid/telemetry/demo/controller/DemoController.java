package com.codbid.telemetry.demo.controller;

import com.codbid.telemetry.demo.model.OrderResponse;
import com.codbid.telemetry.demo.model.PaymentResponse;
import com.codbid.telemetry.demo.service.LoadPressureService;
import com.codbid.telemetry.demo.service.OrderService;
import com.codbid.telemetry.demo.service.PaymentService;
import com.codbid.telemetry.demo.service.SlowService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class DemoController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final SlowService slowService;
    private final LoadPressureService loadPressureService;

    public DemoController(
            OrderService orderService,
            PaymentService paymentService,
            SlowService slowService,
            LoadPressureService loadPressureService
    ) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.slowService = slowService;
        this.loadPressureService = loadPressureService;
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        long start = System.nanoTime();

        double result = 0.0;

        int pressureScore = loadPressureService.pressureScore();
        int iterations = 200_000 + pressureScore * 18_000;

        for (int i = 1; i <= iterations; i++) {
            result += Math.sqrt(i) * Math.sin(i) / Math.cos(i + 1);
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000;

        return Map.of(
                "status", "ok",
                "message", "CPU test request completed",
                "result", result,
                "durationMs", durationMs,
                "pressure", loadPressureService.snapshot()
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
        if (loadPressureService.shouldFail(2, 55)) {
            throw new IllegalStateException("Random flaky failure");
        }

        return Map.of(
                "status", "ok",
                "operation", "flaky",
                "pressure", loadPressureService.snapshot()
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
