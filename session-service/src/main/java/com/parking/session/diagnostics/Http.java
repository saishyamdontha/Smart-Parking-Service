package com.parking.session.diagnostics;

import org.slf4j.MDC;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.SocketTimeoutException;
import java.time.Duration;

/** Shared HTTP helpers: timeouts, correlation-ID propagation and timeout detection. */
public final class Http {

    private Http() {}

    public static SimpleClientHttpRequestFactory factory(int timeoutMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofMillis(timeoutMs));
        f.setReadTimeout(Duration.ofMillis(timeoutMs));
        return f;
    }

    public static ClientHttpRequestInterceptor correlation() {
        return (request, body, execution) -> {
            String id = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (id != null) {
                request.getHeaders().set(CorrelationIdFilter.HEADER, id);
            }
            return execution.execute(request, body);
        };
    }

    public static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
