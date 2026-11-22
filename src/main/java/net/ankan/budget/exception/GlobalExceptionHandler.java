
package net.ankan.budget.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailConflict(
            EmailAlreadyExistsException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(
            org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex) {
        return buildError(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                ex.getReason()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex) {

        String rootMsg = ex.getRootCause() != null
                ? ex.getRootCause().getMessage()
                : "";

        String msg;

        if (rootMsg != null && rootMsg.contains("Duplicate entry")) {
            msg = "A budget for this category and month already exists.";
        } else if (rootMsg != null &&
                (rootMsg.contains("foreign key constraint")
                        || rootMsg.contains("a foreign key constraint"))) {
            msg = "Cannot delete this item because it is linked to other records.";
        } else {
            msg = "A data integrity conflict occurred.";
        }

        return buildError(HttpStatus.CONFLICT, msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        for (ObjectError error : ex.getBindingResult().getAllErrors()) {

            if (error instanceof FieldError fieldError) {

                fieldErrors.put(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                );

            } else {

                // Handle class-level validation errors.
                // Map the goal amount constraint to currentSavedAmount.
                String fieldName = "general";

                if (error.getObjectName().equals("goalAnalysisRequestDto")) {
                    fieldName = "currentSavedAmount";
                }

                fieldErrors.put(
                        fieldName,
                        error.getDefaultMessage()
                );
            }
        }

        Map<String, Object> body = new HashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("fields", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex) {

        ex.printStackTrace();

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
    }

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status,
            String message) {

        Map<String, Object> body = new HashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
