package com.bank.accountservice.config;

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerConfigCustomizer accountServiceCBCustomizer() {
        return CircuitBreakerConfigCustomizer.of("accountService", builder ->
                builder.recordException(this::isDbUnavailable)
        );
    }

    /**
     * Trip the circuit breaker for all DB exceptions except DataIntegrityViolationException,
     * which indicates a business rule violation from a DB trigger, not a connectivity issue.
     */
    private boolean isDbUnavailable(Throwable ex) {
        return !(ex instanceof DataIntegrityViolationException);
    }
}
