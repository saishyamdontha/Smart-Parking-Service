package com.parking.session.web;

import com.parking.session.diagnostics.CorrelationIdFilter;
import com.parking.session.diagnostics.DiagnosticsRegistry;
import com.parking.session.exception.ConflictException;
import com.parking.session.exception.NotFoundException;
import com.parking.session.exception.UpstreamServiceException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Every error response carries a diagnostic code and the request's correlation ID. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private final DiagnosticsRegistry diagnostics;

    public ApiExceptionHandler(DiagnosticsRegistry diagnostics) {
        this.diagnostics = diagnostics;
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "SES-NOT-FOUND", e.getMessage(), false);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ConflictException e) {
        return respond(HttpStatus.CONFLICT, "SES-CONFLICT", e.getMessage(), false);
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<Map<String, String>> handleUpstream(UpstreamServiceException e) {
        HttpStatus status = e.kind() == UpstreamServiceException.Kind.TIMEOUT
                ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
        return respond(status, e.code(), e.getMessage(), true);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = e.getClass().getSimpleName() + " (root cause: " + root.getClass().getSimpleName() + ")";
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "SES-INTERNAL", message, true);
    }

    private ResponseEntity<Map<String, String>> respond(HttpStatus status, String code, String message, boolean fault) {
        if (fault) {
            diagnostics.record(code, message);
        }
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("code", code);
        String cid = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (cid != null) {
            body.put("correlationId", cid);
        }
        return ResponseEntity.status(status).body(body);
    }
}
