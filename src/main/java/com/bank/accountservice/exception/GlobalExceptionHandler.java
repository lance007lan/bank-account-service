package com.bank.accountservice.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// Centralizes exception handling across all controllers, mapping exceptions to structured HTTP error responses.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorRespBody> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(
                ErrorRespBody.builder().status(400).error(ex.getMessage()).build());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, JpaSystemException.class})
    public ResponseEntity<ErrorRespBody> handleDataIntegrity(Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage() != null && cause.getMessage().contains("maximum of 5 accounts")
                ? "Customer already has the maximum of 5 accounts."
                : "Data integrity violation.";
        return ResponseEntity.badRequest().body(
                ErrorRespBody.builder().status(400).error(message).build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespBody> handleUnreadable(HttpMessageNotReadableException ex) {
        String message = ex.getCause() instanceof UnrecognizedPropertyException upe
                ? "Unknown field '" + upe.getPropertyName() + "'. Accepted fields: " + upe.getKnownPropertyIds()
                : "Malformed JSON request";
        return ResponseEntity.badRequest().body(
                ErrorRespBody.builder().status(400).error(message).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespBody> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(
                ErrorRespBody.builder().status(400).error(message).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespBody> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorRespBody.builder().status(500).error("An unexpected error occurred. Please try again later.").build());
    }
}
