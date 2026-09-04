package com.example.urlshortener.web;

import com.example.urlshortener.exception.CodeGenerationException;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ShortUrlNotFoundException;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    public record ApiError(int status, String error, String message, Instant timestamp) {}

    private ApiError error(HttpStatus status, String message) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, Instant.now(clock));
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ApiError> invalidUrl(InvalidUrlException e) {
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> beanValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed"));
    }

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ShortUrlNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(CodeGenerationException.class)
    public ResponseEntity<ApiError> codeGeneration(CodeGenerationException e) {
        log.error("Short-code generation exhausted retries", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate a short code, please retry"));
    }

    /**
     * Storage-layer failures degrade to a clean 503 instead of leaking a stack trace
     * (ambiguous-reliability.md R3). Integrity violations never reach here — the
     * collision-retry loop in the service consumes them.
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiError> storageUnavailable(org.springframework.dao.DataAccessException e) {
        log.error("Storage failure", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error(HttpStatus.SERVICE_UNAVAILABLE, "Storage temporarily unavailable, please retry"));
    }
}
