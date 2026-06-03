package com.codbid.telemetry.demo.telemetry;

import com.codbid.telemetry.customizer.TelemetryEventCustomizer;
import com.codbid.telemetry.model.TelemetryEvent;
import org.springframework.stereotype.Component;

@Component
public class DemoTelemetryCustomizer implements TelemetryEventCustomizer {

    @Override
    public TelemetryEvent customize(TelemetryEvent event) {
        event.getTags().put("demo", "true");
        event.getTags().put("team", "platform");
        event.getTags().put("system", "telemetry-demo");
        return event;
    }
}