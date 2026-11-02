package com.parking.activity.faults;

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
