package com.parking.activity.faults;

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
