package com.bank.accountservice.controller;

import com.bank.accountservice.dto.AccountResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.exception.CircuitBreakerOpenException;
import com.bank.accountservice.exception.ErrorRespBody;
import com.bank.accountservice.service.AccountService;
import com.bank.accountservice.exception.ValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<ErrorRespBody> handleCircuitBreakerOpen(CircuitBreakerOpenException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorRespBody.builder().status(500).error("Service is temporarily unavailable. Please try again later.").build());
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerNickname) {
        if (!StringUtils.hasText(accountNumber) && !StringUtils.hasText(customerName) && !StringUtils.hasText(customerNickname)) {
            throw new ValidationException("At least one filter is required: accountNumber, customerName or customerNickname.");
        }
        return ResponseEntity.ok(accountService.getAccounts(accountNumber, customerName, customerNickname));
    }
}
