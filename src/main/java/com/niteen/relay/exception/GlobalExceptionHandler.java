package com.niteen.relay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(WorkflowValidationException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowValidation(WorkflowValidationException exception) {

        Map<String, Object> errorBody = Map.of (
                "error", Map.of(
                        "message", exception.getMessage(),
                        "code", exception.getCode()
                )
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody);

    }

}
