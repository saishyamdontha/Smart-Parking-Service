"""Generates correlation-ID filter, diagnostics registry/endpoint and catch-all error handler
for parking-service and activity-service, with the right package names and code prefixes."""
from pathlib import Path

SERVICES = [
    ("parking-service", "com.example.parking", "PRK"),
    ("activity-service", "com.parking.activity", "ACT"),
]

FILTER = '''package PKG.diagnostics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Reuses the caller's correlation ID (or creates one) and puts it in logs and responses. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
'''

REGISTRY = '''package PKG.diagnostics;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory record of diagnostic error codes: counts per code and the most recent errors. */
@Component
public class DiagnosticsRegistry {

    public record ErrorEntry(Instant at, String code, String message, String correlationId) {}

    private static final int MAX_RECENT = 50;
    private final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();
    private final Deque<ErrorEntry> recent = new ConcurrentLinkedDeque<>();

    public void record(String code, String message) {
        counts.computeIfAbsent(code, k -> new AtomicLong()).incrementAndGet();
        recent.addFirst(new ErrorEntry(Instant.now(), code, message, MDC.get(CorrelationIdFilter.MDC_KEY)));
        while (recent.size() > MAX_RECENT) {
            recent.pollLast();
        }
    }

    public Map<String, Long> counts() {
        Map<String, Long> out = new TreeMap<>();
        counts.forEach((k, v) -> out.put(k, v.get()));
        return out;
    }

    public List<ErrorEntry> recent() {
        return new ArrayList<>(recent);
    }

    public void reset() {
        counts.clear();
        recent.clear();
    }
}
'''

CONTROLLER = '''package PKG.diagnostics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Service-level diagnostics: database status, error codes and recent errors. */
@RestController
public class DiagnosticsController {

    private final DiagnosticsRegistry registry;
    private final DataSource dataSource;
    private final Instant started = Instant.now();

    public DiagnosticsController(DiagnosticsRegistry registry, DataSource dataSource) {
        this.registry = registry;
        this.dataSource = dataSource;
    }

    @GetMapping("/diagnostics")
    public Map<String, Object> diagnostics() {
        Map<String, Object> dependencies = new LinkedHashMap<>();
        dependencies.put("database", database());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("service", "SERVICE");
        out.put("uptimeSeconds", Duration.between(started, Instant.now()).toSeconds());
        out.put("dependencies", dependencies);
        out.put("errorCounts", registry.counts());
        out.put("recentErrors", registry.recent());
        return out;
    }

    @PostMapping("/diagnostics/reset")
    public Map<String, String> reset() {
        registry.reset();
        return Map.of("status", "reset");
    }

    private String database() {
        try (Connection c = dataSource.getConnection()) {
            return c.isValid(1) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN: " + e.getClass().getSimpleName();
        }
    }
}
'''

HANDLER = '''package PKG.diagnostics;

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
        diagnostics.record("PREFIX-INTERNAL", message);
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("code", "PREFIX-INTERNAL");
        String cid = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (cid != null) {
            body.put("correlationId", cid);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
'''

for service, pkg, prefix in SERVICES:
    base = Path(service) / "src/main/java" / pkg.replace(".", "/") / "diagnostics"
    base.mkdir(parents=True, exist_ok=True)
    files = {
        "CorrelationIdFilter.java": FILTER,
        "DiagnosticsRegistry.java": REGISTRY,
        "DiagnosticsController.java": CONTROLLER,
        "DiagnosticExceptionHandler.java": HANDLER,
    }
    for name, template in files.items():
        text = template.replace("PKG", pkg).replace("PREFIX", prefix).replace("SERVICE", service)
        (base / name).write_text(text)
    print(f"{service}: wrote {len(files)} files to {base}")

# The existing parking-service handler must run before the new catch-all, so its
# NotFound/Conflict handlers keep returning 404/409 instead of being caught as 500.
h = Path("parking-service/src/main/java/com/example/parking/web/ApiExceptionHandler.java")
s = h.read_text()
if "@Order(" not in s:
    s = s.replace("import ", "import org.springframework.core.Ordered;\nimport org.springframework.core.annotation.Order;\nimport ", 1)
    s = s.replace("@RestControllerAdvice", "@RestControllerAdvice\n@Order(Ordered.HIGHEST_PRECEDENCE)", 1)
    h.write_text(s)
    print("parking-service: existing ApiExceptionHandler now runs first")
