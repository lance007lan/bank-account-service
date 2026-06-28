package com.bank.accountservice.config;

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerConfigCustomizer accountServiceCBCustomizer() {
        return CircuitBreakerConfigCustomizer.of("accountService", builder ->
                builder.recordException(this::isDbUnavailable)
        );
    }

    private boolean isDbUnavailable(Throwable ex) {
        boolean dbUnavailable = false;
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof DataAccessResourceFailureException
                    || cause instanceof CannotCreateTransactionException) {
                dbUnavailable = true;
            }
            // A connection reset means the DB aborted our connection (e.g. after a trigger exception),
            // not that the DB is down. Only ConnectException or SocketTimeoutException indicate a real outage.
            if (cause instanceof ConnectException || cause instanceof SocketTimeoutException) {
                return true;
            }
            if (cause instanceof java.net.SocketException && cause.getMessage() != null
                    && cause.getMessage().contains("Connection reset")) {
                return false;
            }
            cause = cause.getCause();
        }
        return dbUnavailable;
    }
}
