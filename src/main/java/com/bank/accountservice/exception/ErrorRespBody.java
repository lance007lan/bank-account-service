package com.bank.accountservice.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorRespBody {

    @Builder.Default
    private final String timestamp = LocalDateTime.now().toString();
    private final int status;
    private final String error;
    private final String retryAfter;
}
