package com.project.examportalbackend.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

/**
 * Catch-all so no unhandled exception escapes as an opaque container error.
 *
 * Extends {@link ResponseEntityExceptionHandler} so Spring's own handling of
 * standard MVC problems (malformed JSON, validation, wrong method, …) is kept
 * intact — those stay 4xx, not 500.
 *
 * Deliberate, validation-style errors are thrown as {@link ResponseStatusException}
 * throughout the services; those already carry the right status + message and are
 * preserved verbatim (the frontend reads {@code error.response.data.message}).
 * Anything genuinely unexpected is logged at ERROR (stack trace stays server-side)
 * and returned as a generic 500 that leaks nothing about internals.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Preserve intentional, status-bearing exceptions (the existing error contract). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(Map.of(
                        "status", ex.getStatus().value(),
                        "error", ex.getStatus().getReasonPhrase(),
                        "message", ex.getReason() != null ? ex.getReason() : ex.getStatus().getReasonPhrase()));
    }

    /** Everything unexpected: log the real cause server-side, return an opaque 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred."));
    }
}
