package com.example.parking.faults;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Experimenter-only controls for injecting faults. Never used by the diagnosis side of the evaluation. */
@RestController
@RequestMapping("/admin/faults")
public class FaultAdminController {

    private final FaultSettings settings;

    public FaultAdminController(FaultSettings settings) {
        this.settings = settings;
    }

    @GetMapping
    public Map<String, Object> current() {
        return settings.describe();
    }

    @PostMapping
    public Map<String, Object> set(@RequestBody Map<String, Object> body) {
        FaultSettings.Mode mode = FaultSettings.Mode.valueOf(String.valueOf(body.getOrDefault("mode", "NONE")));
        int latencyMs = ((Number) body.getOrDefault("latencyMs", 0)).intValue();
        double rate = ((Number) body.getOrDefault("rate", 1.0)).doubleValue();
        settings.set(mode, latencyMs, rate);
        return settings.describe();
    }

    @DeleteMapping
    public Map<String, Object> clear() {
        settings.clear();
        return settings.describe();
    }
}
