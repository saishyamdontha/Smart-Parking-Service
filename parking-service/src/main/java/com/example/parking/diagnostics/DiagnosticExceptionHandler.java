package com.example.parking.diagnostics;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Catch-all for unexpected errors: returns a diagnostic code, the root cause and the correlation ID. */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class DiagnosticExceptionHandler {

    private final DiagnosticsRegistry diagnostics;

    public DiagnosticExceptionHandler(DiagnosticsRegistry diagnostics) {
        this.diagnostics = diagnostics;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = e.getClass().getSimpleName() + " (root cause: " + root.getClass().getSimpleName() + ")";
        diagnostics.record("PRK-INTERNAL", message);
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("code", "PRK-INTERNAL");
        String cid = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (cid != null) {
            body.put("correlationId", cid);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
