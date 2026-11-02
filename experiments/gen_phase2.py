"""Adds controllable fault injection (latency, internal errors) to parking-service and activity-service.
Faults look like real failures: errors go through the normal exception handling.
The /admin/faults controls are for the experimenter only, never read by the evaluation."""
from pathlib import Path

SERVICES = [
    ("parking-service", "com.example.parking"),
    ("activity-service", "com.parking.activity"),
]

SETTINGS = '''package PKG.faults;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** The currently active injected fault (none by default). */
@Component
public class FaultSettings {

    public enum Mode { NONE, LATENCY, ERROR }

    private volatile Mode mode = Mode.NONE;
    private volatile int latencyMs = 0;
    private volatile double rate = 1.0;

    public synchronized void set(Mode mode, int latencyMs, double rate) {
        this.mode = mode;
        this.latencyMs = Math.max(0, latencyMs);
        this.rate = Math.min(1.0, Math.max(0.0, rate));
    }

    public synchronized void clear() {
        set(Mode.NONE, 0, 1.0);
    }

    public Mode mode() { return mode; }
    public int latencyMs() { return latencyMs; }
    public double rate() { return rate; }

    public Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", mode);
        m.put("latencyMs", latencyMs);
        m.put("rate", rate);
        return m;
    }
}
'''

INTERCEPTOR = '''package PKG.faults;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies the active fault to /api/** requests. Latency just slows the request down;
 * errors throw an ordinary exception, so they are handled and recorded like a real internal failure.
 */
@Component
public class FaultInjectionInterceptor implements HandlerInterceptor {

    private final FaultSettings settings;

    public FaultInjectionInterceptor(FaultSettings settings) {
        this.settings = settings;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        FaultSettings.Mode mode = settings.mode();
        if (mode == FaultSettings.Mode.NONE || ThreadLocalRandom.current().nextDouble() >= settings.rate()) {
            return true;
        }
        if (mode == FaultSettings.Mode.LATENCY) {
            Thread.sleep(settings.latencyMs());
            return true;
        }
        throw new IllegalStateException("internal processing failure");
    }
}
'''

WEBCONFIG = '''package PKG.faults;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FaultWebConfig implements WebMvcConfigurer {

    private final FaultInjectionInterceptor interceptor;

    public FaultWebConfig(FaultInjectionInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**");
    }
}
'''

ADMIN = '''package PKG.faults;

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
'''

for service, pkg in SERVICES:
    base = Path(service) / "src/main/java" / pkg.replace(".", "/") / "faults"
    base.mkdir(parents=True, exist_ok=True)
    for name, template in {
        "FaultSettings.java": SETTINGS,
        "FaultInjectionInterceptor.java": INTERCEPTOR,
        "FaultWebConfig.java": WEBCONFIG,
        "FaultAdminController.java": ADMIN,
    }.items():
        (base / name).write_text(template.replace("PKG", pkg))
    print(f"{service}: wrote 4 fault-injection files to {base}")
