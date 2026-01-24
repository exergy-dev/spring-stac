package org.stac.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the STAC API server.
 * Provides consistent error responses across all endpoints.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles NotFoundException - returns 404 Not Found.
     *
     * @param ex the exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage(),
                        Collections.emptyList()
                ));
    }

    /**
     * Handles ConflictException - returns 409 Conflict.
     *
     * @param ex the exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflictException(ConflictException ex) {
        log.warn("Resource conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(createErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        ex.getMessage(),
                        Collections.emptyList()
                ));
    }

    /**
     * Handles ValidationException - returns 400 Bad Request.
     *
     * @param ex the exception
     * @return ResponseEntity with validation errors
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        ex.getErrors()
                ));
    }

    /**
     * Handles RepositoryException - returns 500 Internal Server Error.
     *
     * @param ex the exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(RepositoryException.class)
    public ResponseEntity<Map<String, Object>> handleRepositoryException(RepositoryException ex) {
        log.error("Repository error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An internal error occurred while processing your request",
                        Collections.emptyList()
                ));
    }

    /**
     * Handles IllegalArgumentException - returns 400 Bad Request.
     *
     * @param ex the exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        Collections.emptyList()
                ));
    }

    /**
     * Handles all other exceptions - returns 500 Internal Server Error.
     *
     * @param ex the exception
     * @return ResponseEntity with generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected error occurred",
                        Collections.emptyList()
                ));
    }

    /**
     * Creates a standard error response body.
     *
     * @param code        the HTTP status code
     * @param description the error description
     * @param errors      list of specific errors (can be empty)
     * @return map representing the error response
     */
    private Map<String, Object> createErrorResponse(int code, String description, List<String> errors) {
        return Map.of(
                "code", code,
                "description", description,
                "errors", errors
        );
    }
}
