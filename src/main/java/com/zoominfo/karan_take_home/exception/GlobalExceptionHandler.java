package com.zoominfo.karan_take_home.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Global exception handler for the application.
 * Provides consistent error responses across all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .details(errors)
                .build();

        logger.warn("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles binding errors (e.g., from @ModelAttribute).
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null 
                                ? fieldError.getDefaultMessage() 
                                : "Invalid value",
                        (existing, replacement) -> existing
                ));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Binding Failed")
                .message("Request binding failed")
                .details(errors)
                .build();

        logger.warn("Binding error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles file upload size exceeded errors.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        HttpStatus status = HttpStatus.valueOf(413);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error("File Too Large")
                .message("The uploaded file exceeds the maximum allowed size")
                .build();

        logger.warn("File size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handles WebClient errors from FasterWhisper API calls.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponseException(
            WebClientResponseException ex) {
        HttpStatus httpStatus = HttpStatus.resolve(ex.getStatusCode().value());
        String errorReason = httpStatus != null ? httpStatus.getReasonPhrase() : "Error";
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error(errorReason)
                .message("Error communicating with Faster Whisper service: " + ex.getMessage())
                .build();

        logger.error("WebClient error from FasterWhisper API: {} {}", 
                ex.getStatusCode().value(), ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    /**
     * Handles general WebClient errors (network issues, timeouts, etc.).
     */
    @ExceptionHandler(WebClientException.class)
    public ResponseEntity<ErrorResponse> handleWebClientException(WebClientException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error("Service Unavailable")
                .message("Unable to communicate with Faster Whisper service: " + ex.getMessage())
                .build();

        logger.error("WebClient error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Argument")
                .message(ex.getMessage())
                .build();

        logger.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all other unhandled exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .build();

        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

