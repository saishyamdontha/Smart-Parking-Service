package com.parking.session.diagnostics;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/** Copies the correlation ID into @Async threads, so asynchronous calls stay traceable. */
@Configuration
public class AsyncMdcConfig {

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (context != null) MDC.setContextMap(context); else MDC.clear();
                try {
                    runnable.run();
                } finally {
                    if (previous != null) MDC.setContextMap(previous); else MDC.clear();
                }
            };
        };
    }
}
